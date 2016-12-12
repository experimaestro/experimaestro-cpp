package net.bpiwowar.xpm.scheduler;

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
import net.bpiwowar.xpm.connectors.Connector;
import net.bpiwowar.xpm.connectors.LocalhostConnector;
import net.bpiwowar.xpm.connectors.NetworkShare;
import net.bpiwowar.xpm.connectors.NetworkShareAccess;
import net.bpiwowar.xpm.connectors.SingleHostConnector;
import net.bpiwowar.xpm.connectors.XPMConnector;
import net.bpiwowar.xpm.connectors.XPMProcess;
import net.bpiwowar.xpm.exceptions.CloseException;
import net.bpiwowar.xpm.exceptions.LockException;
import net.bpiwowar.xpm.exceptions.XPMRuntimeException;
import net.bpiwowar.xpm.locks.Lock;
import net.bpiwowar.xpm.manager.experiments.Experiment;
import net.bpiwowar.xpm.manager.experiments.TaskReference;
import net.bpiwowar.xpm.utils.CloseableIterable;
import net.bpiwowar.xpm.utils.CloseableIterator;
import net.bpiwowar.xpm.utils.Heap;
import net.bpiwowar.xpm.utils.ThreadCount;
import net.bpiwowar.xpm.utils.log.Logger;
import org.apache.commons.dbcp2.DriverManagerConnectionFactory;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
    public static final int JOB_CHECKING_LATENCY = 60;

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
    private final ThreadLocal<Connection> _connection = new ThreadLocal<>();

    /**
     * Messenger
     */
    private final MessengerThread messengerThread;

    /**
     * Locks database objects manager
     */
    private final DatabaseObjects<Lock, Dependency> locks;

    private LocalhostConnector localhostConnector;

    /**
     * Listeners
     */
    HashSet<Listener> listeners = new HashSet<>();

    /**
     * True when the application is stopping
     */
    Boolean stopping = false;

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
    private DatabaseObjects<Resource, Void> resources;

    /**
     * The queue for notifications
     */
    private LongOpenHashSet changedResources = new LongOpenHashSet();

    /**
     * The network shares
     */
    private DatabaseObjects<NetworkShare, Void> networkShares;
    private DatabaseObjects<Experiment, Void> experiments;
    private DatabaseObjects<TaskReference, Void> taskReferences;

    private DatabaseObjects<Connector, Void> connectors;

    /**
     * Current version of the database (used to run incremental SQL script updates)
     */
    final static int DBVERSION = 5;

    private XPMConnector xpmConnector;

    private final PoolingDataSource<PoolableConnection> dataSource;
    private String URL;

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

        String connectURI = format("jdbc:hsqldb:file:%s/xpm.db;shutdown=true", baseDirectory);
        DriverManagerConnectionFactory connectionFactory = new DriverManagerConnectionFactory(connectURI, null);
        PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(connectionFactory, null);
        ObjectPool<PoolableConnection> connectionPool = new GenericObjectPool<>(poolableConnectionFactory);
        poolableConnectionFactory.setPool(connectionPool);
        dataSource = new PoolingDataSource<>(connectionPool);

        try (Connection connection = getConnection()) {
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
                        LOGGER.info("Upgrading database to version", version + 1);
                        String resourcename = format("/db/update-%04d.sql", version);
                        try (final InputStream stream = Scheduler.class.getResourceAsStream(resourcename);
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

            resources = new DatabaseObjects<>("Resources", Resource::create);
            networkShares = new DatabaseObjects<>("NetworkShare", NetworkShare::create);
            experiments = new DatabaseObjects<>("Experiments", Experiment::create);
            taskReferences = new DatabaseObjects<>("TaskReferences", TaskReference::create);
            connectors = new DatabaseObjects<>("Connectors", Connector::create);
            locks = new DatabaseObjects<>("Locks", Lock::create);

            // Find or create localhost launcher
            localhostConnector = (LocalhostConnector) Connector.findByIdentifier(LocalhostConnector.IDENTIFIER);
            if (localhostConnector == null) {
                localhostConnector = new LocalhostConnector();
                localhostConnector.save();
            }

            xpmConnector = (XPMConnector) Connector.findByIdentifier(XPMConnector.ID);
            if (xpmConnector == null) {
                xpmConnector = new XPMConnector();
                xpmConnector.save();
            }

            LOGGER.info("Loaded connectors (local=%d, xpm=%d)", localhostConnector.getId(), xpmConnector.getId());

            // Add a shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(Scheduler.this::close));

            // Cleanup dandling processes


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

            // Scheduler initialization
            new Thread("scheduler start") {
                @Override
                public void run() {
                    // Cleanup
                    cleanup();

                    // Loop over resources in state RUNNING
                    try (final CloseableIterable<Resource> resources = resources(EnumSet.of(ResourceState.RUNNING))) {
                        for (Resource resource : resources) {
                            try {
                                Job job = (Job) resource;
                                if (job.getProcess() != null) {
                                    job.getProcess().init(job);
                                } else {
                                    // FIXME: should do something smarter
                                    job.setState(ResourceState.ERROR);
                                    LOGGER.error("No process attached to running job [%s]. New status is: %s", job, job.getState());
                                }
                                job.updateStatus();

                            } catch (SQLException e) {
                                LOGGER.warn(e, "SQL exception while updating job %s", resource);
                            }
                        }
                    } catch (SQLException e) {
                        LOGGER.error(e, "Could not retrieve the list of jobs");
                    } catch (CloseException e) {
                        LOGGER.warn(e, "Error while closing the iterator");
                    }

                    // Loop over resources for which we need to notify (Scheduler stopped before or during the process)
                    try (final CloseableIterable<Resource> changed = resources.find(Resource.SELECT_BEGIN
                            + " WHERE oldStatus != status", null)) {
                        changed.forEach(r -> addChangedResource(r));
                    } catch (SQLException | CloseException e) {
                        LOGGER.error(e, "Could not retrieve the list of jobs");
                    }
                }
            }.start();


            // Start the thread that start the jobs
            LOGGER.info("Starting the job runner thread");
            readyJobSemaphore.setValue(true);
            runner = new JobRunner("JobRunner");
            runner.start();
            runningThreadsCounter.add();


            executorService = Executors.newFixedThreadPool(1);

            // Check every 5 minutes if a running job has finished (in case we missed the notification)
            scheduler.scheduleAtFixedRate(new RunningJobChecker(), 0, 5, TimeUnit.MINUTES);


            LOGGER.info("Done - ready status work now");
        }
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
     * @param connector The single host launcher where this
     * @param path      The path on the launcher
     */
    public static void defineShare(String host, String name, SingleHostConnector connector, String path, int priority) throws SQLException {
        // Save the launcher in DB if necessary
        if (!connector.inDatabase()) {
            connector.save();
        }

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
            final long deltaMS = units.toMillis(rate);
            try {
                try (final XPMStatement statement = statement("SELECT last_update FROM Processes WHERE resource=?")
                        .setLong(1, process.getJob().getId()).execute();
                     final XPMResultSet rs = statement.singleResultSet()) {

                    final long lastUpdate = rs.getTimeStamp(1).getTime();
                    final long currentTime = System.currentTimeMillis();
                    if (currentTime - lastUpdate < deltaMS) {
                        // Skip update if we got news
                        return;
                    }
                }
                process.check(true, Scheduler.JOB_CHECKING_LATENCY);
            } catch (Exception e) {
                LOGGER.error(e, "Error while checking job [%s]: %s", process.getJob());
            } finally {
                closeConnection();
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
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    /**
     * Remove a listener
     *
     * @param listener The listener status remove
     */
    public void removeListener(Listener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    /**
     * Notify
     */
    public void notify(Message message) {
        synchronized (listeners) {
            for (Listener listener : listeners) {
                try {
                    listener.notify(message);
                } catch (RuntimeException e) {
                    LOGGER.warn("Exception when notifying %s / %s", message, e);
                }
            }
        }
    }

    protected boolean isStopping() {
        return stopping;
    }

    /**
     * Shutdown the scheduler
     */
    synchronized public void close() {
        if (INSTANCE == null) {
            return;
        }

        synchronized (stopping) {
            if (stopping) {
                try {
                    wait();
                    return;
                } catch (InterruptedException e) {
                    throw new XPMRuntimeException(e, "Error while waiting the process to stop");
                }
            }
            stopping = true;
        }

        LOGGER.info("Stopping the scheduler");

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
            LOGGER.error("Shutting down executor service");
            executorService.shutdown();
        }


        LOGGER.info("Closing database");
        try (Connection connection = getConnection(); Statement st = connection.createStatement()) {
            st.execute("SHUTDOWN");
        } catch (SQLException e) {
            LOGGER.error(e, "Error while shuting down the database");
        }

        try {
            dataSource.close();
        } catch (Exception e) {
            LOGGER.error(e, "Error while closing the data source");
        }

        INSTANCE = null;

        synchronized (stopping) {
            notifyAll();
        }
        LOGGER.info("Scheduler stopped");
    }

    public DatabaseObjects<Resource, Void> resources() {
        return resources;
    }

    public void sendMessage(Resource destination, Message message) {
        synchronized (messages) {
            LOGGER.debug("Sending message %s", message);
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

    public DatabaseObjects<NetworkShare, Void> networkShares() {
        return networkShares;
    }

    static public Connection getConnection() {
        try {
            Scheduler scheduler = get();
            Connection connection = scheduler._connection.get();
            if (connection == null || connection.isClosed()) {
                try {
                    connection = scheduler.dataSource.getConnection();
                    connection.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
                    connection.setAutoCommit(true);
                    scheduler._connection.set(connection);
                    return connection;
                } catch (SQLException e) {
                    throw new XPMRuntimeException(e);
                }
            }
            return connection;
        } catch (SQLException e) {
            throw new XPMRuntimeException(e, "Could not create SQL connection");
        }
    }

    static public void closeConnection() {
        Scheduler scheduler = get();
        Connection connection = scheduler._connection.get();
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                LOGGER.error(e, "Could not close connection");
            } finally {
                scheduler._connection.remove();
            }
        }
    }

    public DatabaseObjects<Connector, Void> connectors() {
        return connectors;
    }


    public static PreparedStatement prepareStatement(String sql, Object... values) throws SQLException {
        final PreparedStatement preparedStatement = getConnection().prepareStatement(sql);
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
        return new XPMStatement(getConnection(), sql);
    }

    public DatabaseObjects<Lock, Dependency> locks() {
        return locks;
    }

    public DatabaseObjects<Experiment, Void> experiments() {
        return experiments;
    }

    public DatabaseObjects<TaskReference, Void> taskReferences() {
        return taskReferences;
    }

    /**
     * Sets the base URL for the web server
     *
     * @param URL The base URL
     */
    public void setURL(String URL) {
        this.URL = URL;
    }


    /**
     * Gets the base URL
     *
     * @return The base URL
     */
    public String getURL() {
        return URL;
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
                                synchronized (job) {
                                    job.updateStatus();

                                    if (!job.checkProcess()) {
                                        continue;
                                    }
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
                                        job.updateStatus();
                                    } catch (Throwable t) {
                                        LOGGER.warn(t, "Got a trouble while launching job [%s]", job);
                                        job.setState(ResourceState.ERROR);
                                    } finally {
                                        this.setName(name);
                                    }
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
        public Notifier() throws SQLException {
            super("Notifier");

            // Retrieve not resources for which we did not have the time
            final CallableStatement statement = Scheduler.getConnection().prepareCall("SELECT id FROM Resources WHERE status <> oldStatus");
            statement.execute();
            final ResultSet resultSet = statement.getResultSet();
            while (resultSet.next()) {
                changedResources.add(resultSet.getLong(1));
            }
        }

        @Override
        public void run() {
            try {
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

                        // Retrieve the resource that changed - and lock it
                        Resource fromResource;
                        try {
                            fromResource = Resource.getById(resourceId);
                        } catch (SQLException e) {
                            LOGGER.error("Could not retrieve resource with ID %d", resourceId);
                            throw new RuntimeException(e);
                        }


                        try (CloseableIterator<Dependency> dependencies = fromResource.getOutgoingDependencies(true)) {
                            LOGGER.debug("Notifying dependencies from %s", fromResource);

                            while (dependencies.hasNext()) {
                                Dependency dep = dependencies.next();
                                final Resource to = dep.getTo();
                                assert dep.status != DependencyStatus.UNACTIVE;
                                try {
                                    // when the dependency status is null, the dependency is not active anymore
                                    LOGGER.debug("Notifying dependency: [%s] status [%s]; current dep. state=%s", fromResource, to, dep.status);
                                    // Preserves the previous state
                                    to.updatedDependency(dep);
                                } catch (RuntimeException e) {
                                    // FIXME: Should schedule a full update of resource
                                    LOGGER.error(e, "Got an exception while notifying [%s]", fromResource);
                                }
                            }
                        }

                        // OK - update db
                        final XPMStatement updateStatus = statement("UPDATE Resources SET oldStatus = ? WHERE id = ?");
                        updateStatus.setInt(1, fromResource.getState().value());
                        updateStatus.setLong(2, fromResource.getId());
                        updateStatus.executeUpdate();
                    } catch (CloseException e) {
                        LOGGER.error(e, "Caught exception while closing iterator");
                    } catch (Exception e) {
                        LOGGER.error(e, "Caught exception in notifier");
                    }
                }
            } finally {
                LOGGER.info("Stopping notifier thread");
                runningThreadsCounter.del();
                closeConnection();
            }
        }
    }

    /**
     * Checks running jobs
     */
    private class RunningJobChecker implements Runnable {
        @Override
        public void run() {
            try (Connection ignored = getConnection()) {
                // TODO schedule latter if updated
                try (final CloseableIterable<Resource> resources = Scheduler.this.resources(EnumSet.of(ResourceState.RUNNING))) {
                    for (Resource resource : resources) {
                        Job job = (Job) resource;
                        try {
                            XPMProcess process = job.getProcess();
                            if (process != null && !process.isRunning(JOB_CHECKING_LATENCY)) {
                                Scheduler.this.sendMessage(job, new EndOfJobMessage(process.exitValue(false), process.exitTime()));
                            }
                        } catch (Throwable e) {
                            LOGGER.error(e, "could not check if job %s is running", job);
                        }
                    }
                }
            } catch (SQLException | CloseException e) {
                LOGGER.error(e, "Error while checking running jobs");
            }
        }
    }

    /**
     * Retrieve jobs that are marked as not running but have processes
     *
     * @throws SQLException
     */
    void cleanup() {
        try (final XPMStatement statement = statement("SELECT r.id FROM Processes p, Resources r WHERE p.resource = r.id and status != ?")
                .setLong(1, ResourceState.RUNNING.value()).execute();
             final XPMResultSet resultSet = statement.resultSet()) {
            if (resultSet == null) return;
            while (resultSet.next()) {
                final long rid = resultSet.getLong(1);
                try {
                    final Resource resource = Resource.getById(rid);
                    resource.setState(ResourceState.RUNNING);
                    resource.updateStatus();
                } catch (Throwable e) {
                    LOGGER.error(e, "[cleanup] Could not update resource %d", rid);
                }
            }
        } catch (SQLException e) {
            LOGGER.warn(e, "Could not cleanup");
        }

    }

}
