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

import com.google.common.collect.HashMultiset;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.SecondaryIndex;
import sf.net.experimaestro.exceptions.ExperimaestroCannotOverwrite;
import sf.net.experimaestro.manager.DotName;
import sf.net.experimaestro.utils.Heap;
import sf.net.experimaestro.utils.Trie;
import sf.net.experimaestro.utils.log.Logger;

import java.util.Collection;
import java.util.EnumSet;
import java.util.TreeMap;

import static com.sleepycat.je.CursorConfig.READ_UNCOMMITTED;

/**
 * A set of resources
 * <p/>
 * Resources are stored in two databases:
 * <ol>
 * <li>The main resource</li>
 * <li>The <b>active</b> dependencies between resources (only dependencies of jobs in a waiting/holding
 * state are stored here)</li>
 * </ol>
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
abstract public class Resources extends CachedEntitiesStore<Long, Resource> {
    final static private Logger LOGGER = Logger.getLogger();


    /**
     * The associated scheduler
     */
    private final Scheduler scheduler;


    /**
     * Resources by state
     */
    private final SecondaryIndex<ResourceState, Long, Resource> resourceByState;


    /**
     * Resource data
     */
    private final PrimaryIndex<ResourceLocator, ResourceData> data;

    /**
     * Resource data by resource ID
     */
    private final SecondaryIndex<Long, ResourceLocator, ResourceData> dataByID;


    /**
     * The dependencies
     */
    private PrimaryIndex<Long, Dependency> dependencies;

    /**
     * Access to the index that gives dependent resources
     */
    private SecondaryIndex<Long, Long, Dependency> fromDependencies;

    /**
     * Access to the index that gives required resources
     */
    private SecondaryIndex<Long, Long, Dependency> toDependencies;

    /**
     * The dataByGroup the resources belong to
     */
    private final SecondaryIndex<GroupId, ResourceLocator, ResourceData> dataByGroup;


    /**
     * A Trie
     */
    Trie<String, DotName> groupsTrie = new Trie<>();

    /**
     * Initialise the set of resources
     *
     * @param scheduler The scheduler
     * @param dbStore
     * @throws DatabaseException
     */
    public Resources(Scheduler scheduler, EntityStore dbStore)
            throws DatabaseException {
        super(dbStore.getPrimaryIndex(Long.class, Resource.class));

        this.scheduler = scheduler;

        resourceByState = dbStore.getSecondaryIndex(index, ResourceState.class, Resource.STATE_KEY_NAME);

        // Create dependencies indices
        dependencies = dbStore.getPrimaryIndex(Long.class, Dependency.class);
        fromDependencies = dbStore.getSecondaryIndex(dependencies, Long.class, Dependency.FROM_KEY_NAME);
        toDependencies = dbStore.getSecondaryIndex(dependencies, Long.class, Dependency.TO_KEY_NAME);


        // Resource data
        data = dbStore.getPrimaryIndex(ResourceLocator.class, ResourceData.class);
        dataByID = dbStore.getSecondaryIndex(data, Long.class, ResourceData.RESOURCE_ID_NAME);
        dataByGroup = dbStore.getSecondaryIndex(data, GroupId.class, ResourceData.GROUP_KEY_NAME);

    }

    void init(Heap<Job<? extends JobData>> readyJobs) {
        // Get the groups
        try (final EntityCursor<GroupId> keys = dataByGroup.keys()) {
            for (GroupId key : keys)
                groupsTrie.put(DotName.parse(key.getName()));
        }

        update(ResourceState.RUNNING, readyJobs);
        update(ResourceState.READY, readyJobs);

        // Now, update waiting tasks (so they can be ready if a job finished but we did not
        // get notified)
        update(ResourceState.WAITING, readyJobs);
    }

    @Override
    public void close() {
        super.close();
        dependencies.getDatabase().close();
        data.getDatabase().close();
    }

    /**
     * Update resources with a given state
     */
    private void update(ResourceState status, Heap<Job<? extends JobData>> readyJobs) {
        try (final EntityCursor<Resource> cursor = resourceByState.entities(null, status, true, status, true, CursorConfig.READ_UNCOMMITTED)) {
            for (Resource resource : cursor) {
                try {
                    resource.init(scheduler);
                    if (updateStatus(resource, false))
                        cursor.update(resource);

                    switch (resource.state) {
                        case READY:
                            readyJobs.add((Job<? extends JobData>) resource);
                    }

                } catch (Exception e) {
                    LOGGER.error(e, "Error while updating resource %s", resource);
                }
            }
        }
    }


    /**
     * Store a resource
     *
     * @param resource
     * @return The old resource, or null if there was nothing
     * @throws DatabaseException            If an error occured while writing the resource
     * @throws ExperimaestroCannotOverwrite If the old resource could not be overriden
     */
    @Override
    public Resource put(Resource resource) throws DatabaseException, ExperimaestroCannotOverwrite {
        // Get the group
        LOGGER.debug("Storing resource [%s]", resource);
        groupsTrie.put(DotName.parse(resource.getData().getGroupId()));

        final boolean newResource = !resource.stored();

        // Update status before starting
        resource.updateStatus(false);
        final Resource old = super.put(resource);

        if (newResource) {
            final long id = resource.getId();
            LOGGER.debug("Adding a new resource [%s] in database [id=%d]", resource, id);

            // Add the data
            final ResourceData resourceData = resource.getData();
            resourceData.setResourceID(id);
            this.data.put(resourceData);

            // Add the dependencies
            final Collection<Dependency> deps = resource.getDependencies();
            for (Dependency dependency : deps) {
                dependency.setTo(id);
                dependencies.put(dependency);
            }
        }


        return old;
    }

    @Override
    protected boolean canOverride(Resource old, Resource current) {
        return old.canBeOverriden(current);
    }


    @Override
    protected void init(Resource resource) {
        resource.init(scheduler);
    }


    /**
     * Returns resources filtered by state and group
     *
     * @param group
     * @param recursive
     * @return
     */
    public EntityCursor<ResourceData> fromGroup(final String group, EnumSet<ResourceState> states, boolean recursive) {
        String end = group;
        if (recursive) {
            end = group + ".";
        }
        return dataByGroup.entities(new GroupId(group), true, new GroupId(end), true);
    }

    /**
     * Returns subgroups
     *
     * @param group
     * @return
     */
    public HashMultiset<String> subgroups(String group) {
//        System.err.format("Searching children of group [%s]%n", group);
        final Trie<String, DotName> node = groupsTrie.find(DotName.parse(group));
        final HashMultiset<String> set = HashMultiset.create();
        if (node == null)
            return set;

//        System.err.format("Found a node%n");
        for (String key : node.childrenKeys())
            set.add(key);

        return set;
    }

    /**
     * Notify the dependencies that a resource has changed
     *
     * @param resource The resource that has changed
     */
    public void notifyDependencies(Resource resource) {
        // Join between active states
        // TODO: should limit to the dependencies of some resources
        final long from = resource.getId();

        // Notify dependencies in turn
        try (final EntityCursor<Dependency> entities = fromDependencies.entities(null, from, true, from, true, READ_UNCOMMITTED)) {
            for (Dependency dep : entities) {
                if (dep.status != DependencyStatus.UNACTIVE)
                    try {
                        // when the dependency status is null, the dependency is not active anymore
                        LOGGER.debug("Notifying dependency: [%s] to [%s]; current state=%s", from, dep.getTo(), dep.status);
                        // Preserves the previous state
                        DependencyStatus beforeState = dep.status;

                        if (dep.update(scheduler, resource, false)) {
                            final Resource depResource = get(dep.getTo());

                            synchronized (depResource) {
                                // Update the dependency in database
                                store(dep);

                                // Notify the resource that a dependency has changed
                                depResource.notify(depResource, new DependencyChangedMessage(dep, beforeState, dep.status));
                                LOGGER.debug("After notification [%s -> %s], state is %s for [%s]", beforeState, dep.status, depResource.state, depResource);
                            }
                        } else {
                            LOGGER.debug("No change in dependency status [%s -> %s]", beforeState, dep.status);
                        }
                    } catch (RuntimeException e) {
                        LOGGER.error(e, "Got an exception while notifying [%s]", resource);
                    }
            }
        }

    }


    /**
     * Retrieves resources on which the given resource depends
     *
     * @param to The resource
     * @return A map of dependencies
     */
    public TreeMap<Long, Dependency> retrieveDependencies(long to) {
        return getDependencies(to, toDependencies);
    }

    /**
     * Retrieves resources that depend upon the given resource
     *
     * @param from The resource
     * @return A map of dependencies
     */
    public TreeMap<Long, Dependency> retrieveDependentResources(long from) {
        return getDependencies(from, fromDependencies);
    }


    /**
     * Retrieve the dependencies from a given secondary index, and fills a {@link java.util.Map}
     * from it
     *
     * @param id    The key
     * @param index The index
     * @return
     */
    private TreeMap<Long, Dependency> getDependencies(Long id, SecondaryIndex<Long, Long, Dependency> index) {
        TreeMap<Long, Dependency> deps = new TreeMap<>();
        try (final EntityCursor<Dependency> entities = index.entities(id, true, id, true)) {
            for (Dependency dependency : entities)
                deps.put(dependency.getFrom(), dependency);
        }
        return deps;
    }

    public ResourceData getData(Long resourceID) {
        return dataByID.get(resourceID);
    }

    public Resource getByLocator(ResourceLocator locator) {
        assert locator != null;
        final ResourceData resourceData = data.get(locator);
        if (resourceData == null)
            return null;

        return get(resourceData.resourceId);
    }

    /**
     * Update the status of a resource
     */
    protected abstract boolean updateStatus(Resource resource, boolean store);

    public void store(Dependency dependency) {
        dependencies.put(dependency);
    }

    @Override
    public Resource getSame(Resource resource) {
        return getByLocator(resource.getLocator());
    }


}
