package sf.net.experimaestro.scheduler;

/*
 * This file is part of experimaestro.
 * Copyright (c) 2014 B. Piwowarski <benjamin@bpiwowar.net>
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

import sf.net.experimaestro.connectors.XPMProcess;
import sf.net.experimaestro.exceptions.CloseException;
import sf.net.experimaestro.exceptions.LockException;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.utils.CloseableIterable;
import sf.net.experimaestro.utils.CloseableIterator;
import sf.net.experimaestro.utils.ThreadCount;
import sf.net.experimaestro.utils.log.Logger;

import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.io.File;
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
     * Thread local instance (there should be only one scheduler per thread)
     */
    private static Scheduler INSTANCE;
    /**
     * Simple asynchronous executor service (used for asynchronous notification)
     */
    final ExecutorService executorService;
    final Boolean readyJobSemaphore = true;
    /**
     * Scheduler
     */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    /**
     * The entity manager factory
     */
    EntityManagerFactory entityManagerFactory;
    /**
     * Listeners
     */
    HashSet<Listener> listeners = new HashSet<>();
    /**
     * True when the application is stopping
     */
    boolean stopping = false;
    /**
     * List of runners we started
     */
    JobRunner runner;

    /**
     * A query that retrieves jobs that are ready, ordered by decreasing priority
     */
    CriteriaQuery<Job> readyJobsQuery;
    /**
     * Number of running runners
     */
    private ThreadCount counter = new ThreadCount();
    private Timer resourceCheckTimer;

    /**
     * Initialise the task manager
     *
     * @param baseDirectory The directory where the XPM database will be stored
     */
    public Scheduler(File baseDirectory) {
        if (INSTANCE != null) {
            throw new XPMRuntimeException("Only one scheduler instance should be created");
        }

        INSTANCE = this;

        // Get the parameters
        /*
      Number of threads running concurrently (excluding any server)
     */
        int nbThreads = 10;


        // Initialise the database
        LOGGER.info("Initialising database in directory %s", baseDirectory);
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("hibernate.connection.url", format("jdbc:hsqldb:file:%s/xpm;shutdown=true;hsqldb.tx=mvcc", baseDirectory));
        properties.put("hibernate.connection.username", "");
        properties.put("hibernate.connection.password", "");

        ArrayList<Class<?>> loadedClasses = new ArrayList<>();
        ServiceLoader<PersistentClassesAdder> services = ServiceLoader.load(PersistentClassesAdder.class);
        for (PersistentClassesAdder service : services) {
            service.add(loadedClasses);
        }

        properties.put(org.hibernate.jpa.AvailableSettings.LOADED_CLASSES, loadedClasses);

        entityManagerFactory = Persistence.createEntityManagerFactory("net.bpiwowar.experimaestro",
                properties);
        // Add a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(Scheduler.this::close));


        // Create reused criteria queries
        CriteriaBuilder builder = entityManagerFactory.getCriteriaBuilder();
        readyJobsQuery = builder.createQuery(Job.class);
        Root<Job> root = readyJobsQuery.from(Job.class);
        readyJobsQuery.orderBy(builder.desc(root.get("priority")));
        readyJobsQuery.where(root.get("state").in(ResourceState.READY));

        // Initialise the running resources so that they can retrieve their state
        EntityManager entityManager = entityManagerFactory.createEntityManager();

        TypedQuery<Resource> query = entityManager.createQuery("from resources r where r.state = :state", Resource.class);
        query.setParameter("state", ResourceState.RUNNING);
        for (Resource resource : query.getResultList()) {
            LOGGER.info("Job %s is running: starting a watcher", resource);
            Job job = (Job) resource;
            job.process.init(job);
            resource.updateStatus();
        }


        // Start the thread that start the jobs
        LOGGER.info("Starting the job runner thread");
        counter.add();
        runner = new JobRunner("JobRunner");
        runner.start();

        executorService = Executors.newFixedThreadPool(1);


        LOGGER.info("Done - ready status work now");
    }

    public static Scheduler get() {
        return INSTANCE;
    }

    public static void notifyRunners() {
        final Boolean semaphore = get().readyJobSemaphore;
        synchronized (semaphore) {
            semaphore.notify();
        }
    }

    public static EntityManager manager() {
        return get().entityManagerFactory.createEntityManager();
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

    /**
     * Returns resources filtered by group and state
     *
     * @param states The states of the resource
     * @return A closeable iterator
     */
    public CloseableIterator<Resource> resources(EnumSet<ResourceState> states) {
        List<Resource> result = Transaction.evaluate((EntityManager em) -> {
            CriteriaBuilder criteria = entityManagerFactory.getCriteriaBuilder();
            CriteriaQuery<Resource> cq = criteria.createQuery(Resource.class);
            Root<Resource> root = cq.from(Resource.class);
            cq.where(root.get("state").in(states));
            TypedQuery<Resource> query = em.createQuery(cq);
            return query.getResultList();
        });
        return CloseableIterator.of(result.iterator());
    }

    // ----
    // ---- TaskReference related methods
    // ----

    /**
     * Add a listener for the changes in the resource states
     *
     * @param listener The listener status add
     */
    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    /**
     * Remove a listener
     *
     * @param listener The listener status remove
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

    /**
     * Shutdown the scheduler
     */
    synchronized public void close() {
        if (entityManagerFactory == null && stopping) {
            return;
        }
        stopping = true;
        // Stop the checker
        LOGGER.info("Closing resource checker");
        if (resourceCheckTimer != null) {
            resourceCheckTimer.cancel();
            resourceCheckTimer = null;
        }


        // Stop the threads
        LOGGER.info("Stopping runner and scheduler");
        runner.interrupt();
        counter.resume();
        runner = null;

        if (executorService != null) {
            executorService.shutdown();
        }

        INSTANCE = null;

        LOGGER.info("Closing entity manager factory");
        entityManagerFactory.close();
        entityManagerFactory = null;
        LOGGER.info("Scheduler stopped");
    }

    /**
     * Iterator on resources
     */
    public CloseableIterable<Resource> resources() {
        final List<Resource> resultList = Transaction.evaluate(em -> {
            TypedQuery<Resource> query = em.createQuery("from resources", Resource.class);
            query.setFlushMode(FlushModeType.AUTO);
            return query.getResultList();
        });

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
            try {
                // Flag stating whether we should wait for something
                boolean doWait = true;

                while (true) {
                    if (isStopping())
                        return;

                    // ... and wait if we were not lucky (or there were no tasks)
                    try {
                        if (doWait) {
                            synchronized (readyJobSemaphore) {
                                LOGGER.debug("Going to sleep [%s]...", name);
                                readyJobSemaphore.wait();
                                LOGGER.debug("Waking up [%s]...", name);
                            }
                        }
                    } catch (InterruptedException e) {
                        // The server stopped
                        break;
                    }


                    // Try the next task
                    Job job = null;
                    doWait = false;
                    try (Transaction transaction = Transaction.create()) {
                        LOGGER.debug("Searching for ready jobs");

                        final EntityManager em = transaction.em();

                        // We avoid race conditions by starting each job in isolation
                            TypedQuery<Job> query = em.createQuery(Scheduler.this.readyJobsQuery);
                            query.setMaxResults(1);
                            query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
                            List<Job> list = query.getResultList();
                            if (list.isEmpty()) {
                                LOGGER.debug("No job status run");
                                continue;
                            }

                            // Ensures we are the only ones
                            job = list.get(0);
                            if (job == null) {
                                // Nothing happened, so we do not wait for more
                                continue;
                            }

                            doWait = true;
                            try {
                                em.refresh(job, LockModeType.PESSIMISTIC_WRITE);
                            } catch (LockTimeoutException e) {
                                LOGGER.debug("Could not lock job %s", job);
                                Scheduler.notifyRunners();
                                continue;
                            }

                            if (job.getState() != ResourceState.READY) {
                                LOGGER.debug("Job state is not READY anymore", job);
                                Scheduler.notifyRunners();
                                continue;
                            }

                            LOGGER.debug("Next task status run: %s", job);

                            // Set the state status LOCKING
                            try {
                                job.setState(ResourceState.LOCKING);
                                transaction.boundary();
                            } catch (RollbackException e) {
                                LOGGER.debug("Could not JPA lock %s", job);
                                continue;
                            }

                            LOGGER.debug("Job %s was JPA locked", job);

                            this.setName(name + "/" + job);
                            try {
                                job.run(em);

                                LOGGER.info("Job %s has started", job);
                            } catch (LockException e) {
                                // We could not lock the resources: update the job state
                                LOGGER.info("Could not lock all the resources for job %s [%s]", job, e.getMessage());
                                job.setState(ResourceState.WAITING);
                                job.updateStatus();
                            } catch (Throwable t) {
                                LOGGER.warn(t, "Got a trouble while launching job [%s]", job);
                                job.setState(ResourceState.ERROR);
                            }
                            transaction.commit();
                            LOGGER.info("Finished launching %s", job);
                            this.setName(name);
                    } catch (RollbackException e) {
                        LOGGER.warn("Rollback exception for %s", job);
                    } finally {
                        if (job != null) {
                            LOGGER.debug("Finished the processing of %s", job);
                        }
                    }
                }
            } finally {
                LOGGER.info("Shutting down job runner");
                counter.del();
            }
        }
    }

}
