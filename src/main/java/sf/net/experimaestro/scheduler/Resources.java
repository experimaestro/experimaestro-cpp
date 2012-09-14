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
import sf.net.experimaestro.utils.log.Logger;

/**
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
	 * Initialise the set of resources
	 * 
	 * @param dbStore
	 * @throws DatabaseException
	 */
	public Resources(Scheduler scheduler, EntityStore dbStore)
			throws DatabaseException {
        super(dbStore.getPrimaryIndex(ResourceLocator.class, Resource.class));
		this.scheduler = scheduler;

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


}
