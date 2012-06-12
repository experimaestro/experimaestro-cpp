/*
 *
 *  This file is part of experimaestro.
 *  Copyright (c) 2011 B. Piwowarski <benjamin@bpiwowar.net>
 *
 *  experimaestro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  experimaestro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package sf.net.experimaestro.scheduler;

import bpiwowar.argparser.utils.ReadLineIterator;
import com.jcraft.jsch.JSchException;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import com.sleepycat.persist.model.Relationship;
import com.sleepycat.persist.model.SecondaryKey;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.locks.LockType;
import sf.net.experimaestro.locks.UnlockableException;
import sf.net.experimaestro.manager.js.JSLauncher;
import sf.net.experimaestro.utils.PID;
import sf.net.experimaestro.utils.log.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static java.lang.String.format;

/**
 * The most general type of object manipulated by the server (can be a server, a
 * task, or a data)
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Entity
public abstract class Resource implements Comparable<Resource> {
    final static private Logger LOGGER = Logger.getLogger();

    public static final String LOCK_EXTENSION = ".lock";
    public static final String STATUS_EXTENSION = ".status";

    /**
     * Our connector to the host where the resource is located
     */
    Connector connector = new LocalhostConnector();

    /**
     * The task identifier
     */
    @PrimaryKey
    String identifier;

    /**
     * Groups this resource belongs to
     */
    @SecondaryKey(name = "groups", relate = Relationship.MANY_TO_MANY)
    Set<String> groups = new TreeSet<String>();

    /**
     * True when the resource has been generated
     */
    protected ResourceState state;

    /**
     * The access mode
     */
    private LockMode lockmode;

    /**
     * Task manager
     */
    transient Scheduler scheduler;

    /**
     * Our set of listeners (resources that are listening to changes in the
     * state of this resource)
     */
    @SecondaryKey(name = "listeners", relate = Relationship.ONE_TO_MANY, relatedEntity = Resource.class)
    Set<String> listeners = new TreeSet<String>();

    /**
     * If the resource is currently locked
     */
    boolean locked;

    /// Useful derived identifiers
    private transient String statusFileIdentifier, lockFileIdentifier;

    protected Resource() {
    }

    /**
     * Constructs a resource
     *
     * @param taskManager
     * @param identifier
     * @param mode
     */
    public Resource(Scheduler taskManager, String identifier, LockMode mode) {
        this.scheduler = taskManager;
        this.identifier = identifier;
        this.lockmode = mode;
        this.statusFileIdentifier = identifier + STATUS_EXTENSION;
        this.lockFileIdentifier = identifier + LOCK_EXTENSION;
    }

    /**
     * Register a task that waits for our output
     */
    synchronized public void register(Job job) {
        // Copy the string to avoid holding the objects to notify in memory
        listeners.add(new String(job.getIdentifier()));
    }

    /**
     * Unregister a task
     */
    public void unregister(Job task) {
        listeners.remove(task);
    }

    /**
     * Called when we have generated the resources (either by running it or
     * producing it)
     *
     * @throws DatabaseException
     */
    void notifyListeners(Object... objects) throws DatabaseException {
        for (String id : listeners) {
            Job resource = (Job) scheduler.getResource(id);
            resource.notify(this, objects);
        }
    }

    /**
     * Compares to another resource (based on the identifier)
     */
    public int compareTo(Resource o) {
        return identifier.compareTo(o.identifier);
    }

    @Override
    public int hashCode() {
        return identifier.hashCode();
    }

    @Override
    public String toString() {
        return identifier.toString();
    }

    /**
     * Update the status of this resource
     */
    boolean updateStatus() throws Exception {
        boolean updated = update();

        // Check if the resource was generated
        if (new File(identifier + ".done").exists()
                && this.getState() != ResourceState.DONE) {
            updated = true;
            this.state = ResourceState.DONE;
        }

        // Check if the resource is locked
        boolean locked = new File(identifier + ".lock").exists();
        updated |= locked != this.locked;
        this.locked = locked;

        return updated;
    }



    static public enum DependencyStatus {
        /**
         * The resource can be used as is
         */
        OK,

        /**
         * The resouce can be used when properly locked
         */
        OK_LOCK,

        /**
         * The resource is not ready yet
         */
        WAIT,

        /**
         * The resource will not be ready given the current state
         */
        ERROR;

        public boolean isOK() {
            return this == OK_LOCK || this == OK;
        }
    }

    /**
     * Can the dependency be accepted?
     *
     * @param locktype
     * @return {@link DependencyStatus#OK} if the dependency is satisfied,
     *         {@link DependencyStatus#WAIT} if it can be satisfied and
     *         {@link DependencyStatus#ERROR} if it cannot be satisfied
     */
    DependencyStatus accept(LockType locktype) {
        LOGGER.debug("Checking lock %s for resource %s (generated %b)",
                locktype, this, getState());

        // Handle simple cases
        if (state == ResourceState.ERROR)
            return DependencyStatus.ERROR;

        if (state == ResourceState.WAITING)
            return DependencyStatus.WAIT;

        // OK, we have to get a look into it
        switch (locktype) {
            case GENERATED:
                return getState() == ResourceState.DONE ? DependencyStatus.OK
                        : DependencyStatus.WAIT;

            case EXCLUSIVE_ACCESS:
                return writers == 0 && readers == 0 ? DependencyStatus.OK_LOCK
                        : DependencyStatus.WAIT;

            case READ_ACCESS:
                switch (lockmode) {
                    case EXCLUSIVE_WRITER:
                    case SINGLE_WRITER:
                        return writers == 0 ? DependencyStatus.OK
                                : DependencyStatus.WAIT;

                    case MULTIPLE_WRITER:
                        return DependencyStatus.OK;
                    case READ_ONLY:
                        return getState() == ResourceState.DONE ? DependencyStatus.OK
                                : DependencyStatus.WAIT;
                }
                break;

            // We need a write access
            case WRITE_ACCESS:
                switch (lockmode) {
                    case EXCLUSIVE_WRITER:
                        return writers == 0 && readers == 0 ? DependencyStatus.OK_LOCK
                                : DependencyStatus.WAIT;
                    case MULTIPLE_WRITER:
                        return DependencyStatus.OK;
                    case READ_ONLY:
                        return DependencyStatus.ERROR;
                    case SINGLE_WRITER:
                        return writers == 0 ? DependencyStatus.OK_LOCK
                                : DependencyStatus.WAIT;
                }
        }

        return DependencyStatus.ERROR;
    }

    /**
     * Tries to lock the resource
     *
     * @param dependency
     * @throws UnlockableException
     */
    public Lock lock(int pid, LockType dependency) throws UnlockableException {
        // Check the dependency status
        switch (accept(dependency)) {
            case WAIT:
                throw new UnlockableException(
                        "Cannot grant dependency %s for resource %s", dependency,
                        this);
            case ERROR:
                throw new RuntimeException(
                        format("Resource %s cannot accept dependency %s", this,
                                dependency));
            case OK_LOCK:
                return connector.createLockFile(identifier + ".LOCK");

            case OK:
                return new StatusLock(PID.getPID(),
                        dependency == LockType.WRITE_ACCESS);

        }

        return null;

    }

    /**
     * @return the generated
     */
    public boolean isGenerated() {
        return getState() == ResourceState.DONE;
    }

    /**
     * Number of readers and writers
     */
    int writers = 0, readers = 0;

    /**
     * Last check of the status file
     */
    long lastUpdate = 0;

    /**
     * Current list
     */
    transient TreeMap<Integer, Boolean> processMap = new TreeMap<Integer, Boolean>();

    public int getReaders() {
        return readers;
    }

    public int getWriters() {
        return writers;
    }

    /**
     * Update the status of the resource
     *
     * @return A boolean specifying whether something was updated
     * @throws FileNotFoundException If some error occurs while reading status
     */
    public boolean update() throws Exception {
        boolean updated = writers > 0 || readers > 0;

        if (!connector.fileExists(statusFileIdentifier)) {
            writers = readers = 0;
            return updated;
        } else {
            // Check if we need to read the file
            long lastModified = connector.getLastModifiedTime(statusFileIdentifier);
            if (lastUpdate >= lastModified)
                return false;

            lastUpdate = lastModified;

            try {
                for (String line : new ReadLineIterator(connector.getInputStream(statusFileIdentifier))) {
                    String[] fields = line.split("\\s+");
                    if (fields.length != 2)
                        LOGGER.error(
                                "Skipping line %s (wrong number of fields)",
                                line);
                    else {
                        if (fields[1].equals("r"))
                            readers += 1;
                        else if (fields[1].equals("w"))
                            writers += 1;
                        else
                            LOGGER.error("Skipping line %s (unkown mode %s)",
                                    fields[1]);

                        if (processMap != null)
                            processMap.put(Integer.parseInt(fields[0]),
                                    fields[1].equals("w"));
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
        return true;
    }

    /**
     * @param pidFrom     The old PID (or 0 if none)
     * @param pidTo       The new PID (or 0 if none)
     * @param writeAccess True if we need the write access
     * @throws UnlockableException
     */
    void updateStatusFile(int pidFrom, int pidTo, boolean writeAccess)
            throws UnlockableException {
        // --- Lock the resource
        Lock fileLock = connector.createLockFile(lockFileIdentifier);

        try {
            // --- Read the resource status
            update();

            if (pidFrom <= 0) {
                // We are adding a new entry
                PrintWriter out = connector.printWriter(statusFileIdentifier);
                out.format("%d %s%n", pidTo, writeAccess ? "w" : "r");
                out.close();
            } else {
                // We are modifying an entry: rewrite the file
                PrintWriter out = connector.printWriter(statusFileIdentifier + ".tmp");
                processMap.remove(pidFrom);
                if (pidTo > 0)
                    processMap.put(pidTo, writeAccess);
                for (Entry<Integer, Boolean> x : processMap.entrySet())
                    out.format("%d %s%n", x.getKey(), x.getValue() ? "w" : "r");
                out.close();

                connector.renameFile(statusFileIdentifier + ".tmp", statusFileIdentifier);
            }

        } catch (Exception e) {
            throw new UnlockableException(
                    "Status file '%s' could not be created", statusFileIdentifier);
        } finally {
            if (fileLock != null)
                fileLock.dispose();
        }

    }

    /**
     * Defines a status lock
     *
     * @author B. Piwowarski <benjamin@bpiwowar.net>
     */
    public class StatusLock implements Lock {
        private int pid;
        private final boolean writeAccess;

        public StatusLock(int pid, final boolean writeAccess)
                throws UnlockableException {
            this.pid = pid;
            this.writeAccess = writeAccess;
            updateStatusFile(-1, pid, writeAccess);
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    try {
                        updateStatusFile(StatusLock.this.pid, -1, writeAccess);
                    } catch (UnlockableException e) {
                        LOGGER.warn(
                                "Unable to change status file %s (tried to remove pid %d)",
                                statusFileIdentifier, StatusLock.this.pid);
                    }
                }
            });
        }

        /**
         * Change the PID in the status file
         *
         * @throws UnlockableException
         */
        public void changePID(int pid) throws UnlockableException {
            updateStatusFile(this.pid, pid, writeAccess);
            this.pid = pid;
        }

        /*
           * (non-Javadoc)
           *
           * @see bpiwowar.expmanager.locks.Lock#dispose()
           */
        public boolean dispose() {
            try {
                updateStatusFile(pid, -1, writeAccess);
            } catch (UnlockableException e) {
                return false;
            }
            return true;
        }

        /*
           * (non-Javadoc)
           *
           * @see bpiwowar.expmanager.locks.Lock#changeOwnership(int)
           */
        public void changeOwnership(int pid) {
            try {
                updateStatusFile(this.pid, pid, writeAccess);
            } catch (UnlockableException e) {
                return;
            }

            this.pid = pid;
        }
    }

    /**
     * Get the task identifier
     *
     * @return The task unique identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Is the resource locked?
     */
    public boolean isLocked() {
        return locked;
    }

    /**
     * Get the state of the resource
     *
     * @return
     */
    public ResourceState getState() {
        return state;
    }

    /**
     * Checks whether a resource should be kept in main memory (e.g.,
     * running/waiting jobs & monitored resources)
     * <p/>
     * When overriding this method, use
     * <code>super.isActive() || condition</code>
     *
     * @return true if the resource is active
     */
    protected boolean isActive() {
        return !listeners.isEmpty();
    }

    /**
     * Returns the list of listeners
     */
    public Set<String> getListeners() {
        return listeners;
    }

    /**
     * Update the database after a change in state
     */
    void updateDb() {
        try {
            scheduler.store(this);
        } catch (DatabaseException e) {
            LOGGER.error(
                    "Could not update the information in the database for %s",
                    this);
        }
    }

    /**
     * Defines how printing should be done
     */
    static public class PrintConfig {
        public String detailURL;
    }

    /**
     * Writes an HTML description of the resource
     *
     * @param out
     * @param config The configuration for printing
     */
    public void printHTML(PrintWriter out, PrintConfig config) {
        out.format("<div><b>Resource id</b>: %s</h2>", identifier);
        out.format("<div><b>Status</b>: %s</div>", state);

    }
}
