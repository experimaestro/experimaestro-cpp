/*
 *
 *  This file is part of experimaestro.
 *  Copyright (c) 2012 B. Piwowarski <benjamin@bpiwowar.net>
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
import sf.net.experimaestro.connectors.Connector;
import sf.net.experimaestro.utils.iterators.AbstractIterator;
import sf.net.experimaestro.utils.log.Logger;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.WeakHashMap;

/**
 * Stores the connectors in a database
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class ComputationalResources implements Iterable<Connector> {
	final static private Logger LOGGER = Logger.getLogger();

	/** The index */
	private PrimaryIndex<String, ComputationalResource> index;

	/**
	 * A cache to get track of connectors in memory
	 */
	private WeakHashMap<String, WeakReference<Connector>> cache = new WeakHashMap<String, WeakReference<Connector>>();

	/**
	 * The associated scheduler
	 */
	private final Scheduler scheduler;

	/**
	 * Initialise the set of connectors
	 *
	 * @param dbStore
	 * @throws com.sleepycat.je.DatabaseException
	 */
	public ComputationalResources(Scheduler scheduler, EntityStore dbStore)
			throws DatabaseException {
		this.scheduler = scheduler;
		index = dbStore.getPrimaryIndex(String.class, ComputationalResource.class);
	}

	/**
	 * Store the resource in the database - called when an entity has changed
	 *
	 *
	 * @param resource
	 *            The resource to add
	 * @return True if the insertion was successful, or false if the resource
	 *         was not updated (e.g. because it is a running job)
	 * @throws com.sleepycat.je.DatabaseException
	 *             If an error occurs while putting the resource in the database
	 */
	synchronized public boolean put(ComputationalResource resource) throws DatabaseException {
		// Check if overriding a running resource (unless it is the same object)
		Connector old = get(resource.getIdentifier());

		// Store in database and in cache
		index.put(resource);
		cache.put(resource.getIdentifier(), new WeakReference<Connector>(resource));

		// OK, we did update
		return true;
	}

	/**
	 * Get a connector
	 *
	 * @param id
	 * @return The connector or null if no such entities exist
	 * @throws com.sleepycat.je.DatabaseException
	 */
	synchronized public Connector get(String id) throws DatabaseException {
		Connector connector = getFromCache(id);
		if (connector != null)
			return connector;

		// Get from the database
		connector = index.get(id);
		return connector;
	}

	private Connector getFromCache(String id) {
		// Try to get from cache first
		WeakReference<Connector> reference = cache.get(id);
		if (reference != null) {
			Connector connector = reference.get();
			if (connector != null)
				return connector;
		}
		return null;
	}

	@Override
	public Iterator<Connector> iterator() {
		try {
			return new AbstractIterator<Connector>() {
				EntityCursor<String> iterator = index.keys();

				@Override
				protected void finalize() throws Throwable {
					super.finalize();
					if (iterator != null)
						iterator.close();
				}

				@Override
				protected boolean storeNext() {
					try {
						String id = iterator.next();
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
							LOGGER.error(e, "Could not close the cursor on connectors");
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
