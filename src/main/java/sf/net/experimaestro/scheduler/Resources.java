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
import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.SecondaryIndex;
import org.apache.commons.lang.NotImplementedException;
import sf.net.experimaestro.manager.DotName;
import sf.net.experimaestro.utils.Heap;
import sf.net.experimaestro.utils.Trie;
import sf.net.experimaestro.utils.log.Logger;

/**
 *  A set of resources
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
    private final SecondaryIndex<String, ResourceLocator,Resource> groups;

    /**
     * A Trie
     */
    Trie<String, DotName> groupsTrie = new Trie<>();

    /**
	 * Initialise the set of resources
	 *
     * @param scheduler The scheduler
     * @param readyJobs A heap where the resources that are ready will be placed during the initialisation
	 * @param dbStore
	 * @throws DatabaseException
	 */
	public Resources(Scheduler scheduler, EntityStore dbStore, Heap<Job> readyJobs)
			throws DatabaseException {
        super(dbStore.getPrimaryIndex(ResourceLocator.class, Resource.class));
        groups = dbStore.getSecondaryIndex(index, String.class, Resource.GROUP_KEY_NAME);
        this.scheduler = scheduler;

        // Loop over all the resources
        for (Resource resource : this) {
            groupsTrie.put(DotName.parse(resource.getGroup()));
            if (ResourceState.ACTIVE.contains(resource.getState())) {
                // Initialize the resource
                resource.init(scheduler);
                // Add job to the queue if ready
                if (resource instanceof Job && resource.getState() == ResourceState.READY)
                    readyJobs.add((Job) resource);
            }
        }

        // Loop over all groups


    }


    @Override
    public synchronized boolean put(Resource resource) throws DatabaseException {
        groupsTrie.put(DotName.parse(resource.getGroup()));
        return super.put(resource);
    }

    @Override
    protected boolean canOverride(Resource old, Resource resource) {
        if (old.state == ResourceState.RUNNING && resource != old) {
            LOGGER.error(String.format("Cannot override a running task [%s] / %s vs %s", resource.locator,
                    resource.hashCode(), old.hashCode()));
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


    public Iterable<Resource> fromGroup(final String group, boolean recursive) {
        if (!recursive)
            return groups.entities(group, true, group, true);

         throw new NotImplementedException();
    }

    /**
     * Returns subgroups
     * @param group
     * @return
     */
    public HashMultiset<String> subgroups(String group) {
        System.err.format("Searching children of group [%s]%n", group);
        final Trie<String, DotName> node = groupsTrie.find(DotName.parse(group));
        final HashMultiset<String> set = HashMultiset.create();
        if (node == null)
            return set;

        System.err.format("Found a node%n");
        for(String key: node.childrenKeys())
            set.add(key);

        return set;
    }
}
