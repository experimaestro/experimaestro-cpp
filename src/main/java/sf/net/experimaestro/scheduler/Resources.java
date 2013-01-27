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
import com.sleepycat.je.Transaction;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.SecondaryIndex;
import org.apache.commons.lang.NotImplementedException;
import sf.net.experimaestro.exceptions.ExperimaestroCannotOverwrite;
import sf.net.experimaestro.manager.DotName;
import sf.net.experimaestro.utils.CloseableIterable;
import sf.net.experimaestro.utils.Heap;
import sf.net.experimaestro.utils.Trie;
import sf.net.experimaestro.utils.log.Logger;

import java.util.ArrayList;
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
public class Resources extends CachedEntitiesStore<ResourceLocator, Resource> {
    final static private Logger LOGGER = Logger.getLogger();


    /**
     * The associated scheduler
     */
    private final Scheduler scheduler;

    /**
     * The groups the resources belong to
     */
    private final SecondaryIndex<String, ResourceLocator, Resource> groups;

    /**
     * The dependencies
     */
    private PrimaryIndex<Long, Dependency> dependencies;

    /**
     * Access to the depencies
     */
    private SecondaryIndex<ResourceLocator, Long, Dependency> dependenciesFrom;
    private SecondaryIndex<ResourceLocator, Long, Dependency> dependenciesTo;

    /**
     * The job states
     */
    private SecondaryIndex<ResourceState, ResourceLocator, Resource> states;

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
        super(dbStore.getPrimaryIndex(ResourceLocator.class, Resource.class));

        groups = dbStore.getSecondaryIndex(index, String.class, Resource.GROUP_KEY_NAME);
        dependencies = dbStore.getPrimaryIndex(Long.class, Dependency.class);
        dependenciesFrom = dbStore.getSecondaryIndex(dependencies, ResourceLocator.class, Dependency.FROM_KEY_NAME);
        dependenciesTo = dbStore.getSecondaryIndex(dependencies, ResourceLocator.class, Dependency.TO_KEY_NAME);
        states = dbStore.getSecondaryIndex(index, ResourceState.class, Resource.STATE_KEY_NAME);

        this.scheduler = scheduler;
    }

    void init(Heap<Job> readyJobs) {
        // Get the groups
        try (final EntityCursor<String> keys = groups.keys()) {
            for (String key : keys)
                groupsTrie.put(DotName.parse(key));
        }

        update(ResourceState.RUNNING, readyJobs);
        update(ResourceState.READY, readyJobs);

        // Now, update waiting tasks (so they can be ready if a job finished but we did not
        // get notified)
        update(ResourceState.WAITING, readyJobs);
    }

    /**
     * Update resources in a given state
     */
    private void update(ResourceState state, Heap<Job> readyJobs) {
        try (final EntityCursor<Resource> cursor = states.entities(null, state, true, state, true, CursorConfig.READ_UNCOMMITTED)) {
            for (Resource resource : wrap(cursor)) {
                resource.init(scheduler);
                try {
                    if (resource.updateStatus(null, false))
                        put(null, resource);

                    switch (resource.getState()) {
                        case READY:
                            readyJobs.add((Job) resource);
                    }

                } catch (Exception e) {
                    LOGGER.error(e, "Error while updating resource %s", resource);
                }
            }
        }
    }


    @Override
    public synchronized Resource put(XPMTransaction txn, Resource resource) throws DatabaseException, ExperimaestroCannotOverwrite {
        return put(txn, resource, null);
    }

    public Resource put(XPMTransaction txn, Resource resource, Resource.Changes changes) throws DatabaseException, ExperimaestroCannotOverwrite {
        // Get the group
        groupsTrie.put(DotName.parse(resource.getGroup()));

        Resource old = null;
        // Starts the transaction
        if (changes == null || changes.state)
            old = super.put(txn, resource);

        if (changes == null || !changes.dependencies.isEmpty() && resource.retrievedDependencies()) {
            // TODO: more fine grained update would be better
            // Delete everything
            LOGGER.debug("Deleting dependencies to %s [%b]", resource, txn != null);
            final Transaction transaction = txn != null ? txn.getTransaction() : null;
            dependenciesTo.delete(transaction, resource.getLocator());

            // Store the dependencies
            LOGGER.debug("Storing dependencies from %s [%b]", resource, txn != null);
            for (Dependency dependency : resource.getDependencies()) {
                dependencies.put(transaction, dependency);
            }
        }

        return old;
    }

    @Override
    protected boolean canOverride(Resource old, Resource resource) {
        if (old.state == ResourceState.RUNNING && resource != old) {
            LOGGER.error(String.format("Cannot override a running task [%s] / %s vs %s", resource.locator,
                    System.identityHashCode(resource), System.identityHashCode(old.hashCode())));
            return false;
        }
        return true;
    }


    @Override
    protected ResourceLocator getKey(Resource resource) {
        return resource.getLocator();
    }

    @Override
    protected void init(Resource resource) {
        resource.init(scheduler);
    }


    public CloseableIterable<Resource> fromGroup(final Transaction txn, final String group, boolean recursive) {
        final CursorConfig config = new CursorConfig();
        if (!recursive) {
            return new IterableWrapper(groups.entities(txn, group, true, group, true, config));
        }
        throw new NotImplementedException();
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
        final ResourceLocator from = resource.getLocator();

        // Get all the dependencies
        ArrayList<Dependency> dependencies = new ArrayList<>();
        try (final EntityCursor<Dependency> entities = dependenciesFrom.entities(null, from, true, from, true, READ_UNCOMMITTED)) {
            Dependency dep;
            while ((dep = entities.next()) != null)
                dependencies.add(dep);
        }

        // Notify each of these
        resource.init(scheduler);
        for (Dependency dependency : dependencies) {
            final ResourceLocator to = dependency.getTo();
            Resource dep = get(to);
            try {
                if (dep == null)
                    LOGGER.warn("Dependency [%s] of [%s] was not found", from, to);
                else {
                    LOGGER.info("Notifying dependency [%s] from [%s]", to, from);
                    dep.init(scheduler);
                    dep.notify(resource);
                }
            } catch (RuntimeException e) {
                LOGGER.error(e, "Got an exception while notifying [%s]", resource);
            }
        }
    }


    /**
     * Retrieves resources on which the given resource depends
     *
     * @param to The resource
     * @return A map of dependencies
     */
    public TreeMap<ResourceLocator, Dependency> retrieveDependencies(ResourceLocator to) {
        return getDependencies(to, dependenciesTo);
    }

    /**
     * Retrieves resources that depend upon the given resource
     *
     * @param from The resource
     * @return A map of dependencies
     */
    public TreeMap<ResourceLocator, Dependency> retrieveDependentResources(ResourceLocator from) {
        return getDependencies(from, dependenciesFrom);
    }


    /**
     * Retrieve the dependencies from a given secondary index, and fills a {@link java.util.Map}
     * from it
     *
     * @param locator The key
     * @param index   The index
     * @return
     */
    private TreeMap<ResourceLocator, Dependency> getDependencies(ResourceLocator locator, SecondaryIndex<ResourceLocator, Long, Dependency> index) {
        TreeMap<ResourceLocator, Dependency> deps = new TreeMap<>();
        try (final EntityCursor<Dependency> entities = index.entities(locator, true, locator, true)) {
            for (Dependency dependency : entities)
                deps.put(dependency.getFrom(), dependency);
        }
        return deps;
    }

}
