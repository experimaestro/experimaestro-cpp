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

import sf.net.experimaestro.connectors.Connector;
import sf.net.experimaestro.connectors.XPMProcess;
import sf.net.experimaestro.exceptions.CloseException;
import sf.net.experimaestro.exceptions.ExperimaestroCannotOverwrite;
import sf.net.experimaestro.exceptions.LockException;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.manager.experiments.Experiment;
import sf.net.experimaestro.manager.experiments.TaskReference;
import sf.net.experimaestro.utils.CloseableIterable;
import sf.net.experimaestro.utils.CloseableIterator;
import sf.net.experimaestro.utils.Heap;
import sf.net.experimaestro.utils.ThreadCount;
import sf.net.experimaestro.utils.log.Logger;

import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

import static java.lang.String.format;

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
     * Thread local instance (there should be only one scheduler per thread)
     */
    private static ThreadLocal<Scheduler> INSTANCE = new ThreadLocal<>();

    /**
     * Simple asynchronous executor service (used for asynchronous notification)
     */
    final ExecutorService executorService;

    /**
     * Scheduler
     */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final EntityManager entityManager;

    /**
     * The list of jobs organised in a heap - with those having all dependencies
     * fulfilled first
     */
    private final Heap<Job> readyJobs = new Heap<>(JobComparator.INSTANCE);

    /**
     * Listeners
     */
    HashSet<Listener> listeners = new HashSet<>();

    boolean stopping = false;

    /** List of threads we started */
    ArrayList<Thread> threads = new ArrayList<>();

    /**
     * Number of running threads
     */
    private ThreadCount counter = new ThreadCount();

    private Timer resourceCheckTimer;

    /**
     * Initialise the task manager
     *
     * @param baseDirectory The directory where the XPM database will be stored
     */
    public Scheduler(File baseDirectory) {
        if (INSTANCE.get() != null) {
            throw new XPMRuntimeException("Only one scheduler instance should be created");
        }

        INSTANCE.set(this);

        // Get the parameters
        /*
      Number of threads running concurrently (excluding any server)
     */
        int nbThreads = 10;


        // Initialise the database
        LOGGER.info("Initialising database in directory %s", baseDirectory);
        HashMap<String, String> properties = new HashMap<>();
        properties.put("hibernate.connection.url", format("jdbc:hsqldb:file:%s/xpm;shutdown=true", baseDirectory));
        properties.put("hibernate.connection.username", "");
        properties.put("hibernate.connection.password", "");

        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("net.bpiwowar.experimaestro",
                properties);
        entityManager = entityManagerFactory.createEntityManager();

        // Add a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(Scheduler.this::close));

        // Initialise the running resources so that they can retrieve their state
        TypedQuery<Resource> query = entityManager.createQuery("from resources r where r.state = :state", Resource.class);
        query.setParameter("state", ResourceState.RUNNING);
        for (Resource resource : query.getResultList()) {
            readyJobs.add((Job) resource);
        }


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

    public static EntityTransaction transaction() {
        EntityTransaction transaction = INSTANCE.get().entityManager.getTransaction();
        if (transaction.isActive())
            return transaction;
        transaction.begin();
        return transaction;
    }

    public static Scheduler get() {
        return INSTANCE.get();
    }

    public Connector getConnector(String id) {
        return entityManager.find(Connector.class, id);
    }

    public void put(Connector connector) throws ExperimaestroCannotOverwrite {
        entityManager.persist(connector);
    }

    public ScheduledFuture<?> schedule(final XPMProcess process, int rate, TimeUnit units) {
        return scheduler.scheduleAtFixedRate(() -> {
            try {
                process.check();
            } catch (Exception e) {
                LOGGER.error(e, "Error while checking job [%s]: %s", process.getJob());
            }
        }, 0, rate, units);

    }

    public void store(Dependency dependency) {
        entityManager.persist(dependency);
    }

    /**
     * Returns resources filtered by group and state
     *
     * @param states The states of the resource
     * @return A closeable iterator
     */
    public CloseableIterator<Resource> resources(EnumSet<ResourceState> states) {
        CriteriaBuilder criteria = entityManager.getCriteriaBuilder();
        CriteriaQuery<Resource> cq = criteria.createQuery(Resource.class);
        Root<Resource> root = cq.from(Resource.class);
        cq.where(root.get("state").in(states));
        TypedQuery<Resource> query = entityManager.createQuery(cq);
        List<Resource> result = query.getResultList();

        return CloseableIterator.of(result.iterator());
    }

    /**
     * Add a listener for the changes in the resource states
     *
     * @param listener The listener to add
     */
    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    /**
     * Remove a listener
     *
     * @param listener The listener to remove
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

    protected boolean isStopping() {
        return stopping;
    }

    // ----
    // ---- TaskReference related methods
    // ----

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

        if (executorService != null) {
            executorService.shutdown();
        }

        INSTANCE = null;
        LOGGER.info("Scheduler stopped");
    }

    /**
     * Get a resource by ID
     */

    public Resource getResource(long id) {
        return entityManager.find(Resource.class, id);
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
                    return readyJobs.pop();
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
        // TDOO should to something else
        TypedQuery<Resource> query = entityManager.createQuery("from resources", Resource.class);
        query.setFlushMode(FlushModeType.AUTO);
        List<Resource> resultList = query.getResultList();
        return new CloseableIterable<Resource>() {
            @Override
            public void close() throws CloseException {

            }

            @Override
            public Iterator<Resource> iterator() {
                return resultList.iterator();
            }
        };
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
     */
    synchronized public void store(final Resource resource, boolean notify) throws ExperimaestroCannotOverwrite {
        // Update the task and notify ourselves since we might want
        // to run new processes

        if (isStopping()) {
            LOGGER.warn("Database is closing: Could not update resource %s", resource);
            return;
        }

        // If new, update the status
        if (!resource.stored()) {
            resource.updateStatus(false);
        }

        // Store the resource
        CriteriaBuilder criteria = entityManager.getCriteriaBuilder();
        CriteriaQuery<Resource> cq = criteria.createQuery(Resource.class);
        Root<Resource> root = cq.from(Resource.class);
        cq.where(root.get("path").in(resource.path));
        TypedQuery<Resource> query = entityManager.createQuery(cq);
        List<Resource> result = query.getResultList();
        assert result.size() <= 1;

        Resource old = null;
        if (!result.isEmpty()) {
            old = result.get(0);
        }
        // FIXME ?
        entityManager.persist(resource);

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
        if (notify) {
            executorService.submit(new Notifier(resource));
        }
    }

    public void store(TaskReference reference) throws ExperimaestroCannotOverwrite {
        entityManager.persist(reference);
    }

    public void store(Experiment experiment) throws ExperimaestroCannotOverwrite {
        entityManager.persist(experiment);
    }

    public Resource getResource(Path path) {
        CriteriaBuilder criteria = entityManager.getCriteriaBuilder();
        CriteriaQuery<Resource> cq = criteria.createQuery(Resource.class);
        Root<Resource> root = cq.from(Resource.class);
        cq.where(root.get("path").in((Path) path));
        TypedQuery<Resource> query = entityManager.createQuery(cq);
        List<Resource> result = query.getResultList();
        assert result.size() <= 1;

        return result.get(0);
    }

    public void remove(Object resource) {
        this.entityManager.remove(resource);
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
                resource.notifyDependencies();
            }
        }
    }
}
