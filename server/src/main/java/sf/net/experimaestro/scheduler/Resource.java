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

import org.apache.commons.lang.NotImplementedException;
import org.hsqldb.Database;
import org.json.simple.JSONObject;
import sf.net.experimaestro.connectors.Connector;
import sf.net.experimaestro.connectors.LocalhostConnector;
import sf.net.experimaestro.connectors.SingleHostConnector;
import sf.net.experimaestro.exceptions.DatabaseException;
import sf.net.experimaestro.exceptions.ExperimaestroCannotOverwrite;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.manager.scripting.Expose;
import sf.net.experimaestro.manager.scripting.Exposed;
import sf.net.experimaestro.utils.FileNameTransformer;
import sf.net.experimaestro.utils.log.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileSystemException;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

import static java.lang.String.format;

/**
 * The most general type of object manipulated by the server (can be a server, a
 * task, or a data)
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Exposed
@TypeIdentifier("RESOURCE")
public class Resource implements Identifiable {
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
     * Extension used status mark a produced resource
     */
    public static final FileNameTransformer DONE_EXTENSION = new FileNameTransformer("", ".done");

    /**
     * Extension for the file containing the return code
     */
    public static final FileNameTransformer CODE_EXTENSION = new FileNameTransformer("", ".code");


    // --- Values filled on demand

    /**
     * Extension for the file containing the script status run
     */
    public static final FileNameTransformer RUN_EXTENSION = new FileNameTransformer("", ".xpm.run");

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
    public static final FileNameTransformer INPUT_EXTENSION = new FileNameTransformer("", ".xpm.input.");


    final static private Logger LOGGER = Logger.getLogger();

    /**
     * The path with the connector
     */
    protected String locator;

    /**
     * The resource ID
     */
    private Long resourceID;

    /**
     * The connector
     * <p>
     * A null value is used for LocalhostConnector
     */
    private Connector connector;

    /**
     * The outgoing dependencies (resources that depend on this)
     */
    private Map<Resource, Dependency> outgoingDependencies;

    /**
     * The ingoing dependencies (resources that we depend upon)
     */
    private Map<Resource, Dependency> ingoingDependencies;

    /**
     * The resource state
     */
    private ResourceState state = ResourceState.ON_HOLD;

    /**
     * Cached Path
     */
    transient private Path path;

    public Resource(Connector connector, Path path) throws IOException {
        this(connector, path.toString());
    }

    public Resource(Connector connector, String name) {
        this.connector = connector instanceof LocalhostConnector ? null : connector;
        this.locator = name;
        outgoingDependencies = new HashMap<>();
        ingoingDependencies = new HashMap<>();
    }

    /** Construct from DB */
    public Resource(Long id, String locator) {
        this(LocalhostConnector.getInstance(), locator);
        this.resourceID = id;
        this.locator = locator;
    }

    /**
     * Get a resource by locator
     *
     * @param path The path of the resource
     * @return The resource or null if there is no such resource
     */
    public static Resource getByLocator(String path) throws DatabaseException {
        return Scheduler.get().resources().getByLocator(path);
    }

    @Override
    protected void finalize() {
        LOGGER.debug("Finalizing resource [%s@%s]", System.identityHashCode(this), this);
    }

    @Override
    public int hashCode() {
        return getLocator().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Resource && locator.equals(((Resource) obj).getLocator());
    }

    @Override
    @Expose
    public String toString() {
        if (resourceID == null)
            return "R-";

        return format("R%d", resourceID);
    }

    /**
     * Returns a detailed description
     */
    public String toDetailedString() {
        return toString();
    }

    /**
     * Get the ID
     */
    @Override
    public Long getId() {
        return resourceID;
    }

    public void setId(Long id) {
        this.resourceID = id;
    }

    /**
     * Update the state of the resource by checking all that could have changed externally
     * <p>
     * Calls {@linkplain #doUpdateStatus()}.
     * If the update fails for some reason, then we just put the state into HOLD
     */
    final public boolean updateStatus() throws DatabaseException {
        try {
            boolean b = doUpdateStatus();
            return b;
        } catch (Exception e) {
            LOGGER.error(e, "Exception while updating status");
            return setState(ResourceState.ON_HOLD);
        }
    }

    /**
     * Do a full update of the state of this resource.
     * <p>
     * This implies looking at the disk status check for done/lock files, etc.
     *
     * @return True if the state was updated
     */
    protected boolean doUpdateStatus() throws Exception {
        return false;
    }

    public boolean isStored() {
        return resourceID != null;
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
    public void notify(Message message) throws DatabaseException {
        switch (message.getType()) {
            case RESOURCE_REMOVED:
                break;
        }
    }

    /**
     * The set of resources the resource is dependent upon
     */
    public Collection<Dependency> getDependencies() {
        return ingoingDependencies.values();
    }

    /**
     * The set of dependencies that are dependent on this resource
     */
    public Collection<Dependency> getOutgoingDependencies() {
        return outgoingDependencies.values();
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
    public String getLocator() {
        return locator;
    }

    /**
     * Get a filesystem path
     */
    public Path getPath() {
        if (path != null) {
            return path;
        }
        return path = Connector.create(this.locator);
    }

    /**
     * Get the identifier of this task
     *
     * @return Return a stringified version of the path
     */
    public String getIdentifier() {
        return getLocator().toString();
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
    public boolean setState(ResourceState state) throws DatabaseException {
        if (this.state == state)
            return false;

        // Update in DB
        if (inDatabase()) {
            try (final PreparedStatement st = Scheduler.get().getConnection().prepareStatement("UPDATE Resources SET status=? WHERE id=?")) {
                st.setLong(1, state.value());
                st.setLong(2, getId());
                st.execute();
                final int count = st.getUpdateCount();

                if (count != 1) {
                    throw new DatabaseException("Updating resource resulted in %d updated rows", count);
                }
            } catch (SQLException e) {
                throw new DatabaseException(e);
            }
            LOGGER.debug("Stored new state [%s] of job %s in database", state, this);
        }
        this.state = state;

        if (inDatabase()) {
            if (state == ResourceState.READY) {
                LOGGER.debug("Notifying runners");
                Scheduler.notifyRunners();
            }
            Scheduler.get().addChangedResource(this);
        }

        Scheduler.get().notify(new SimpleMessage(Message.Type.STATE_CHANGED, this));

        return true;
    }

    private boolean inDatabase() {
        return resourceID != null;
    }

    /**
     * Creates a new dependency on this resource
     *
     * @param type The parameters for the dependency
     * @return a new dependency or null if this object does not need to be locked
     */
    public Dependency createDependency(Object type) {
        return null;
    }

    /**
     * Returns the main output file for this resource
     */
    public Path outputFile() throws IOException {
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
        out.format("<div><b>Resource id</b>: %s</h2>", getLocator());
        out.format("<div><b>Status</b>: %s</div>", getState());
    }

    /**
     * Get a JSON representation of the object
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

    public void clean() {
    }

    synchronized protected Dependency addIngoingDependency(Dependency dependency) {
        dependency.to = this;
        dependency.from.addOutgoingDependency(dependency);
        return this.ingoingDependencies.put(dependency.getFrom(), dependency);
    }

    synchronized protected void addOutgoingDependency(Dependency dependency) {
        LOGGER.debug("Adding dependency from %s to %s [outgoing]", this, dependency.getTo());
        outgoingDependencies.put(dependency.getTo(), dependency);
    }

    /**
     * Store a resource
     *
     * @param resource
     * @return The old resource, or null if there was nothing
     * @throws ExperimaestroCannotOverwrite If the old resource could not be overriden
     */
    synchronized public Resource put(Resource resource) throws ExperimaestroCannotOverwrite, DatabaseException {
        // Get the group
        final boolean newResource = !resource.isStored();
        if (newResource) {
            resource.updateStatus();
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

            // Notify
            Scheduler.get().notify(new SimpleMessage(Message.Type.RESOURCE_ADDED, resource));
        }


        return old;
    }

    /**
     * Delete a resource
     *
     * @param recursive Delete dependent resources
     */
    public synchronized void delete(boolean recursive) throws DatabaseException {
        if (getState() == ResourceState.RUNNING) {
            throw new XPMRuntimeException("Cannot delete the running task [%s]", this);
        }

        Collection<Dependency> dependencies = getOutgoingDependencies();
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

        // Remove
        Scheduler.get().resources().delete(this);
        Scheduler.get().notify(new SimpleMessage(Message.Type.RESOURCE_REMOVED, this));
    }

    public Path getFileWithExtension(FileNameTransformer extension) throws IOException {
        return extension.transform(getConnector().resolve(locator));
    }

    final public void replaceBy(Resource resource) throws ExperimaestroCannotOverwrite {
        if (resource.getClass() != this.getClass()) {
            throw new ExperimaestroCannotOverwrite("Class %s and %s differ", resource.getClass(), this.getClass());
        }

        if (!resource.getLocator().equals(this.locator)) {
            throw new ExperimaestroCannotOverwrite("Path %s and %s differ", resource.getLocator(), this.getLocator());
        }

        doReplaceBy(resource);
    }

    protected void doReplaceBy(Resource resource) {
        // Remove dependencies that are not anymore
        final Iterator<Map.Entry<Resource, Dependency>> iterator = ingoingDependencies.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<Resource, Dependency> entry = iterator.next();
            final Dependency dependency = resource.ingoingDependencies.remove(entry.getKey());
            if (dependency != null) {
                entry.getValue().replaceBy(dependency);
            } else {
                iterator.remove();
            }
        }

        // Add new dependencies
        resource.ingoingDependencies.entrySet().stream()
                .forEach(entry -> {
                    entry.getValue().to = this;
                    this.ingoingDependencies.put(entry.getKey(), entry.getValue());
                });

        this.state = resource.state;
    }

    public Connector getConnector() {
        return connector == null ? LocalhostConnector.getInstance() : connector;
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
     * Save the entity in DB
     */
    public void save() throws DatabaseException {
        // Update the status
        updateStatus();

        // Save in DB
        Scheduler.get().resources().save(this);

        LOGGER.debug("Resource %s stored", this);
        Scheduler.get().notify(new SimpleMessage(Message.Type.RESOURCE_ADDED, this));
    }


    @Expose("output")
    public Path output() throws IOException {
        return outputFile();
    }

    @Expose
    public Path file() throws FileSystemException {
        return getPath();
    }

    @Expose
    public Path resolve(String path) throws FileSystemException {
        return getPath().getParent().resolve(path);
    }

    @Expose
    public Dependency lock(String lockType) {
        return createDependency(lockType);
    }

    public static Resource getById(long resourceId) throws DatabaseException {
        return Scheduler.get().resources().getById(resourceId);
    }

    public Map<Resource, Dependency> getIngoingDependencies() {
        return ingoingDependencies;
    }

    /**
     * Defines how printing should be done
     */
    static public class PrintConfig {
        public String detailURL;
    }

}
