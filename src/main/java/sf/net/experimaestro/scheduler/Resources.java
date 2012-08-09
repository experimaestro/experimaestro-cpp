/*
 *
 *  This file is part of experimaestro.
 *  Copyright (c) 2011 B. Piwowarski <benjamin@bpiwowar.net>
 *
 *  experimaestro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  experimaestro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package sf.net.experimaestro.scheduler;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import sf.net.experimaestro.utils.iterators.AbstractIterator;
import sf.net.experimaestro.utils.log.Logger;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.WeakHashMap;

/**
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class Resources implements Iterable<Resource> {
	final static private Logger LOGGER = Logger.getLogger();

	/** The index */
	private PrimaryIndex<Locator, Resource> index;

	/**
	 * A cache to get track of resources in memory
	 */
	private WeakHashMap<Locator, WeakReference<Resource>> cache = new WeakHashMap<Locator, WeakReference<Resource>>();

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
		this.scheduler = scheduler;
		index = dbStore.getPrimaryIndex(Locator.class, Resource.class);
	}

	/**
	 * Store the resource in the database - called when an entity has changed
	 * 
	 * 
	 * @param resource
	 *            The resource to add
	 * @return True if the insertion was successful, or false if the resource
	 *         was not updated (e.g. because it is a running job)
	 * @throws DatabaseException
	 *             If an error occurs while putting the resource in the database
	 */
	synchronized public boolean put(Resource resource) throws DatabaseException {
		// Check if overriding a running resource (unless it is the same object)
		Resource old = get(resource.locator);

		if (old != null) {
			// Don't override a running task
			if (old.state == ResourceState.RUNNING && resource != old) {
				LOGGER.warn("Cannot override a running task [%s]");
				return false;
			}
			// FIXME: should do something
		}

		// Store in database and in cache
		index.put(resource);
		cache.put(resource.locator, new WeakReference<Resource>(resource));

		// OK, we did update
		return true;
	}

	/**
	 * Get a resource
	 * 
	 * @param id
	 * @return The resource or null if no such entities exist
	 * @throws DatabaseException
	 */
	synchronized public Resource get(Locator id) throws DatabaseException {
		Resource resource = getFromCache(id);
		if (resource != null)
			return resource;

		// Get from the database
		resource = index.get(id);
		if (resource != null) {
            resource.init(scheduler);
		}
		return resource;
	}

	private Resource getFromCache(Locator id) {
		// Try to get from cache first
		WeakReference<Resource> reference = cache.get(id);
		if (reference != null) {
			Resource resource = reference.get();
			if (resource != null)
				return resource;
		}
		return null;
	}

	@Override
	public Iterator<Resource> iterator() {
		try {
			return new AbstractIterator<Resource>() {
				EntityCursor<Locator> iterator = index.keys();

				@Override
				protected void finalize() throws Throwable {
					super.finalize();
					if (iterator != null)
						iterator.close();
				}

				@Override
				protected boolean storeNext() {
					try {
						Locator id = iterator.next();
						if (id != null) {
							value = get(id);
							return true;
						}
					} catch (DatabaseException e) {
						throw new RuntimeException(e);
					}

					if (iterator != null) {
						try {
							iterator.close();
						} catch (DatabaseException e) {
							LOGGER.error(e, "Could not close the cursor on resources");
						}

					}

					return false;
				}
			};
		} catch (DatabaseException e) {
			throw new RuntimeException(e);
		}
	}

}
