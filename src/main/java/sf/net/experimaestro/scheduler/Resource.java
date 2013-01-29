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
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import com.sleepycat.persist.model.Relationship;
import com.sleepycat.persist.model.SecondaryKey;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import sf.net.experimaestro.connectors.Connector;
import sf.net.experimaestro.connectors.SingleHostConnector;
import sf.net.experimaestro.exceptions.ExperimaestroCannotOverwrite;
import sf.net.experimaestro.utils.log.Logger;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.TreeMap;

/**
 * The most general type of object manipulated by the server (can be a server, a
 * task, or a data)
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Entity
public abstract class Resource<Data extends ResourceData> implements /*not sure if ID or locator... Comparable<Resource>,*/ Cleaneable {
    final static private Logger LOGGER = Logger.getLogger();

    // --- Resource description

    /**
     * The resource ID
     */
    @PrimaryKey(sequence = "resourceId")
    private Long resourceID;


    /**
     * The resource state
     */
    @SecondaryKey(relate = Relationship.MANY_TO_ONE, name = STATE_KEY_NAME)
    ResourceState state;

    /**
     * Lock-related data
     */
    LockData lockData;


    // --- Values filled on demand

    /**
     * The dependencies for this job (dependencies are on any resource)
     * This array is filled when needed.
     */
    private transient Map<Long, Dependency> dependencies = null;

    /**
     * Resource data
     */
    private transient ResourceData data;


    // --- Values filled on init

    /**
     * Task manager
     */
    protected transient Scheduler scheduler;
    private String group;


    @Override
    protected void finalize() {
        LOGGER.debug("Finalizing resource [%s@%s]", System.identityHashCode(this), this);
    }


    /**
     * Called when deserializing from database
     */
    protected Resource() {
    }

    /**
     * Constructs a resource
     *
     * @param scheduler The scheduler
     * @param data      The associated resource data
     */
    public Resource(Scheduler scheduler, Data data) {
        this.scheduler = scheduler;
        this.data = data;
        this.dependencies = new TreeMap<>();
        LOGGER.debug("Constructor of resource [%s@%s]", System.identityHashCode(this), this);
    }


    /**
     * Compares to another resource (based on the locator)
     */
    public int compareTo(Resource o) {
        return Long.compare(resourceID, o.resourceID);
    }

    @Override
    public int hashCode() {
        return resourceID.hashCode();
    }

    @Override
    public String toString() {
        return String.format("Resource %x", resourceID);
    }

    /**
     * Get the ID
     */
    public long getId() {
        return resourceID;
    }

    /**
     * Update the state of the resource by checking all that could have changed externally
     * <p/>
     * Calls {@linkplain #doUpdateStatus()}.
     * If the update fails for some reason, then we just put the state into HOLD
     */
    final public boolean updateStatus() {
        try {
            return doUpdateStatus();
        } catch (Exception e) {
            LOGGER.error(e, "Exception while updating status");
            return set(ResourceState.ON_HOLD);
        }
    }


    /**
     * Do a full update of the state of this resource.
     * <p/>
     * This implies looking at the disk to check for done/lock files, etc.
     *
     * @return True if the state was updated
     */
    synchronized protected boolean doUpdateStatus() throws Exception {
        throw new NotImplementedException();
        // FIXME: some of the method should go down to Jobs, etc.
//        updateFromStatusFile(changes);
//
//        // Check if the resource was generated
//        final FileObject doneFile = getMainConnector().resolveFile(locator.getPath() + DONE_EXTENSION);
//        if (doneFile.exists() && state != ResourceState.DONE) {
//            changes.state = true;
//            if (this instanceof Job) {
//                ((Job) this).endTimestamp = doneFile.getContent().getLastModifiedTime();
//            }
//            this.state = ResourceState.DONE;
//        }
//
//        // Check if the resource is locked
//        boolean locked = getMainConnector().resolveFile(locator.getPath() + LOCK_EXTENSION).exists();
//        // TODO: should be a locking change
//        changes.state |= locked != this.locked;
//        this.locked = locked;

    }


    /**
     * Get the resource data
     */
    Data getData() {
        if (data != null) {
            assert scheduler != null;
            return (Data) scheduler.getResources().getData(resourceID).init(scheduler);
        }
        return (Data) data;
    }


    /**
     * Returns the connector associated to this resource
     *
     * @return a Connector object
     */
    final public Connector getConnector() {
        return getData().getLocator().getConnector();
    }

    /**
     * Returns the main connector associated with this resource
     *
     * @return a SingleHostConnector object
     */
    public final SingleHostConnector getMainConnector() {
        return getConnector().getMainConnector();
    }


    /**
     * Initialise a resource when retrieved from database. Does nothing if the resource
     * is already initialized
     *
     * @return <tt>true</tt> if the object was initialized, <tt>false</tt> if it was already
     */
    public boolean init(Scheduler scheduler) throws DatabaseException {
        if (this.scheduler != null)
            return false;

        this.scheduler = scheduler;
        return true;
    }


    /**
     * States in which a resource can replaced
     */
    final static EnumSet<ResourceState> UPDATABLE_STATES
            = EnumSet.of(ResourceState.READY, ResourceState.ON_HOLD, ResourceState.ERROR, ResourceState.WAITING);

    /**
     * Replace this resource
     *
     * @param old
     * @return true if the resource was replaced, and false if an error occured
     */
    public boolean replace(Resource old) throws ExperimaestroCannotOverwrite {
        synchronized (old) {
            assert old.getLocator().equals(getLocator()) : "locators do not match";
            if (UPDATABLE_STATES.contains(old.state)) {
                scheduler.store(this);
                return true;
            }
        }
        return false;
    }

    /**
     * Notifies the resource that something happened
     *
     * @param resource The original resource to which this message was targetted
     * @param message  The message
     */
    public void notify(Resource resource, Message message) {

    }


    /**
     * Sets the state
     *
     * @param state
     * @return <tt>true</tt> if state changed, <tt>false</tt> otherwise
     */
    public boolean set(ResourceState state) {
        if (this.state == state)
            return false;
        this.state = state;
        return true;
    }

    /**
     * The set of dependencies for this object
     */
    public Collection<Dependency> getDependencies() {
        return getDependencyMap().values();
    }

    /**
     * The set of dependencies for this object
     */
    protected Map<Long, Dependency> getDependencyMap() {
        if (dependencies == null)
            if (resourceID != 0)
                dependencies = scheduler.getDependencies(resourceID);
            else
                dependencies = new TreeMap<>();
        return dependencies;
    }



    public void addDependency(Dependency dependency) {
        if (resourceID != 0)
            throw new RuntimeException("Cannot add dependencies to a stored resource");

        getDependencyMap().put(dependency.getFrom(), dependency);
    }


    /**
     * @return the generated
     */
    public boolean isGenerated() {
        return getState() == ResourceState.DONE;
    }


    /**
     * Get the task from
     *
     * @return The task unique from
     */
    public ResourceLocator getLocator() {
        return getData().getLocator();
    }

    public String getIdentifier() {
        return getLocator().toString();
    }

    public LockData getLockData() {
        return lockData;
    }

    /**
     * Get the state of the resource
     *
     * @return The current state of the resource
     */
    final public ResourceState getState() {
        return state;
    }


    /**
     * Update the database after a change in state
     *
     * @return true if everything went well
     */
    boolean storeState() {
        try {
            scheduler.store(this);
            return true;
        } catch (DatabaseException | ExperimaestroCannotOverwrite e) {
            LOGGER.error(
                    "Could not update the information in the database for %s: %s",
                    this, e.getMessage());
        }
        return false;
    }


    public FileObject getFileWithExtension(String extension) throws FileSystemException {
        return getMainConnector().resolveFile(getLocator().path + extension);
    }

    public String getGroup() {
        return getData().getGroupId();
    }

    public void setGroup(String group) {
        getData().setGroupId(group);
    }

    /**
     * Creates a new dependency on this resource
     *
     * @param object The parameters for the dependency
     * @return a new dependency
     */
    public abstract Dependency createDependency(Object type);

    /**
     * Defines how printing should be done
     */
    static public class PrintConfig {
        public String detailURL;
    }

    /**
     * Writes an XML description of the resource
     *
     * @param out
     * @param config The configuration for printing
     */
    public void printXML(PrintWriter out, PrintConfig config) {
        out.format("<div><b>Resource id</b>: %s</h2>", getLocator());
        out.format("<div><b>Status</b>: %s</div>", getState());
    }


    @Override
    public void clean() {
    }

    /**
     * Provides an access to the reference of the resource
     */
    final static public class ResourcesIndex extends Resources {
        /**
         * Initialise the set of resources
         *
         * @param scheduler The scheduler
         * @param dbStore
         * @throws com.sleepycat.je.DatabaseException
         *
         */
        public ResourcesIndex(Scheduler scheduler, EntityStore dbStore) throws DatabaseException {
            super(scheduler, dbStore);
        }

        @Override
        protected boolean updateStatus(Resource resource) {
            return resource.updateStatus();
        }

        @Override
        protected Long getKey(Resource resource) {
            return resource.resourceID;
        }
    }


    /**
     * Extension for the lock file
     */
    public static final String LOCK_EXTENSION = ".lock";

    /**
     * Extension for the file that describes the state of the resource
     */
    public static final String STATUS_EXTENSION = ".state";

    /**
     * Extension used to mark a produced resource
     */
    public static final String DONE_EXTENSION = ".done";

    /**
     * Extension for the file containing the return code
     */
    public static final String CODE_EXTENSION = ".code";

    /**
     * Extension for the file containing the script to run
     */
    public static final String RUN_EXTENSION = ".run";

    /**
     * Extension for the standard output of a job
     */
    public static final String OUT_EXTENSION = ".out";

    /**
     * Extension for the standard error of a job
     */
    public static final String ERR_EXTENSION = ".err";

    /**
     * Extension for the standard input of a job
     */
    public static final String INPUT_EXTENSION = ".input";

    /**
     * Secondary key for "state"
     */
    public static final String STATE_KEY_NAME = "state";
}
