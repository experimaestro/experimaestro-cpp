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
import sf.net.experimaestro.connectors.Connector;
import sf.net.experimaestro.connectors.LocalhostConnector;
import sf.net.experimaestro.connectors.NetworkShare;
import sf.net.experimaestro.connectors.NetworkShareAccess;
import sf.net.experimaestro.connectors.SingleHostConnector;
import sf.net.experimaestro.connectors.XPMConnector;
import sf.net.experimaestro.connectors.XPMProcess;
import sf.net.experimaestro.exceptions.CloseException;
import sf.net.experimaestro.exceptions.LockException;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.utils.CloseableIterable;
import sf.net.experimaestro.utils.CloseableIterator;
import sf.net.experimaestro.utils.Heap;
import sf.net.experimaestro.utils.ThreadCount;
import sf.net.experimaestro.utils.log.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Properties;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

/**
 * The scheduler
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
final public class Scheduler {
    /**
     * Time added when rescheduling
     */
    static final long RESCHEDULING_DELTA_TIME = 250;

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
     * Database connection
     */
    private final Connection connection;

    /**
     * Messenger
     */
    private final MessengerThread messengerThread;

    private final DatabaseObjects<Lock> locks;

    private LocalhostConnector localhostConnector;

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
     * Queue for messages
     */
    Heap<MessagePackage> messages = new Heap<>();

    /**
     * Asynchronous notification
     */
    private Notifier notifier;

    /**
     * Number of running runners
     */
    private ThreadCount runningThreadsCounter = new ThreadCount();

    private Timer resourceCheckTimer;

    /**
     * Resources
     */
    private DatabaseObjects<Resource> resources;

    /**
     * The queue for notifications
     */
    private LongOpenHashSet changedResources = new LongOpenHashSet();

    /**
     * The network shares
     */
    private DatabaseObjects<NetworkShare> networkShares;

    private DatabaseObjects<Connector> connectors;

    final static int DBVERSION = 1;

    private XPMConnector xpmConnector;

    /**
     * Initialise the task manager
     *
     * @param baseDirectory The directory where the XPM database will be stored
     */
    public Scheduler(File baseDirectory) throws IOException, ClassNotFoundException, SQLException, CloseException {
        if (INSTANCE != null) {
            throw new XPMRuntimeException("Only one scheduler instance should be created");
        }

        INSTANCE = this;

        // Initialise the database - we do not use any isolation
        Class.forName("org.hsqldb.jdbcDriver");
        connection = DriverManager.getConnection(format("jdbc:hsqldb:file:%s/xpm.db;shutdown=true", baseDirectory));
        connection.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
        connection.setAutoCommit(true);

        // Read the property file
        final File file = new File(baseDirectory, "xpm.ini");
        Properties properties = new Properties();
        if (file.exists()) {
            try (final FileInputStream inStream = new FileInputStream(file)) {
                properties.load(inStream);
            }
        }
        int version = Integer.parseInt(properties.getProperty("db.version", "0"));

        if (version != DBVERSION) {
            final ScriptRunner scriptRunner = new ScriptRunner(connection, true, true);

            if (version == 0) {
                try (final InputStream stream = Scheduler.class.getResourceAsStream("/db/creation.sql");
                     final Reader reader = new InputStreamReader(stream)) {
                    scriptRunner.runScript(reader);
                    version = DBVERSION;
                    properties.setProperty("db.version", Integer.toString(version));
                    try (FileOutputStream out = new FileOutputStream(file)) {
                        properties.store(out, "");
                    }
                }
            } else {
                while (version < DBVERSION) {
                    try (final InputStream stream = Scheduler.class.getResourceAsStream(format("/db/update-%4d.sql", version));
                         final Reader reader = new InputStreamReader(stream)) {
                        scriptRunner.runScript(reader);
                        version++;
                        properties.setProperty("db.version", Integer.toString(version));
                        try (FileOutputStream out = new FileOutputStream(file)) {
                            properties.store(out, "");
                        }
                    }
                }

            }
        }

        resources = new DatabaseObjects<>(connection, "Resources", Resource::create);
        networkShares = new DatabaseObjects<>(connection, "NetworkShare", NetworkShare::create);
        connectors = new DatabaseObjects<>(connection, "Connectors", Connector::create);
        locks = new DatabaseObjects<>(connection, "Locks", Lock::create);

        // Find or create localhost connector
        localhostConnector = (LocalhostConnector) Connector.findByURI(LocalhostConnector.IDENTIFIER);
        if (localhostConnector == null) {
            localhostConnector = new LocalhostConnector();
            localhostConnector.save();
        }

        xpmConnector = (XPMConnector) Connector.findByURI(XPMConnector.ID);
        if (xpmConnector == null) {
            xpmConnector = new XPMConnector();
            xpmConnector.save();
        }

        LOGGER.info("Loaded connectors (local=%d, xpm=%d)", localhostConnector.getId(), xpmConnector.getId());

        // Add a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(Scheduler.this::close));

        // Start the thread that notify dependencies
        LOGGER.info("Starting the notifier thread");
        notifier = new Notifier();
        notifier.start();
        runningThreadsCounter.add();

        // Start the thread that notify dependencies
        LOGGER.info("Starting the messager thread");
        messengerThread = new MessengerThread();
        messengerThread.start();
        runningThreadsCounter.add();

        // Loop over resources in state RUNNING
        try (final CloseableIterable<Resource> resources = resources(EnumSet.of(ResourceState.RUNNING))) {
            for (Resource resource : resources) {
                Job job = (Job) resource;
                if (job.getProcess() != null) {
                    job.getProcess().init(job);
                } else {
                    job.setState(ResourceState.ERROR);
                    LOGGER.error("No process attached to running job [%s]. New status is: %s", job, job.getState());
                }
                job.updateStatus();
            }
        }




        // Start the thread that start the jobs
        LOGGER.info("Starting the job runner thread");
        readyJobSemaphore.setValue(true);
        runner = new JobRunner("JobRunner");
        runner.start();
        runningThreadsCounter.add();


        executorService = Executors.newFixedThreadPool(1);


        LOGGER.info("Done - ready status work now");
    }

    public static Scheduler get() {
        return INSTANCE;
    }

    // ----
    // ---- TaskReference related methods
    // ----

    public static void notifyRunners() {
        final MutableBoolean semaphore = get().readyJobSemaphore;
        synchronized (semaphore) {
            semaphore.setValue(true);
            semaphore.notify();
        }
    }

    /**
     * Defines a share
     *
     * @param host      The host name for the share
     * @param name      The name of the share on the hosts
     * @param connector The single host connector where this
     * @param path      The path on the connector
     */
    public static void defineShare(String host, String name, SingleHostConnector connector, String path, int priority) throws SQLException {
        // Save the connector in DB if necessary
        connector.save();

        NetworkShare networkShare = NetworkShare.find(host, name);

        if (networkShare == null) {
            networkShare = new NetworkShare(host, name);
            networkShare.save();
            final NetworkShareAccess access = new NetworkShareAccess(connector, path, priority);
            networkShare.add(access);
        } else {
            for (NetworkShareAccess access : networkShare.getAccess()) {
                if (access.is(connector)) {
                    // Found it - just update
                    access.setPath(path);
                    access.setPriority(priority);
                    return;
                }
            }

            // Add
            final NetworkShareAccess networkShareAccess = new NetworkShareAccess(connector, path, priority);
            networkShare.add(networkShareAccess);
        }
    }

    /**
     * Check if the process has ended at a given rate
     *
     * @param process
     * @param rate
     * @param units
     * @return
     */
    public ScheduledFuture<?> schedule(final XPMProcess process, int rate, TimeUnit units) {
        return scheduler.scheduleAtFixedRate(() -> {
            try {
                process.check(false);
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
    public CloseableIterable<Resource> resources(EnumSet<ResourceState> states) throws SQLException {
        return Resource.find(states);
    }

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
        for (Listener listener : listeners) {
            listener.notify(message);
        }
    }

    protected boolean isStopping() {
        return stopping;
    }

    /**
     * Shutdown the scheduler
     */
    synchronized public void close() {
        if (connection == null && stopping) {
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
        if (runner != null) {
            runner.interrupt();
        }

        if (notifier != null) {
            notifier.interrupt();
        }

        if (messengerThread != null) {
            messengerThread.interrupt();
        }

        // Wait for all threads to complete
        runningThreadsCounter.resume();
        runner = null;
        notifier = null;

        if (executorService != null) {
            executorService.shutdown();
        }


        LOGGER.info("Closing database");
        try {
            if (!connection.isClosed()) {
                Statement st = connection.createStatement();
                st.execute("SHUTDOWN");
                connection.close();    // if there are no other open connection
            }
        } catch (SQLException e) {
            LOGGER.error(e, "Error while shuting down the database");
        }

        INSTANCE = null;

        LOGGER.info("Scheduler stopped");
    }

    public DatabaseObjects<Resource> resources() {
        return resources;
    }

    public void sendMessage(Resource destination, Message message) {
        synchronized (messages) {
            // Add the message (with a timestamp one ms before the time, to avoid locks)
            messages.add(new MessagePackage(message, destination, System.currentTimeMillis() - 1));
            // Notify
            messages.notify();
        }
    }

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

    public DatabaseObjects<NetworkShare> networkShares() {
        return networkShares;
    }

    public Connection getConnection() {
        return connection;
    }

    public DatabaseObjects<Connector> connectors() {
        return connectors;
    }


    public static PreparedStatement prepareStatement(String sql, Object... values) throws SQLException {
        final PreparedStatement preparedStatement = get().getConnection().prepareStatement(sql);
        for (int i = 0; i < values.length; i++) {
            Object value = getObject(values[i]);
            preparedStatement.setObject(i + 1, value);
        }

        return preparedStatement;
    }

    protected static Object getObject(Object value1) {
        return value1;
    }

    public LocalhostConnector getLocalhostConnector() {
        return localhostConnector;
    }

    public XPMConnector getXPMConnector() {
        return xpmConnector;
    }

    public static XPMStatement statement(String sql) throws SQLException {
        return new XPMStatement(get().connection, sql);
    }

    public DatabaseObjects<Lock> locks() {
        return locks;
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

                    LOGGER.debug("Searching for ready jobs");
                    try (final CloseableIterable<Resource> resources = Resource.find(EnumSet.of(ResourceState.READY))) {
                        /* TODO: consider a smarter way to retrieve good candidates (e.g. using a bloom filter for tokens) */
                        for (Resource resource : resources) {
                            try {
                                Job job = (Job) resource;
                                this.setName(name + "/" + job);

                                LOGGER.debug("Looking at %s", job);

                                if (job.getState() != ResourceState.READY) {
                                    LOGGER.debug("Job %s state is not in state READY anymore but [%s]", job, job.getState());
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
                                    continue;
                                }

                                try {
                                    job.run();

                                    LOGGER.info("Job %s has started", job);
                                } catch (LockException e) {
                                    // We could not lock the resources: update the job state
                                    LOGGER.info("Could not lock all the resources for job %s [%s]", job, e.getMessage());
                                    job.setState(ResourceState.WAITING);
                                    job.updateStatus();
                                    LOGGER.info("Finished launching %s", job);
                                } catch (Throwable t) {
                                    LOGGER.warn(t, "Got a trouble while launching job [%s]", job);
                                    job.setState(ResourceState.ERROR);
                                } finally {
                                    this.setName(name);
                                }

                            } catch (Exception e) {
                                // FIXME: should do something smarter
                                LOGGER.error(e, "Caught an exception");
                            } finally {
                            }
                        }
                    } catch (CloseException e) {
                        LOGGER.error(e, "SQL exception while retrieving ready jobs");
                    } catch (SQLException e) {
                        LOGGER.error(e, "SQL error while retrieving ready jobs");
                    }
                }
            } finally {
                LOGGER.info("Shutting down job runner");
                runningThreadsCounter.del();
            }
        }
    }

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

            mainLoop:
            while (!isStopping()) {
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
                        // Retrieve the resource that changed - and lock it
                        Resource destination = Resource.getById(messagePackage.destination);
                        LOGGER.debug("Sending message %s to %s", messagePackage.message, destination);
                        destination.notify(messagePackage.message);
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
     * The notifier thread for resources that changed of state
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

                    // Retrieve the resource that changed - and lock it
                    Resource fromResource;
                    try {
                        fromResource = Resource.getById(resourceId);
                    } catch (SQLException e) {
                        LOGGER.error("Could not retrieve resource with ID %d", resourceId);
                        throw new RuntimeException(e);
                    }


                    try (CloseableIterator<Dependency> dependencies = fromResource.getOutgoingDependencies()) {
                        LOGGER.info("Notifying dependencies from %s", fromResource);

                        while (dependencies.hasNext()) {
                            Dependency dep = dependencies.next();
                            final Resource to = dep.getTo();
                            if (dep.status == DependencyStatus.UNACTIVE) {
                                LOGGER.debug("We won't notify [%s] status [%s] since the dependency is not active", fromResource, to);

                            } else
                                try {
                                    // when the dependency status is null, the dependency is not active anymore
                                    LOGGER.debug("Notifying dependency: [%s] status [%s]; current dep. state=%s", fromResource, to, dep.status);
                                    // Preserves the previous state
                                    to.updatedDependency(dep);
                                } catch (RuntimeException e) {
                                    LOGGER.error(e, "Got an exception while notifying [%s]", fromResource);
                                }
                        }
                    }
                } catch (CloseException e) {
                    LOGGER.error(e, "Caught exception while closing iterator");
                } catch (Exception e) {
                    LOGGER.error(e, "Caught exception in notifier");
                }
            }

            runningThreadsCounter.del();
            LOGGER.info("Stopping notifier thread");
        }
    }
}
