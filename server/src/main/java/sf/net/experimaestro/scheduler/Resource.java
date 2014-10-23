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

import org.json.simple.JSONObject;
import sf.net.experimaestro.exceptions.ExperimaestroCannotOverwrite;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.utils.FileNameTransformer;
import sf.net.experimaestro.utils.log.Logger;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.persistence.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileSystemException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;

/**
 * The most general type of object manipulated by the server (can be a server, a
 * task, or a data)
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Entity(name = "resources")
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.INTEGER)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(name = "resources")
public abstract class Resource
        implements /*not sure if ID or locator... Comparable<Resource>,*/ Listener {
    /**
     * Extension for the lock file
     */
    public static final FileNameTransformer LOCK_EXTENSION = new FileNameTransformer("", ".lock");

    // --- Resource description

    /**
     * Extension for the file that describes the state of the resource
     */
    public static final FileNameTransformer STATUS_EXTENSION = new FileNameTransformer("", ".state");

    /**
     * Extension used to mark a produced resource
     */
    public static final FileNameTransformer DONE_EXTENSION = new FileNameTransformer("", ".done");

    /**
     * Extension for the file containing the return code
     */
    public static final FileNameTransformer CODE_EXTENSION = new FileNameTransformer(".xpm.", ".code");


    // --- Values filled on demand

    /**
     * Extension for the file containing the script to run
     */
    public static final FileNameTransformer RUN_EXTENSION = new FileNameTransformer(".xpm.", ".run");

    /**
     * Extension for the standard output of a job
     */
    public static final FileNameTransformer OUT_EXTENSION = new FileNameTransformer("", ".out");


    // --- Values filled on doPostInit

    /**
     * Extension for the standard error of a job
     */
    public static final FileNameTransformer ERR_EXTENSION = new FileNameTransformer("", ".err");

    /**
     * Extension for the standard input of a job
     */
    public static final FileNameTransformer INPUT_EXTENSION = new FileNameTransformer(".xpm.input.", "");

    public static final String COMMAND_LINE_JOB_TYPE = "1";

    final static private Logger LOGGER = Logger.getLogger();

    /** The path with the connector */
    @Basic
    Path path;

    /**
     * The resource ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long resourceID;

    /**
     * Comparator on the database ID
     */
    static public final Comparator<Resource> ID_COMPARATOR = new Comparator<Resource>() {
        @Override
        public int compare(Resource o1, Resource o2) {
            return Long.compare(o1.resourceID, o2.resourceID);
        }
    };

    /**
     * The resource state
     */
    private ResourceState state;

    /**
     * Lock-related data - can be reconstructed
     */
    transient private LockData lockData;

    /**
     * The resource this resource is dependent upon
     */
    @ElementCollection
    @CollectionTable(name = "dependencies", joinColumns = @JoinColumn(name = "userId"))
    @Column(name = "dependency")
    @MapKeyJoinColumn(name = "messageId")
    Map<Resource, Dependency> dependencies = null;

    /**
     * Called when deserializing from database
     */
    protected Resource() {
        LOGGER.debug("Constructor of resource [%s@%s]", System.identityHashCode(this), this);
    }

    public Resource(Path path) {
        this.path = path;
    }

    @Override
    protected void finalize() {
        LOGGER.debug("Finalizing resource [%s@%s]", System.identityHashCode(this), this);
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
        if (resourceID == null)
            return "R-";
        return String.format("R%d", resourceID);
    }

    /**
     * Get the ID
     */
    public long getId() {
        if (resourceID == null)
            return 0;
        return resourceID;
    }

    /**
     * Update the state of the resource by checking all that could have changed externally
     * <p/>
     * Calls {@linkplain #doUpdateStatus(boolean)}.
     * If the update fails for some reason, then we just put the state into HOLD
     */
    final protected boolean updateStatus(boolean store) {
        try {
            boolean b = doUpdateStatus(store);
            return b;
        } catch (Exception e) {
            LOGGER.error(e, "Exception while updating status");
            return setState(ResourceState.ON_HOLD);
        }
    }

    /**
     * Do a full update of the state of this resource.
     * <p/>
     * This implies looking at the disk to check for done/lock files, etc.
     *
     * @param store <tt>true</tt> if changes should be stored in DB
     * @return True if the state was updated
     */
    synchronized protected boolean doUpdateStatus(boolean store) throws Exception {
        return false;
    }

    public boolean stored() {
        return resourceID != null;
    }

    /**
     * Replace this resource
     *
     * @param old
     * @return true if the resource was replaced, and false if an error occured
     */
    public boolean replace(Resource old) throws ExperimaestroCannotOverwrite {
        assert old.getPath().equals(getPath()) : String.format("locators %s and %s do not match", old, this);

        if (!old.canBeReplaced()) return false;
        LOGGER.info("Old [%s] is in state %s", old.getPath(), old.state);

        synchronized (old) {
            if (ResourceState.UPDATABLE_STATES.contains(old.state)) {
                Scheduler.get().store(this, false);
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether this resource can be replaced
     *
     * @return
     */
    public boolean canBeReplaced() {
        return ResourceState.UPDATABLE_STATES.contains(state);
    }

    /**
     * Notifies the resource that something happened
     *
     * @param message The message
     */
    @Override
    public void notify(Message message) {
        switch (message.getType()) {
            case RESOURCE_REMOVED:
                resourceID = null;
                break;
        }
    }

    /**
     * The set of dependencies for this object
     */
    public Collection<Dependency> getDependencies() {
        return dependencies.values();
    }

    /**
     * Update a dependency if we have a cache of it
     *
     * @param dependency The dependency
     */
    protected Dependency updateDependency(Dependency dependency) {
        if (dependencies != null)
            return dependencies.put(dependency.getFrom(), dependency);
        return null;
    }

    public void addDependency(Dependency dependency) {
        throw new RuntimeException("Cannot add dependency to resource of type " + this.getClass());
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
    public Path getPath() {
        return path; }

    public String getIdentifier() {
        return getPath().toString();
    }

    public LockData getLockData() {
        return lockData;
    }

    public void setLockData(LockData lockData) {
        this.lockData = lockData;
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
     * Sets the state
     *
     * @param state
     * @return <tt>true</tt> if state changed, <tt>false</tt> otherwise
     */
    public boolean setState(ResourceState state) {
        if (this.state == state)
            return false;
        this.state = state;
        if (this.stored()) {
            Scheduler.get().notify(new SimpleMessage(Message.Type.STATE_CHANGED, this));
        }
        return true;
    }

    /**
     * Update the database after a change in state
     *
     * @param notify
     * @return true if everything went well
     */
    boolean storeState(boolean notify) {
        try {
            Scheduler.get().store(this, notify);
            return true;
        } catch (ExperimaestroCannotOverwrite e) {
            LOGGER.error(
                    "Could not update the information in the database for %s: %s",
                    this, e.getMessage());
        }
        return false;
    }

    public Path getFileWithExtension(String extension) throws FileSystemException {
        return new FileNameTransformer("", extension).transform(path);
    }

    /**
     * Creates a new dependency on this resource
     *
     * @param type The parameters for the dependency
     * @return a new dependency
     */
    public abstract Dependency createDependency(Object type);

    /**
     * Returns whether this resource can be overriden
     *
     * @param current
     * @return
     */
    public boolean canBeOverriden(Resource current) {
        if (state == ResourceState.RUNNING && current != this) {
            LOGGER.error(String.format("Cannot override a running task [%s] / %s vs %s", current,
                    System.identityHashCode(current), System.identityHashCode(this.hashCode())));
            return false;
        }
        return true;
    }

    /**
     * Returns the main output file for this resource
     */
    public Path outputFile() throws FileSystemException {
        throw new IllegalAccessError("No output file for resources of type " + this.getClass());
    }

    /**
     * Writes an XML description of the resource
     *
     * @param out
     * @param config The configuration for printing
     * @deprecated Use {@linkplain #toJSON()}
     */
    @Deprecated
    public void printXML(PrintWriter out, PrintConfig config) {
        out.format("<div><b>Resource id</b>: %s</h2>", getPath());
        out.format("<div><b>Status</b>: %s</div>", getState());
    }

    /**
     * Get a JSON reprensentation of the object
     *
     * @return
     * @throws IOException
     */
    public JSONObject toJSON() throws IOException {
        JSONObject object = new JSONObject();
        object.put("id", getId());
        object.put("status", getState().toString());
        return object;
    }

    @PostRemove
    public void clean() {
    }

    /**
     * Retrieves resources that depend upon the given resource
     *
     * @return A map of dependencies
     */
    public Collection<Dependency> retrieveDependentResources() {
        return dependencies.values();
    }

    /**
     * Notify the dependencies that a resource has changed
     */
    public void notifyDependencies() {
        // Join between active states
        // TODO: should limit to the dependencies of some resources
        final long from = getId();

        // Notify dependencies in turn
        Collection<Dependency> dependencies = retrieveDependentResources();
        LOGGER.info("Notifying dependencies from R%s [%d]", from, dependencies.size());

        EntityTransaction transaction = Scheduler.transaction();
        try {
            for (Dependency dep : dependencies) {
                if (dep.status == DependencyStatus.UNACTIVE) {
                    LOGGER.debug("We won't notify [R%s] to [R%s] since the dependency is unactive", from, dep.getTo());

                } else
                    try {
                        // when the dependency status is null, the dependency is not active anymore
                        LOGGER.debug("Notifying dependency: [R%s] to [R%s]; current dep. state=%s", from, dep.getTo(), dep.status);
                        // Preserves the previous state
                        DependencyStatus beforeState = dep.status;

                        if (dep.update(false)) {
                            final Resource depResource = dep.getTo();
                            if (!ResourceState.NOTIFIABLE_STATE.contains(depResource.getState())) {
                                LOGGER.debug("We won't notify resource %s since its state is %s", depResource, depResource.getState());
                                continue;
                            }

                            // We ensure nobody else can modify the resource first
                            synchronized (depResource) {
                                // Update the dependency in database
                                Scheduler.get().store(dep);

                                // Notify the resource that a dependency has changed
                                depResource.notify(new DependencyChangedMessage(dep, beforeState, dep.status));
                                LOGGER.debug("After notification [%s -> %s], state is %s for [%s]",
                                        beforeState, dep.status, depResource.getState(), depResource);
                            }
                        } else {
                            LOGGER.debug("No change in dependency status [%s -> %s]", beforeState, dep.status);
                        }
                    } catch (RuntimeException e) {
                        LOGGER.error(e, "Got an exception while notifying [%s]", this);
                    }
            }
            transaction.commit();
        } finally {
        }

    }


    /**
     * Store a resource
     *
     * @param resource
     * @return The old resource, or null if there was nothing
     * @throws ExperimaestroCannotOverwrite If the old resource could not be overriden
     */
    synchronized public Resource put(Resource resource) throws ExperimaestroCannotOverwrite {
        // Get the group
        final boolean newResource = !resource.stored();
        if (newResource) {
            resource.updateStatus(false);
        }

        // TODO implement
        final Resource old = null;
        if (old == null) {
            throw new NotImplementedException();
        }

        LOGGER.debug("Storing resource %s [%x@%s] in state %s", resource, System.identityHashCode(resource), resource.getId(), resource.getState());

        if (newResource) {
            final long id = resource.getId();
            LOGGER.debug("Adding a new resource [%s] in database [id=%d/%x]", resource, id, System.identityHashCode(resource));

            // Add the dependencies
            final Collection<Dependency> deps = resource.getDependencies();
            for (Dependency dependency : deps) {
                // FIXME
                dependency.setTo(this);
                Scheduler.get().store(dependency);
                LOGGER.debug("Added new dependency %s [%d]", dependency, dependency.getDatabaseId());
            }

            // Notify
            Scheduler.get().notify(new SimpleMessage(Message.Type.RESOURCE_ADDED, resource));
        }


        return old;
    }

    public Collection<Dependency> getDependentResources() {
        return null;
    }

    public boolean updateStatus() {
        return false;
    }

    /**
     * Delete a resource
     *
     * @param recursive Delete dependent resources
     */
    public synchronized void delete(boolean recursive) {
        if (getState() == ResourceState.RUNNING)
            throw new XPMRuntimeException("Cannot delete the running task [%s]", this);
        Collection<Dependency> dependencies = retrieveDependentResources();
        if (!dependencies.isEmpty()) {
            if (recursive) {
                for (Dependency dependency : dependencies) {
                    Resource dep = dependency.getTo();
                    if (dep != null) {
                        dep.delete(true);
                    }
                }
            } else
                throw new XPMRuntimeException("Cannot delete the resource %s: it has dependencies", this);
        }

        Scheduler.get().remove(this);
        SimpleMessage message = new SimpleMessage(Message.Type.RESOURCE_REMOVED, this);
        this.notify(message);
        notify(message);
    }

    public Path getFileWithExtension(FileNameTransformer extension) throws FileSystemException {
        return extension.transform(path);
    }

    /**
     * Defines how printing should be done
     */
    static public class PrintConfig {
        public String detailURL;
    }

}
