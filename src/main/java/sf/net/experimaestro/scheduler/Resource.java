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

import bpiwowar.argparser.utils.ReadLineIterator;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import com.sleepycat.persist.model.Relationship;
import com.sleepycat.persist.model.SecondaryKey;
import org.apache.commons.vfs2.FileObject;
import sf.net.experimaestro.connectors.Connector;
import sf.net.experimaestro.connectors.SingleHostConnector;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.locks.LockType;
import sf.net.experimaestro.locks.StatusLock;
import sf.net.experimaestro.locks.UnlockableException;
import sf.net.experimaestro.utils.log.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

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

    /** Extension for the lock file */
    public static final String LOCK_EXTENSION = ".lock";

    /** Extension for the file that describes the status of the resource */
    public static final String STATUS_EXTENSION = ".status";

    /** Extension used to mark a produced resource */
    public static final String DONE_EXTENSION = ".done";

    /** Extension for the file containing the return code */
    public static final String CODE_EXTENSION = ".code";

    /** Extension for the file containing the script to run */
    public static final String RUN_EXTENSION = ".run";

    /** Extension for the standard output of a job */
    public static final String OUT_EXTENSION = ".out";

    /** Extension for the standard error of a job */
    public static final String ERR_EXTENSION = ".err";

    /** Extension for the standard input of a job */
    public static final String INPUT_EXTENSION = ".input";
    public static final String GROUP_KEY_NAME = "group";

    /**
     * The task locator
     */
    @PrimaryKey
    ResourceLocator locator;

    /**
     * Group this resource belongs to. Note that name are 0 separated for sorting reasons
     */
    @SecondaryKey(name = GROUP_KEY_NAME, relate = Relationship.MANY_TO_ONE)
    private String group = null;

    /**
     * Resource state
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

    @Override
    protected void finalize() {
      LOGGER.debug("Finalizing resource %s", this.hashCode());
    }

    /**
     * Our set of listeners (resources that are listening to changes in the
     * state of this resource)
     */
    @SecondaryKey(name = "listeners", relate = Relationship.MANY_TO_MANY, relatedEntity = Resource.class)
    Set<ResourceLocator> listeners = new TreeSet<>();

    /**
     * If the resource is currently locked
     */
    boolean locked;

    protected Resource() {
    }

    /**
     * Constructs a resource
     *
     * @param scheduler The scheduler
     * @param connector The connectorId to the resource
     * @param path The path to the resource
     * @param mode The locking mode
     */
    public Resource(Scheduler scheduler, Connector connector, String path, LockMode mode) {
        this.scheduler = scheduler;
        this.locator = new ResourceLocator(connector, path);
        this.lockmode = mode;
    }

    public Resource(Scheduler scheduler, ResourceLocator identifier, LockMode lockMode) {
        this.scheduler = scheduler;
        this.locator = identifier;
        this.lockmode = lockMode;
    }

    /**
     * Register a task that waits for our output
     */
    synchronized public void register(Job job) {
        // Copy the string to avoid holding the objects to notify in memory
        listeners.add(new ResourceLocator(job.locator));
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
        for (ResourceLocator id : listeners) {
            try {
                Resource resource = scheduler.getResource(id);
                resource.notify(this, objects);
            } catch(Exception e) {
                LOGGER.error(e, "Cannot notify %s [from %s]: %s", id, this.getIdentifier());
            }
        }
    }

    /**
     * Compares to another resource (based on the locator)
     */
    public int compareTo(Resource o) {
        return locator.compareTo(o.locator);
    }

    @Override
    public int hashCode() {
        return locator.hashCode();
    }

    @Override
    public String toString() {
        return locator.toString();
    }

    /**
     * Update the status of the resource by checking all that could have changed externally
     *
     * Calls {@linkplain #doUpdateStatus()}. If a change is detected, then it updates
     * the representation of the resource in the database by calling {@linkplain Scheduler#store(Resource)}.
     */
    final public boolean updateStatus() throws Exception {
        boolean changed = doUpdateStatus();
        if (changed)
            scheduler.store(this);
        return changed;
    }

    /**
     * Update the status of this resource
     */
    protected boolean doUpdateStatus() throws Exception {
        boolean updated = updateFromStatusFile();

        // Check if the resource was generated
        final FileObject doneFile = getMainConnector().resolveFile(locator.getPath() + DONE_EXTENSION);
        if (doneFile.exists()
                && this.getState() != ResourceState.DONE) {
            updated = true;
            // TODO: get the end timestamp reading the done file time stamp
//            doneFile.getContent().getLastModifiedTime();
            this.state = ResourceState.DONE;
        }

        // Check if the resource is locked
        boolean locked = getMainConnector().resolveFile(locator.getPath() + LOCK_EXTENSION).exists();
        updated |= locked != this.locked;
        this.locked = locked;

        return updated;
    }

    /**
     * Returns the connector associated to this resource
     * @return a Connector object
     */
    final public Connector getConnector() {
        return locator.getConnector();
    }

    /**
     * Returns the main connector associated with this resource
     * @return a SingleHostConnector object
     */
    public final SingleHostConnector getMainConnector() {
        return getConnector().getMainConnector();
    }


    /** Initialise a resource when retrieved from database */
    public void init(Scheduler scheduler) throws DatabaseException {
        this.scheduler = scheduler;
        this.locator.init(scheduler);
    }


    final static EnumSet<ResourceState> UPDATABLE_STATES
            = EnumSet.of(ResourceState.READY, ResourceState.ERROR, ResourceState.WAITING);

    /**
     * Replace this resource
     *
     * @param old
     * @return true if the resource was replaced, and false if an error occured
     */
    public boolean replace(Resource old) {
        synchronized (old) {
            assert old.locator.equals(locator) : "locators do not match";

            if (UPDATABLE_STATES.contains(old.state)) {
                // Transfer dependencies
                this.listeners = old.getListeners();
                updateDb();
                return true;
            }
            return false;
        }

    }

    /**
     * Notifies the resource that something happened
     * @param resource
     * @param objects
     */
    public void notify(Resource resource, Object... objects) {

    }

    public void setState(ResourceState state) {
        this.state = state;
    }


    /**
     * Sets the name
     * @param group
     */
    public void setGroup(String group) {
        this.group = group;
    }

    public String getGroup() {
        return this.group;
    }




    /**
     * Defines how a given lock type is satisfied by the current state
     * of the resource
     */
    static public enum DependencyStatus {
        /**
         * The resource can be used as is
         */
        OK,

        /**
         * The resource can be used when properly locked
         */
        OK_LOCK,

        /**
         * The resource is not ready yet
         */
        WAIT,

        /**
         * The resource is not ready yet, and is on hold (this can only be changed
         * by the external intervention)
         */
        HOLD,

        /**
         * The resource is not ready, and this is due to an error (possibly among
         * dependencies)
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
     * @see {@linkplain }
     * @return {@link DependencyStatus#OK} if the dependency is satisfied,
     *         {@link DependencyStatus#WAIT} if it can be satisfied and
     *         {@link DependencyStatus#ERROR} if it cannot be satisfied
     */
    DependencyStatus accept(LockType locktype) {
        LOGGER.debug("Checking lock %s for resource %s (state %s)",
                locktype, this, getState());

        // Handle simple cases
        if (state == ResourceState.ERROR)
            return DependencyStatus.ERROR;

        if (state == ResourceState.ON_HOLD)
            return DependencyStatus.HOLD;

        // If not done, then we wait
        if (state != ResourceState.DONE)
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
     * Tries to lock the resource depending on the type of dependency
     *
     * @param dependency
     * @throws UnlockableException
     */
    public Lock lock(String pid, LockType dependency) throws UnlockableException {
        // Check the dependency status
        switch (accept(dependency)) {
            // Cases where the lock cannot be granted (task waiting or in error)
            case WAIT:
                throw new UnlockableException(
                        "Cannot grant dependency %s for resource %s", dependency,
                        this);
            case ERROR:
                throw new RuntimeException(
                        format("Resource %s cannot accept dependency %s", this,
                                dependency));

            case OK_LOCK:
                // Case where we need a full lock
                return getMainConnector().createLockFile(locator + LOCK_EXTENSION);

            case OK:
                // Case where we just need a status lock
                return new StatusLock(this, pid, dependency == LockType.WRITE_ACCESS);

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
    transient TreeMap<String, Boolean> processMap = new TreeMap<String, Boolean>();

    public int getReaders() {
        return readers;
    }

    public int getWriters() {
        return writers;
    }


    /**
     * Get the task locator
     *
     * @return The task unique locator
     */
    public ResourceLocator getLocator() {
        return locator;
    }

    public String getIdentifier() {
        return locator.toString();
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
     * Returns the list of listeners
     */
    public Set<ResourceLocator> getListeners() {
        return listeners;
    }

    /**
     * Update the database after a change in state
     * @return true if everything went well
     */
    boolean updateDb() {
        try {
            scheduler.store(this);
            return true;
        } catch (DatabaseException e) {
            LOGGER.error(
                    "Could not update the information in the database for %s: %s",
                    this, e.getMessage());
        }
        return false;
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
        out.format("<div><b>Resource id</b>: %s</h2>", locator);
        out.format("<div><b>Status</b>: %s</div>", state);
    }


    /**
     * Update the status file
     * @param pidFrom     The old PID (or 0 if none)
     * @param pidTo       The new PID (or 0 if none)
     * @param writeAccess True if we need the write access
     * @throws UnlockableException
     */
    public void updateStatusFile(String pidFrom, String pidTo, boolean writeAccess)
            throws UnlockableException {
        // --- Lock the resource
        Lock fileLock = getMainConnector().createLockFile(locator.path + LOCK_EXTENSION);

        try {
            // --- Read the resource status
            updateFromStatusFile();

            final FileObject tmpFile = getMainConnector().resolveFile(locator.path + STATUS_EXTENSION + ".tmp");
            PrintWriter out = new PrintWriter(tmpFile.getContent().getOutputStream());
            if (pidFrom == null) {
                // We are adding a new entry
                out.format("%s %s%n", pidTo, writeAccess ? "w" : "r");
            } else {
                // We are modifying an entry: rewrite the file
                processMap.remove(pidFrom);
                if (pidTo != null)
                    processMap.put(pidTo, writeAccess);
                for (Map.Entry<String, Boolean> x : processMap.entrySet())
                    out.format("%s %s%n", x.getKey(), x.getValue() ? "w" : "r");
            }

            out.close();
            tmpFile.moveTo(getMainConnector().resolveFile(locator.path + STATUS_EXTENSION));
        } catch (Exception e) {
            throw new UnlockableException(
                    "Status file '%s' could not be created", locator.path + STATUS_EXTENSION);
        } finally {
            if (fileLock != null)
                fileLock.dispose();
        }

    }


    /**
     * Update the status of the resource using its status file
     *
     * @return A boolean specifying whether something was updated
     * @throws java.io.FileNotFoundException If some error occurs while reading status
     */
    public boolean updateFromStatusFile() throws Exception {
        boolean updated = writers > 0 || readers > 0;

        final FileObject statusFile = getMainConnector().resolveFile(locator.path + STATUS_EXTENSION);

        if (!statusFile.exists()) {
            writers = readers = 0;
            return updated;
        } else {
            // Check if we need to read the file
            long lastModified = statusFile.getContent().getLastModifiedTime();
            if (lastUpdate >= lastModified)
                return false;

            lastUpdate = lastModified;

            try {
                for (String line : new ReadLineIterator(statusFile.getContent().getInputStream())) {
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
                            processMap.put(fields[0], fields[1].equals("w"));
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
        return true;
    }
}
