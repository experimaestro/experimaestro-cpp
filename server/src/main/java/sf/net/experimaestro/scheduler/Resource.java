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
import sf.net.experimaestro.connectors.LocalhostConnector;
import sf.net.experimaestro.connectors.SingleHostConnector;
import sf.net.experimaestro.exceptions.ExperimaestroCannotOverwrite;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.manager.scripting.Expose;
import sf.net.experimaestro.manager.scripting.Exposed;
import sf.net.experimaestro.utils.FileNameTransformer;
import sf.net.experimaestro.utils.log.Logger;

import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileSystemException;
import java.nio.file.Path;
import java.util.*;

import static java.lang.String.format;

/**
 * The most general type of object manipulated by the server (can be a server, a
 * task, or a data)
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@SuppressWarnings("JpaAttributeTypeInspection")
@Entity(name = "resources")
@DiscriminatorColumn(name = "resourceType", discriminatorType = DiscriminatorType.INTEGER)
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Table(name = "resources", indexes = @Index(columnList = "locator"))
@Exposed
public class Resource {
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

    public static final String JOB_TYPE = "1";
    public static final String TOKEN_RESOURCE_TYPE = "2";

    final static private Logger LOGGER = Logger.getLogger();
    static private SharedLongLocks resourceLocks = new SharedLongLocks();
    /**
     * Version for optimistic locks
     */
    @Version
    @Column(name = "version")
    protected long version;

    /**
     * The path with the connector
     */
    @Column(name = "locator", updatable = false)
    protected String locator;
    /**
     * Keeps the state of the resource before saving
     */
    protected transient ResourceState oldState;
    transient boolean prepared = false;
    /**
     * The resource ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE)
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
     * The connector
     * <p>
     * A null value is used for LocalhostConnector
     */
    @JoinColumn(name = "connector", updatable = false)
    @ManyToOne(cascade = CascadeType.PERSIST)
    private Connector connector;
    /**
     * The outgoing dependencies (resources that depend on this)
     */
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "from")
    @MapKey(name = "to")
    private Map<Resource, Dependency> outgoingDependencies = new HashMap<>();
    /**
     * The ingoing dependencies (resources that we depend upon)
     */
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.REFRESH}, fetch = FetchType.LAZY, mappedBy = "to")
    @MapKey(name = "from")
    private Map<Resource, Dependency> ingoingDependencies = new HashMap<>();

    /**
     * The resource state
     */
    @Column(name = "state")
    private ResourceState state = ResourceState.ON_HOLD;
    /**
     * Cached Path
     */
    transient private Path path;

    /**
     * Called when deserializing from database
     */
    protected Resource() {
        LOGGER.trace("Constructor of resource [%s@%s]", System.identityHashCode(this), this);
    }

    public Resource(Connector connector, Path path) throws IOException {
        this(connector, path.toString());
    }

    public Resource(Connector connector, String name) {
        this.connector = connector instanceof LocalhostConnector ? null : connector;
        this.locator = name;
    }

    /**
     * Get a resource by locator
     *
     * @param em   The current entity manager
     * @param path The path of the resource
     * @return The resource or null if there is no such resource
     */
    public static Resource getByLocator(EntityManager em, String path) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Resource> cq = cb.createQuery(Resource.class);
        Root<Resource> root = cq.from(Resource.class);
        cq.where(root.get("locator").in(cb.parameter(String.class, "locator")));


        TypedQuery<Resource> query = em.createQuery(cq);
        query.setParameter("locator", path);
        List<Resource> result = query.getResultList();
        assert result.size() <= 1;

        if (result.isEmpty())
            return null;

        return result.get(0);
    }

    /**
     * Lock a resource by ID
     *
     * @param transaction
     * @param resourceId
     * @param exclusive
     * @param timeout     A timeout value (in ms) or a negative value (no timeout)
     */
    public static EntityLock lock(Transaction transaction, long resourceId, boolean exclusive, long timeout) {
        return transaction.lock(resourceLocks, resourceId, exclusive, timeout);
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
        return toString() + "@" + version;
    }

    /**
     * Get the ID
     */
    public Long getId() {
        return resourceID;
    }

    /**
     * Update the state of the resource by checking all that could have changed externally
     * <p>
     * Calls {@linkplain #doUpdateStatus()}.
     * If the update fails for some reason, then we just put the state into HOLD
     */
    final public boolean updateStatus() {
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
     * @param t
     * @param em
     * @param message The message
     */
    public void notify(Transaction t, EntityManager em, Message message) {
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

        try {
            return path = getConnector().resolve(locator);
        } catch (IOException e) {
            throw new AssertionError("Unexpected conversion error", e);
        }
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
    public boolean setState(ResourceState state) {
        if (this.state == state)
            return false;
        this.state = state;

        // FIXME: Should be done when the operation is committed
//        if (this.isStored()) {
//            Scheduler.get().notify(new SimpleMessage(Message.Type.STATE_CHANGED, this));
//        }
        return true;
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

    protected Dependency addIngoingDependency(Dependency dependency) {
        dependency.to = this;
        return this.ingoingDependencies.put(dependency.getFrom(), dependency);
    }

    /**
     * Called when the resource was persisted/updated and committed
     */
    public void stored() {
        // We switched from a stopped state to a non stopped state : notify dependents
        if (oldState != null && oldState.isUnactive() ^ state.isUnactive()) {
            Scheduler.get().addChangedResource(this);
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
    public synchronized void delete(boolean recursive) {
        if (getState() == ResourceState.RUNNING)
            throw new XPMRuntimeException("Cannot delete the running task [%s]", this);

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
        Transaction.run((em, t) -> {
            Resource ourselves = em.find(Resource.class, getId());
            em.remove(ourselves);
            SimpleMessage message = new SimpleMessage(Message.Type.RESOURCE_REMOVED, ourselves);
            this.notify(t, em, message);
            notify(t, em, message);
        });


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
        return connector == null ? LocalhostConnector.getInstance() : null;
    }

    /**
     * Returns the main connector associated with this resource
     *
     * @return a SingleHostConnector object
     */
    public final SingleHostConnector getMainConnector() {
        return getConnector().getMainConnector();
    }

    public EntityLock lock(Transaction t, boolean exclusive) {
        return t.lock(resourceLocks, this.getId(), exclusive, 0);
    }

    public EntityLock lock(Transaction t, boolean exclusive, long timeout) {
        return t.lock(resourceLocks, this.getId(), exclusive, timeout);
    }

    @PostLoad
    protected void postLoad() {
        cacheState();
        prepared = true;
        LOGGER.debug("Loaded %s (state %s) - version %d", this, state, version);
    }

    /**
     * Cache the current state to allow notification when resource is stored in database
     */
    protected void cacheState() {
        oldState = state;
    }

    /**
     * Called after an INSERT or UPDATE
     */
    @PostUpdate
    @PostPersist
    public void _post_update() {
        Transaction.current().registerPostCommit(this::saved);
    }

    @PostRemove
    public void postRemove() {
        Transaction.current().registerPostCommit(this::removed);
    }

    public void save(Transaction transaction) {
        final EntityManager em = transaction.em();

        // Lock all the dependencies
        // This avoids to miss any notification
        List<EntityLock> locks = new ArrayList<>();
        lockDependencies:
        while (true) {
            // We loop until we get all the locks - using a timeout just in case
            for (Dependency dependency : getDependencies()) {
                final EntityLock lock = dependency.from.lock(transaction, false, 100);
                if (lock == null) {
                    for (EntityLock _lock : locks) {
                        _lock.close();
                    }
                    continue lockDependencies;
                }

                locks.add(lock);
            }

            break;
        }

        for (Dependency dependency : getDependencies()) {
            if (!em.contains(dependency.from)) {
                dependency.from = em.find(Resource.class, dependency.from.getId());
            } else {
                em.refresh(dependency.from);
            }
        }

        prepared = true;
        em.persist(this);

        // Update the status
        updateStatus();
    }

    /**
     * Called before adding this entity to the database
     */
    @PrePersist
    protected void prePersist() {
        if (!prepared) {
            throw new AssertionError("The object has not been prepared");
        }
    }

    public void removed(Transaction transaction) {
        LOGGER.debug("Resource %s removed", this, version);
        Scheduler.get().notify(new SimpleMessage(Message.Type.RESOURCE_REMOVED, this));
        clean();
    }

    public void saved(Transaction transaction) {
        LOGGER.debug("Resource %s stored with version=%s", this, version);
        if (oldState != state) {
            if (oldState == null) {
                Scheduler.get().notify(new SimpleMessage(Message.Type.RESOURCE_ADDED, this));
            } else {
                Scheduler.get().notify(new SimpleMessage(Message.Type.STATE_CHANGED, this));
            }
        }
        stored();
        cacheState();

        // Move back to false
        prepared = false;
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

    /**
     * Defines how printing should be done
     */
    static public class PrintConfig {
        public String detailURL;
    }

}
