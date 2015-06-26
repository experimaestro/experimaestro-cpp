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
import org.json.simple.JSONObject;
import sf.net.experimaestro.connectors.Connector;
import sf.net.experimaestro.connectors.SingleHostConnector;
import sf.net.experimaestro.exceptions.ExperimaestroCannotOverwrite;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.manager.scripting.Expose;
import sf.net.experimaestro.manager.scripting.Exposed;
import sf.net.experimaestro.utils.CloseableIterable;
import sf.net.experimaestro.utils.FileNameTransformer;
import sf.net.experimaestro.utils.GsonSerialization;
import sf.net.experimaestro.utils.JsonSerializationInputStream;
import sf.net.experimaestro.utils.db.SQLInsert;
import sf.net.experimaestro.utils.log.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.FileSystemException;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
    static ConstructorRegistry<Resource> REGISTRY = new ConstructorRegistry(new Class[]{Long.class, String.class})
            .add(Resource.class, CommandLineTask.class, TokenResource.class);

    public static final String SELECT_BEGIN = "SELECT id, type, path, status FROM resources";

    public static final String INSERT_DEPENDENCY = "INSERT INTO Dependencies(fromId, toId, status) VALUES(?,?,?)";

    SQLInsert SQL_INSERT = new SQLInsert("Resources", true, "id", "type", "path", "status", "data");


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
    @GsonSerialization(serialize = false)
    private Long resourceID;

    /**
     * The connector
     * <p>
     * A null value is used for LocalhostConnector
     */
    transient private Connector connector;

    /**
     * The outgoing dependencies (resources that depend on this)
     */
    transient private Map<Resource, Dependency> outgoingDependencies;

    /**
     * The ingoing dependencies (resources that we depend upon)
     */
    transient private Map<Resource, Dependency> ingoingDependencies;

    /**
     * The resource state
     */
    transient private ResourceState state = ResourceState.ON_HOLD;

    /**
     * Cached Path
     */
    transient private Path path;

    /**
     * Flag that says whether the data has been loaded
     */
    transient private boolean dataLoaded;

    public Resource(Connector connector, Path path) throws IOException {
        this(connector, path.toString());
    }

    public Resource(Connector connector, String name) {
        this.connector = connector;
        this.locator = name;
        outgoingDependencies = new HashMap<>();
        ingoingDependencies = new HashMap<>();
        dataLoaded = true;
    }

    /**
     * Construct from DB
     */
    public Resource(Long id, String locator) {
        this(Scheduler.get().getLocalhostConnector(), locator);
        this.resourceID = id;
        this.locator = locator;
        dataLoaded = false;
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
    synchronized final public boolean updateStatus() throws SQLException {
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
    public void notify(Message message) throws SQLException {
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
    synchronized public boolean setState(ResourceState state) throws SQLException {
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
                    throw new SQLException(format("Updating resource resulted in %d updated rows", count));
                }
            } catch (SQLException e) {
                throw new SQLException(e);
            }
            LOGGER.debug("Stored new state [%s] of job %s in database", state, this);
        }
        this.state = state;

        if (inDatabase()) {
            if (state == ResourceState.READY) {
                LOGGER.debug("Notifying runners");
                Scheduler.notifyRunners();
            }
            LOGGER.debug("Notifying dependencies of %s [new state %s]", this, state);
            Scheduler.get().addChangedResource(this);
        }

        Scheduler.get().notify(new SimpleMessage(Message.Type.STATE_CHANGED, this));

        return true;
    }

    public boolean inDatabase() {
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

        final Dependency put = this.ingoingDependencies.put(dependency.getFrom(), dependency);
        assert put == null;
        return put;
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
    synchronized public Resource put(Resource resource) throws ExperimaestroCannotOverwrite, SQLException {
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
    public synchronized void delete(boolean recursive) throws SQLException {
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
                throw new XPMRuntimeException("Cannot delete the resource %s: it has dependencies [%s]", this,
                        dependencies);
        }

        // Remove
        Scheduler.get().resources().delete(this);

        // Remove dependencies
        for (Dependency dependency : getIngoingDependencies().values()) {
            final Resource from = dependency.getFrom();
            if (from.outgoingDependencies != null) {
                from.outgoingDependencies.remove(this);
            }
        }

        Scheduler.get().notify(new SimpleMessage(Message.Type.RESOURCE_REMOVED, this));
    }

    public Path getFileWithExtension(FileNameTransformer extension) throws IOException {
        return extension.transform(getConnector().resolve(locator));
    }

    synchronized final public void replaceBy(Resource resource) throws ExperimaestroCannotOverwrite, SQLException {
        if (!resource.getLocator().equals(this.locator)) {
            throw new ExperimaestroCannotOverwrite("Path %s and %s differ", resource.getLocator(), this.getLocator());
        }

        resource.save(Scheduler.get().resources(), this);

        // Not in DB anymore
        this.setId(null);
    }

    public Connector getConnector() {
        return connector;
    }

    /**
     * Returns the main connector associated with this resource
     *
     * @return a SingleHostConnector object
     */
    public final SingleHostConnector getMainConnector() {
        return getConnector().getMainConnector();
    }

    public void save() throws SQLException {
        save(Scheduler.get().resources(), null);
    }

    protected void save(DatabaseObjects<Resource> resources, Resource old) throws SQLException {
        boolean update = old != null;
        if (update && getId() == null) {
            throw new SQLException("Resource not in database");
        } else if (!update && getId() != null) {
            throw new SQLException("Resource already in database");
        }

        // Update the status
        updateStatus();

        // Save resource
        // Save on file
        final long typeValue = DatabaseObjects.getTypeValue(getClass());
        LOGGER.debug("Saving resource [%s] of type %s [%d], status %s [%s]",
                getLocator(), getClass(), typeValue,
                getState(), getState().value());

        try (final JsonSerializationInputStream jsonInputStream = JsonSerializationInputStream.of(this)) {
            resources.save(this, SQL_INSERT, update, typeValue, getLocator(), getState().value(), jsonInputStream);
        } catch (IOException e) {
            throw new SQLException(e);
        }

        if (update) {
            // Remove dependencies that are not anymore
            final Iterator<Map.Entry<Resource, Dependency>> iterator = ingoingDependencies.entrySet().iterator();
            while (iterator.hasNext()) {
                final Map.Entry<Resource, Dependency> entry = iterator.next();
                final Dependency dependency = old.ingoingDependencies.remove(entry.getKey());
                if (dependency != null) {
                    entry.getValue().replaceBy(dependency);
                } else {
                }
            }

            // Delete old
            try (PreparedStatement st = resources.connection.prepareStatement("DELETE FROM Dependencies WHERE fromId=? and toId=?")) {
                for (Dependency dependency : old.ingoingDependencies.values()) {
                    if (!ingoingDependencies.containsKey(dependency.getFrom())) {
                        st.setLong(1, dependency.getFrom().getId());
                        st.setLong(2, getId());
                        st.execute();
                    }
                }
            }

        }

        // Save dependencies (if not already in DB)
        try (PreparedStatement st = resources.connection.prepareStatement(INSERT_DEPENDENCY)) {
            st.setLong(2, getId());
            for (Dependency dependency : getIngoingDependencies().values()) {
                if (!update || !old.ingoingDependencies.containsKey(dependency.getFrom())) {
                    st.setLong(1, dependency.getFrom().getId());
                    st.setLong(3, dependency.status.getId());
                    st.execute();
                }
            }
        }


        LOGGER.debug("Resource %s saved/updated", this);
        Scheduler.get().notify(new SimpleMessage(!update ? Message.Type.RESOURCE_ADDED : Message.Type.STATE_CHANGED, this));
    }

    /**
     * Get a resource by locator
     *
     * @param path The path of the resource
     * @return The resource or null if there is no such resource
     */
    static public Resource getByLocator(String path) throws SQLException {
        return Scheduler.get().resources().findUnique(SELECT_BEGIN + " WHERE path=?", st -> st.setString(1, path));
    }

    static protected Resource create(DatabaseObjects<Resource> db, ResultSet result) {
        try {
            long id = result.getLong(1);
            long type = result.getLong(2);
            String path = result.getString(3);
            long status = result.getLong(4);

            final Constructor<? extends Resource> constructor = REGISTRY.get(type);
            final Resource resource = constructor.newInstance(id, path);

            // Set stored values
            resource.setState(ResourceState.fromValue(status));

            return resource;
        } catch (InstantiationException | SQLException | InvocationTargetException | IllegalAccessException e) {
            throw new XPMRuntimeException("Error retrieving database object", e);
        }
    }

    /**
     * Iterator on resources
     */
    static public CloseableIterable<Resource> resources() throws SQLException {
        final DatabaseObjects<Resource> resources = Scheduler.get().resources();
        return resources.find(SELECT_BEGIN, st -> {
        });
    }

    public static CloseableIterable<Resource> find(EnumSet<ResourceState> states) throws SQLException {
        final DatabaseObjects<Resource> resources = Scheduler.get().resources();
        StringBuilder sb = new StringBuilder();
        sb.append(SELECT_BEGIN);
        sb.append(" WHERE status in (");
        boolean first = true;
        for (ResourceState state : states) {
            if (first) {
                first = false;
            } else {
                sb.append(',');
            }
            sb.append(state.value());
        }
        sb.append(")");

        final String query = sb.toString();
        LOGGER.debug("Searching for resources in states %s: %s", states, query);
        return resources.find(query, st -> {
        });
    }

    static public Resource getById(long resourceId) throws SQLException {
        final DatabaseObjects<Resource> resources = Scheduler.get().resources();
        final Resource r = resources.getFromCache(resourceId);
        if (r != null) {
            return r;
        }

        return resources.findUnique(SELECT_BEGIN + " WHERE id=?", st -> st.setLong(1, resourceId));
    }


    /**
     * Load data from database
     */
    protected void loadData() {
        if (dataLoaded) {
            return;
        }

        Scheduler.get().resources().loadData(this, "data");
        dataLoaded = true;
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
