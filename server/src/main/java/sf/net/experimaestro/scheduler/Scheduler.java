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

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.apache.commons.lang.mutable.MutableBoolean;
import sf.net.experimaestro.connectors.XPMProcess;
import sf.net.experimaestro.exceptions.CloseException;
import sf.net.experimaestro.exceptions.LockException;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.utils.*;
import sf.net.experimaestro.utils.log.Logger;

import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.io.File;
import java.sql.Connection;
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

    /**
     * Whether we should look at the list of ready jobs or not
     */
    final MutableBoolean readyJobSemaphore = new MutableBoolean(false);

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
     * The job runner (just one)
     */
    JobRunner runner;

    /**
     * Asynchronous notification
     */
    private Notifier notifier;


    /**
     * A query that retrieves jobs that are ready, ordered by decreasing priority
     */
    CriteriaQuery<Job> readyJobsQuery;
    /**
     * Number of running runners
     */
    private ThreadCount runningThreadsCounter = new ThreadCount();
    private Timer resourceCheckTimer;

    /**
     * Messenger
     */
    private final MessengerThread messengerThread;

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

        /* From HSQLDB http://hsqldb.org/doc/guide/sessions-chapt.html#snc_tx_mvcc

        In MVCC mode
        - locks are at the row level
        - no shared (i.e. read) locks
        - in TRANSACTION_READ_COMMITTED mode: if a session wants to read/write a row that was written by another one => wait
        - in TRANSACTION_REPEATABLE_READ: if a session wants to write the same row than another one => exception
        */
        properties.put("hibernate.connection.isolation", String.valueOf(Connection.TRANSACTION_READ_COMMITTED));

        ArrayList<Class<?>> loadedClasses = new ArrayList<>();
        ServiceLoader<PersistentClassesAdder> services = ServiceLoader.load(PersistentClassesAdder.class);
        for (PersistentClassesAdder service : services) {
            service.add(loadedClasses);
        }

        properties.put(org.hibernate.jpa.AvailableSettings.LOADED_CLASSES, loadedClasses);

        entityManagerFactory = Persistence.createEntityManagerFactory("net.bpiwowar.experimaestro", properties);

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
        runner = new JobRunner("JobRunner");
        runner.start();
        runningThreadsCounter.add();

        // Start the thread that notify dependencies
        LOGGER.info("Starting the job runner thread");
        notifier = new Notifier();
        notifier.start();
        runningThreadsCounter.add();

        // Start the thread that notify dependencies
        LOGGER.info("Starting the messager thread");
        messengerThread = new MessengerThread();
        messengerThread.start();
        runningThreadsCounter.add();


        executorService = Executors.newFixedThreadPool(1);


        LOGGER.info("Done - ready status work now");
    }

    public static Scheduler get() {
        return INSTANCE;
    }

    public static void notifyRunners() {
        final MutableBoolean semaphore = get().readyJobSemaphore;
        synchronized (semaphore) {
            semaphore.setValue(true);
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

        LOGGER.info("Stopping the scheduler");

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
        notifier.interrupt();
        messengerThread.interrupt();

        // Wait for all threads to complete
        runningThreadsCounter.resume();
        runner = null;
        notifier = null;

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
                while (!isStopping()) {
                    // ... and wait if we were not lucky (or there were no tasks)
                    synchronized (readyJobSemaphore) {

                        while (!readyJobSemaphore.booleanValue()) {
                            try {
                                LOGGER.debug("Going to sleep [%s]...", name);
                                readyJobSemaphore.wait();
                                LOGGER.debug("Waking up [%s]...", name);
                            } catch (InterruptedException e) {
                                // The server stopped
                                break;
                            }
                        }

                        // Set it to false
                        readyJobSemaphore.setValue(false);
                    }


                    // Try the next task
                    try (Transaction transaction = Transaction.create()) {
                        LOGGER.debug("Searching for ready jobs");

                        final EntityManager em = transaction.em();

                        /* TODO: consider a smarter way to retrieve good candidates (e.g. using a bloom filter for tokens) */
                        TypedQuery<Job> query = em.createQuery(Scheduler.this.readyJobsQuery);
                        List<Job> list = query.getResultList();

                        for (Job job : list) {
                            job.lock(transaction, true);
                            em.refresh(job);

                            LOGGER.debug("Looking at %s", job);

                            if (job.getState() != ResourceState.READY) {
                                LOGGER.debug("Job state is not READY anymore", job);
                                transaction.clearLocks();
                                continue;
                            }

                            // Checks the tokens
                            boolean tokensAvailable = true;
                            for (Dependency dependency : job.getDependencies()) {
                                if (dependency instanceof TokenDependency) {
                                    TokenDependency tokenDependency = (TokenDependency) dependency;
                                    if (!tokenDependency.canLock()) {
                                        LOGGER.debug("Token dependency [%s] prevents running job", tokenDependency);
                                        tokensAvailable = false;
                                        break;
                                    }
                                    LOGGER.debug("OK to lock token dependency: %s", tokenDependency);
                                }
                            }
                            if (!tokensAvailable) {
                                // Remove locks
                                transaction.clearLocks();
                                continue;
                            }

                            this.setName(name + "/" + job);
                            try {
                                job.run(em, transaction);

                                LOGGER.info("Job %s has started", job);
                            } catch (LockException e) {
                                // We could not lock the resources: update the job state
                                LOGGER.info("Could not lock all the resources for job %s [%s]", job, e.getMessage());
                                job.setState(ResourceState.WAITING);
                                job.updateStatus();
                                transaction.boundary();
                                LOGGER.info("Finished launching %s", job);
                            } catch (RollbackException e) {
                                LOGGER.error(e, "Rollback exception");
                                synchronized (readyJobSemaphore) {
                                    readyJobSemaphore.setValue(true);
                                }

                                break;
                            } catch (Throwable t) {
                                LOGGER.warn(t, "Got a trouble while launching job [%s]", job);
                                job.setState(ResourceState.ERROR);
                                transaction.boundary();
                            } finally {
                                this.setName(name);
                            }
                        }

                    } catch (RollbackException e) {
                        LOGGER.warn("Rollback exception");
                    } catch (Exception e) {
                        // FIXME: should do something smarter
                        LOGGER.error(e, "Caught an exception");
                    } finally {
                    }
                }
            } finally {
                LOGGER.info("Shutting down job runner");
                runningThreadsCounter.del();
            }
        }
    }

    final static private class MessagePackage extends Heap.DefaultElement<MessagePackage> implements Comparable<MessagePackage> {
        public Message message;
        public long destination;
        public long timestamp;

        public MessagePackage(Message message, Resource destination, long timestamp) {
            this.message = message;
            this.destination = destination.getId();
            this.timestamp = timestamp;
        }

        @Override
        public int compareTo(MessagePackage o) {
            return Long.compare(this.timestamp, o.timestamp);
        }
    }

    /**
     * Queue for messages
     */
    Heap<MessagePackage> messages = new Heap<>();

    public void sendMessage(Resource destination, Message message) {
        synchronized (messages) {
            // Add the message (with a timestamp one ms before the time, to avoid locks)
            messages.add(new MessagePackage(message, destination, System.currentTimeMillis() - 1));
            // Notify
            messages.notify();
        }
    }

    /** Time added when rescheduling */
    static final long  RESCHEDULING_DELTA_TIME = 250;

    /**
     * The message thread
     */
    private class MessengerThread extends Thread {
        public MessengerThread() {
            super("Message");
        }

        @Override
        public void run() {
            LOGGER.info("Starting messager thread");

            mainLoop: while (!isStopping()) {
                try {
                    // Get the next resource ID
                    final MessagePackage messagePackage;
                    while (true) {
                        synchronized (messages) {
                            LOGGER.debug("Waiting for the next message");
                            long wait = 0;
                            if (!messages.isEmpty()) {
                                wait = messages.peek().timestamp - System.currentTimeMillis();
                                LOGGER.debug("Next message has a waiting time of %d", wait);
                            }

                            if (wait >= 0) {
                                try {
                                    messages.wait(wait);
                                } catch (InterruptedException e) {
                                    if (isStopping())
                                        break mainLoop;
                                }
                                continue;

                            } else {
                                messagePackage = messages.pop();
                                break;
                            }
                        }
                    }

                    // Notify all the dependencies
                    try {
                        Transaction.run((em, t) -> {
                            // Retrieve the resource that changed - and lock it
                            Resource destination = em.find(Resource.class, messagePackage.destination);
                            Resource.lock(t, messagePackage.destination, true, 0);
                            em.refresh(destination);
                            LOGGER.debug("Sending message %s to %s", messagePackage.message, destination);
                            destination.notify(t, em, messagePackage.message);
                        });
                    } catch (Throwable e) {
                        LOGGER.warn("Error [%s] while notifying %s - Rescheduling", e.toString(), messagePackage.destination);
                        synchronized (messages) {
                            messagePackage.timestamp = System.currentTimeMillis() + RESCHEDULING_DELTA_TIME;
                            messages.add(messagePackage);
                        }
                    }

                } catch (Throwable e) {
                    LOGGER.error("Caught exception in notifier", e);
                }
            }

            runningThreadsCounter.del();
            LOGGER.info("Stopping notifier thread");
        }


    }


    /**
     * The queue for notifications
     */
    private LongOpenHashSet changedResources = new LongOpenHashSet();

    /**
     * Adds a changed resource to the queue
     */
    void addChangedResource(Resource resource) {
        synchronized (changedResources) {
            changedResources.add(resource.getId());

            // Notify
            changedResources.notify();
        }
    }

    /**
     * The notifier thread
     */
    private class Notifier extends Thread {
        public Notifier() {
            super("Notifier");
        }

        @Override
        public void run() {
            LOGGER.info("Starting notifier thread");

            while (!isStopping()) {
                try {
                    final long resourceId;

                    // Get the next resource ID
                    synchronized (changedResources) {
                        if (changedResources.isEmpty()) {
                            try {
                                changedResources.wait();
                            } catch (InterruptedException e) {
                            }
                            continue;

                        } else {
                            final LongIterator iterator = changedResources.iterator();
                            resourceId = iterator.next();
                            iterator.remove();
                        }
                    }

                    LOGGER.debug("Notifying dependencies from R%d", resourceId);
                    // Notify all the dependencies
                    Transaction.run((em, t) -> {

                        // Retrieve the resource that changed - and lock it
                        Resource fromResource = em.find(Resource.class, resourceId);
                        fromResource.lock(t, false);

                        Collection<Dependency> dependencies = fromResource.getOutgoingDependencies();
                        LOGGER.info("Notifying dependencies from %s [%d]", fromResource, dependencies.size());

                        for (Dependency dep : dependencies) {
                            if (dep.status == DependencyStatus.UNACTIVE) {
                                LOGGER.debug("We won't notify [%s] status [%s] since the dependency is not active", fromResource, dep.getTo());

                            } else
                                try {
                                    // when the dependency status is null, the dependency is not active anymore
                                    LOGGER.debug("Notifying dependency: [%s] status [%s]; current dep. state=%s", fromResource, dep.getTo(), dep.status);
                                    // Preserves the previous state
                                    DependencyStatus beforeState = dep.status;

                                    if (dep.update()) {
                                        final Resource depResource = dep.getTo();
                                        if (!ResourceState.NOTIFIABLE_STATE.contains(depResource.getState())) {
                                            LOGGER.debug("We won't notify resource %s since its state is %s", depResource, depResource.getState());
                                            continue;
                                        }

                                        // Queue this change in dependency state
                                        depResource.notify(t, em, new DependencyChangedMessage(dep, beforeState, dep.status));

                                    } else {
                                        LOGGER.debug("No change in dependency status [%s -> %s]", beforeState, dep.status);
                                    }
                                } catch (RuntimeException e) {
                                    LOGGER.error(e, "Got an exception while notifying [%s]", fromResource);
                                }
                        }
                    });

                } catch (Exception e) {
                    LOGGER.error("Caught exception in notifier", e);
                }
            }

            runningThreadsCounter.del();
            LOGGER.info("Stopping notifier thread");
        }
    }
}
