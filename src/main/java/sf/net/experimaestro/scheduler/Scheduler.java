/*
 * This file is part of experimaestro.
 * Copyright (c) 2012 B. Piwowarski <benjamin@bpiwowar.net>
 *
 * experimaestro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * experimaestro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 */

package sf.net.experimaestro.scheduler;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.EnvironmentLockedException;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.StoreConfig;
import com.sleepycat.persist.model.AnnotationModel;
import com.sleepycat.persist.model.EntityModel;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import sf.net.experimaestro.connectors.Connector;
import sf.net.experimaestro.connectors.XPMProcess;
import sf.net.experimaestro.utils.Heap;
import sf.net.experimaestro.utils.ThreadCount;
import sf.net.experimaestro.utils.iterators.AbstractIterator;
import sf.net.experimaestro.utils.je.FileObjectProxy;
import sf.net.experimaestro.utils.je.FileProxy;
import sf.net.experimaestro.utils.log.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Timer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * The scheduler
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class Scheduler {
    final static private Logger LOGGER = Logger.getLogger();

    /**
     * Used for atomic series of locks
     */
    public static String LockSync = "I am just a placeholder";

    /**
     * Main directory for the task manager. One database subfolder will be
     * created
     */
    private File baseDirectory;

    /**
     * Number of threads running concurrently (excluding any server)
     */
    private int nbThreads = 5;

    /**
     * Number of running threads
     */
    private ThreadCount counter = new ThreadCount();

    /**
     * The list of jobs organised in a heap - with those having all dependencies
     * fulfilled first
     */
    private Heap<Job> readyJobs = new Heap<>(JobComparator.INSTANCE);

    /**
     * All the resources
     */
    private Resources resources;

    /**
     * All the connectors
     */
    private Connectors connectors;


    /**
     * The database store
     */
    private EntityStore dbStore;

    /**
     * The database environement
     */
    private Environment dbEnvironment;

    /**
     * Scheduler
     */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    {
    }

    /**
     * The file manager
     */
    private static FileSystemManager fsManager;

    static {
        try {
            fsManager = VFS.getManager();
        } catch (FileSystemException e) {
            LOGGER.error("Cannot initialize the file system manager: " + e);
            System.exit(-1);
        }
    }

    public static FileSystemManager getVFSManager() {
        return fsManager;
    }

    public Connector getConnector(String id) throws DatabaseException {
        return connectors.get(id);
    }

    public void put(Connector connector) throws DatabaseException {
        connectors.put(connector);
    }

    public ScheduledFuture<?> schedule(final XPMProcess process, int rate, TimeUnit units) {
        return scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    process.check();
                } catch (Exception e) {
                    LOGGER.error("Error while checking job [%s]: %s", process.getJob(), e);
                }
            }
        }, 0, rate, units);

    }

    /**
     * Returns resources in the given states
     *
     * @param states
     * @return
     */
    public Iterable<Resource> resources(final EnumSet<ResourceState> states) {
        return new Iterable<Resource>() {
            @Override
            public Iterator<Resource> iterator() {
                return new AbstractIterator<Resource>() {
                    Iterator<Resource> iterator = resources().iterator();

                    @Override
                    protected boolean storeNext() {
                        while (iterator.hasNext()) {
                            value = iterator.next();
                            if (states.contains(value.getState()))
                                return true;
                        }
                        return false;

                    }
                };
            }
        };
    }

    /**
     * This task runner takes a new task each time
     */
    class JobRunner extends Thread {
        @Override
        public void run() {
            Job job;
            try {
                while (!Scheduler.this.isStopping() && (job = getNextWaitingJob()) != null) {
                    LOGGER.info("Starting %s", job);
                    try {
                        job.run();
                        LOGGER.info("Finished %s", job);
                    } catch (Throwable t) {
                        LOGGER.warn("Houston, we got a problem: %s", t);
                    }
                }
            } catch (InterruptedException e) {
                LOGGER.warn("Shutting down job runner", e);
            }

            counter.del();
        }
    }


    /**
     * Initialise the task manager
     *
     * @param baseDirectory
     * @throws EnvironmentLockedException
     * @throws DatabaseException
     */
    public Scheduler(File baseDirectory)
            throws EnvironmentLockedException, DatabaseException {
        // Get the parameters
        this.baseDirectory = baseDirectory;
        this.nbThreads = 10;


        // Initialise the JE database
        LOGGER.info("Initialising JE database in directory %s", baseDirectory);
        EnvironmentConfig myEnvConfig = new EnvironmentConfig();
        myEnvConfig.setTransactional(false);
        StoreConfig storeConfig = new StoreConfig();

        myEnvConfig.setAllowCreate(true);
        storeConfig.setAllowCreate(true);
        dbEnvironment = new Environment(baseDirectory, myEnvConfig);

        EntityModel model = new AnnotationModel();
        model.registerClass(FileProxy.class);
        model.registerClass(FileObjectProxy.LocalProxy.class);
        storeConfig.setModel(model);

        // Add a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                Scheduler.this.close();
            }
        }));

        // Initialise the store
        dbStore = new EntityStore(dbEnvironment, "SchedulerStore", storeConfig);
        resources = new Resources(this, dbStore);
        connectors = new Connectors(this, dbStore);

        // FIXME: use bytecode enhancement to remove public default constructors and have better performance
        // See http://docs.oracle.com/cd/E17277_02/html/java/com/sleepycat/persist/model/ClassEnhancer.html


        // Initialise the running resources so that they can retrieve their state
        for (Resource resource : resources) {
            // TODO: restrict to "interesting" states (READY, WAITING, RUNNING)
            if (ResourceState.ACTIVE.contains(resource.getState())) {
                resource.init(this);
                // Add job to the queue if ready
                if (resource instanceof Job && resource.getState() == ResourceState.READY)
                    readyJobs.add((Job) resource);
            }
        }

        // Start the thread that start the jobs
        LOGGER.info("Starting %d threads", nbThreads);
        for (int i = 0; i < nbThreads; i++) {
            counter.add();
            final JobRunner runner = new JobRunner();
            threads.add(runner);
            runner.start();
        }

        LOGGER.info("Done - ready to work now");
    }

    boolean stopping = false;

    protected boolean isStopping() {
        return stopping;
    }

    // List of threads we started
    ArrayList<Thread> threads = new ArrayList<Thread>();

    private Timer resourceCheckTimer;

    /**
     * Shutdown the scheduler
     */
    public void close() {
        // Stop the checker
        if (resourceCheckTimer != null) {
            resourceCheckTimer.cancel();
            resourceCheckTimer = null;
        }

        // Stop the threads
        for (Thread thread : threads) {
            thread.interrupt();
        }
        threads.clear();

        if (dbStore != null) {
            try {
                dbStore.close();
            } catch (Exception e) {
                LOGGER.error(String.format("Error while closing the database: %s", e));
            }
            dbStore = null;
            LOGGER.info("Closed the database store");
        }

        if (dbEnvironment != null) {
            try {
                // Finally, close environment.
                dbEnvironment.close();
                dbEnvironment = null;
                LOGGER.info("Closed the database environment");
            } catch (DatabaseException dbe) {
                LOGGER.error("Error closing MyDbEnv: " + dbe.toString());
            }
        }
    }

    // ----
    // ---- Task related methods
    // ----

    /**
     * Get a resource by locator
     * <p/>
     * First checks if the resource is in the list of tasks to be run. If not,
     * we look directly on disk to get back information on the resource.
     *
     * @throws DatabaseException
     */
    synchronized public Resource getResource(ResourceLocator id)
            throws DatabaseException {
        Resource resource = resources.get(id);
        return resource;
    }



    /**
     * Get the next waiting job
     *
     * @return A boolean, true if a task was started
     * @throws InterruptedException
     */
    Job getNextWaitingJob() throws InterruptedException {
        while (true) {
            LOGGER.debug("Fetching the next task to run");
            // Try the next task
            synchronized (this) {
                if (!readyJobs.isEmpty()) {
                    final Job task = readyJobs.peek();
                    LOGGER.debug(
                            "Fetched task %s: checking for execution [%d unsatisfied]",
                            task, task.nbUnsatisfied);
                    if (task.nbUnsatisfied == 0) {
                        return readyJobs.pop();
                    }
                }

                // ... and wait if we were not lucky (or there were no tasks)
                try {
                    wait();
                } catch (InterruptedException e) {
                    // The server stopped
                }
            }

        }
    }


    /**
     * Iterator on resources
     */
    synchronized public Iterable<Resource> resources() {
        return resources;
    }

    public Iterable<Job> tasks() {
        return readyJobs;
    }

    /**
     * Store a resource in the database
     *
     * @param resource
     * @throws DatabaseException
     */
    synchronized public void store(Resource resource) throws DatabaseException {
        // Update the task and notify ourselves since we might want
        // to run new processes

        if (resource instanceof Job) {
            Job job = (Job) resource;
            // Update the heap
            if (resource.getState() == ResourceState.READY) {
                if (job.getIndex() < 0)
                    readyJobs.add(job);
                else
                    readyJobs.update(job);

                LOGGER.info("Job %s is ready [notifying]", resource);

                // Notify job runners
                notify();
            } else if (job.getIndex() > 0) {
                readyJobs.remove(job);
            }
        }

        // Notify listeners
        resource.notifyListeners();


        resources.put(resource);
    }

}
