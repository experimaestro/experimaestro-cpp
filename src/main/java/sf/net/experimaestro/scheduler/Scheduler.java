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

import com.google.common.collect.Multiset;
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
import sf.net.experimaestro.exceptions.ExperimaestroCannotOverwrite;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.exceptions.LockException;
import sf.net.experimaestro.utils.CloseableIterable;
import sf.net.experimaestro.utils.CloseableIterator;
import sf.net.experimaestro.utils.Heap;
import sf.net.experimaestro.utils.ThreadCount;
import sf.net.experimaestro.utils.je.FileProxy;
import sf.net.experimaestro.utils.log.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * The scheduler
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
final public class Scheduler {
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
    private Heap<Job<? extends JobData>> readyJobs = (Heap<Job<? extends JobData>>) new Heap<>(JobComparator.INSTANCE);

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
     * Listeners
     */
    HashSet<Listener> listeners = new HashSet<>();

    /**
     * Scheduler
     */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * Simple asynchronous executor service (used for asynchronous notification)
     */
    final ExecutorService executorService;


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

    /**
     * Returns the connector with the given id
     *
     * @param id
     * @return
     * @throws DatabaseException
     */
    public Connector getConnector(String id) throws DatabaseException {
        return connectors.get(id);
    }

    public void put(Connector connector) throws DatabaseException, ExperimaestroCannotOverwrite {
        connectors.put(connector);
    }

    public ScheduledFuture<?> schedule(final XPMProcess process, int rate, TimeUnit units) {
        return scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    process.check();
                } catch (Exception e) {
                    LOGGER.error(e, "Error while checking job [%s]: %s", process.getJob());
                }
            }
        }, 0, rate, units);

    }

    /**
     * Returns groups contained
     */
    public Multiset<String> subgroups(String group) {
        return resources.subgroups(group);
    }


    /**
     * Retrieves resources on which the given resource depends
     *
     *
     * @param to The resource
     * @return A map of dependencies
     */
    public ArrayList<Dependency> getDependencies(long to) {
        return resources.retrieveDependencies(to);
    }

    /**
     * Retrieves resources that depend upon the given resource
     *
     *
     * @param from The resource
     * @return A map of dependencies
     */
    public ArrayList<Dependency> getDependentResources(long from) {
        return resources.retrieveDependentResources(from);
    }

    /**
     * Delete a resource
     *
     * @param resource  The resource to delete
     * @param recursive Delete dependent resources
     */
    public void delete(Resource resource, boolean recursive) {
        synchronized (resource) {
            if (resource.getState() == ResourceState.RUNNING)
                throw new ExperimaestroRuntimeException("Cannot delete the running task [%s]", resource);
            ArrayList<Dependency> dependencies = resources.retrieveDependentResources(resource.getId());
            if (!dependencies.isEmpty()) {
                if (recursive) {
                    for (Dependency dependency : dependencies) {
                        Resource dep = getResource(dependency.getTo());
                        if (dep != null)
                            delete(dep, true);
                    }
                } else
                    throw new ExperimaestroRuntimeException("Cannot delete the resource %s: it has dependencies", resource);
            }

            if (resources.delete(resource.getId())) {
                SimpleMessage message = new SimpleMessage(Message.Type.RESOURCE_REMOVED, resource);
                resource.notify(message);
                notify(message);
            } else {
                LOGGER.error("Could not delete resource %s", resource);
            }
        }
    }

    public Resources getResources() {
        return resources;
    }

    public Resource getResource(ResourceLocator locator) {
        return resources.getByLocator(locator);
    }

    public void store(Dependency dependency) {
        getResources().store(dependency);
    }

    /**
     * Returns resources filtered by group and state
     *
     * @param group     The group the resource should belong to
     * @param recursive If <tt>true</tt>, it will iterate through all subgroups
     * @param states    The states of the resource
     * @param init      If true, resources will be initialized with the scheduler
     * @return
     */
    public CloseableIterator<Resource> resources(String group, boolean recursive, EnumSet<ResourceState> states, boolean init) {
        return resources.entities(group, recursive, states, init);
    }

    public CloseableIterator<Resource> resources(ResourceState state, boolean init) {
        return resources.entities("", true, EnumSet.of(state), init);
    }

    /**
     * Add listener
     *
     * @param listener
     */
    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    /**
     * Remove a listener
     *
     * @param listener
     */
    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    /**
     * Notify
     */
    public void notify(Message message) {
        for (Listener listener : listeners)
            listener.notify(message);
    }


    /**
     * This task runner takes a new task each time
     */
    class JobRunner extends Thread {
        private final String name;

        JobRunner(String name) {
            super(name);
            this.name = name;
        }

        @Override
        public void run() {
            Job job;
            try {
                while (!Scheduler.this.isStopping() && (job = getNextWaitingJob()) != null) {
                    // Set the state to LOCKING
                    job.setState(ResourceState.LOCKING);
                    LOGGER.info("Launching %s", job);
                    this.setName(name + "/" + job);
                    try {
                        job.run();
                        LOGGER.info("Job %s has started", job);
                    } catch (LockException e) {
                        // We could not lock the resources: update the job state
                        LOGGER.info("Could not lock all the resources for job %s [%s]", job, e.getMessage());
                        job.setState(ResourceState.WAITING);
                        job.updateStatus(true);
                        job.storeState(false);
                    } catch (Throwable t) {
                        LOGGER.warn(t, "Got a trouble while launching job [%s]", job);
                        job.setState(ResourceState.ERROR);
                        job.storeState(true);
                    } finally {
                    }
                    LOGGER.info("Finished launching %s", job);
                    this.setName(name);
                }
            } catch (InterruptedException e) {
                LOGGER.warn("Shutting down job runner", e);
            } finally {
                LOGGER.info("Shutting down job runner");
                counter.del();
            }
        }
    }


    /**
     * Initialise the task manager
     *
     * @param baseDirectory The directory where the XPM database will be stored
     * @throws EnvironmentLockedException
     * @throws DatabaseException
     */
    public Scheduler(File baseDirectory) throws DatabaseException {
        // Get the parameters
        this.baseDirectory = baseDirectory;
        this.nbThreads = 10;


        // Initialise the JE database
        LOGGER.info("Initialising JE database in directory %s", baseDirectory);
        EnvironmentConfig myEnvConfig = new EnvironmentConfig();
        myEnvConfig.setTransactional(true);
        StoreConfig storeConfig = new StoreConfig();

        myEnvConfig.setAllowCreate(true);
        storeConfig.setAllowCreate(true);
        storeConfig.setTransactional(true);
        dbEnvironment = new Environment(baseDirectory, myEnvConfig);

        EntityModel model = new AnnotationModel();
        model.registerClass(FileProxy.class);
        storeConfig.setModel(model);

        // Add a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                Scheduler.this.close();
            }
        }));

        // Initialise the stores
        dbStore = new EntityStore(dbEnvironment, "SchedulerStore", storeConfig);
        connectors = new Connectors(this, dbStore);

        // TODO: use bytecode enhancement to delete public default constructors and have better performance
        // See http://docs.oracle.com/cd/E17277_02/html/java/com/sleepycat/persist/model/ClassEnhancer.html


        // Initialise the running resources so that they can retrieve their state
        resources = new Resource.ResourcesIndex(this, dbStore);
        resources.init(readyJobs);

        // Start the thread that start the jobs
        LOGGER.info("Starting %d job runner threads", nbThreads);
        for (int i = 0; i < nbThreads; i++) {
            counter.add();
            final JobRunner runner = new JobRunner("JobRunner@" + i);
            threads.add(runner);
            runner.start();
        }

        executorService = Executors.newFixedThreadPool(1);


        LOGGER.info("Done - ready to work now");
    }

    boolean stopping = false;

    protected boolean isStopping() {
        return stopping;
    }

    // List of threads we started
    ArrayList<Thread> threads = new ArrayList<>();

    private Timer resourceCheckTimer;

    /**
     * Shutdown the scheduler
     */
    public void close() {
        stopping = true;

        // Stop the checker
        if (resourceCheckTimer != null) {
            resourceCheckTimer.cancel();
            resourceCheckTimer = null;
        }

        synchronized (readyJobs) {
            readyJobs.notifyAll();
        }

        counter.resume();

        // Stop the threads
        for (Thread thread : threads) {
            thread.interrupt();
        }
        threads.clear();

        executorService.shutdown();

        if (dbStore != null) {
            try {
                resources.close();
                connectors.close();
                dbStore.close();
            } catch (Throwable e) {
                LOGGER.error(e, String.format("Error while closing the database"));
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

        LOGGER.info("Scheduler stopped");
    }

    // ----
    // ---- Task related methods
    // ----

    /**
     * Get a resource by from
     * <p/>
     * First checks if the resource is in the list of tasks to be run. If not,
     * we look directly on disk to get back information on the resource.
     *
     * @throws DatabaseException
     */

    public Resource getResource(long id)
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
            synchronized (readyJobs) {
                LOGGER.debug("Looking at the next job to run [%d]", readyJobs.size());
                if (isStopping())
                    return null;

                if (!readyJobs.isEmpty()) {
                    final Job task = readyJobs.peek();
                    LOGGER.debug("Fetched task %s: checking for execution", task);
                    final Job pop = readyJobs.pop();
                    return pop;
                }

                // ... and wait if we were not lucky (or there were no tasks)
                try {
                    readyJobs.wait();
                } catch (InterruptedException e) {
                    // The server stopped
                }
            }

        }
    }


    /**
     * Iterator on resources
     */
    public CloseableIterable<Resource> resources() {
        return resources.values();
    }


    /**
     * Add/remove a job to the list of jobs ready to be run
     */
    void setReady(Job job) {
        boolean ready = job.getState() == ResourceState.READY;
        if (ready) {
            synchronized (readyJobs) {
                if (job.getIndex() < 0) {
                    readyJobs.add(job);
                    readyJobs.notify();
                    LOGGER.info("Job %s [%d] is ready [notifying], %d", job, job.getIndex(), readyJobs.size());
                }
            }
        } else {
            synchronized (readyJobs) {
                if (job.getIndex() >= 0) {
                    // don't notify since nothing has to be done
                    LOGGER.info("Deleting job [%s/%d] from ready jobs", job, job.getIndex());
                    readyJobs.remove(job);
                }
            }
        }
    }

    /**
     * Store a resource in the database.
     * <p/>
     * This method is in charge of storing all the necessary information in the database and
     * updating the different structures (e.g. list of jobs to be run).
     *
     * @param resource The resource to store
     * @throws DatabaseException
     */
    synchronized public void store(final Resource resource, boolean notify) throws DatabaseException, ExperimaestroCannotOverwrite {
        // Update the task and notify ourselves since we might want
        // to run new processes

        if (isStopping()) {
            LOGGER.warn("Database is closing: Could not update resource %s", resource);
            return;
        }

        // If new, update the status
        if (!resource.stored())
            resource.updateStatus(false);

        // First store the resource
        final Resource old = resources.put(resource);

        // Special case of jobs: we need to track
        // jobs that are ready to be run
        if (resource instanceof Job) {

            // If overridding, remove the previous job from heap
            if (old != null && old instanceof Job) {
                int index = ((Job) old).getIndex();
                if (index > 0)
                    readyJobs.remove((Job) old);
            }

            Job job = (Job) resource;
            setReady(job);

        }

        // Notify dependencies, using a new process
        if (notify)
            executorService.submit(new Notifier(resource));
    }

    /**
     * Will notify dependencies
     */
    private class Notifier implements Runnable {
        private final Resource resource;

        public Notifier(Resource resource) {
            this.resource = resource;
        }

        @Override
        public void run() {
            if (!isStopping()) {
                resources.notifyDependencies(resource);
            }
        }
    }
}
