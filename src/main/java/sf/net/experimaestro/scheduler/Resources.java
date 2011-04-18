package sf.net.experimaestro.scheduler;

import java.util.Iterator;
import java.util.TreeMap;

import sf.net.experimaestro.utils.iterators.AbstractIterator;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;

/**
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class Resources implements Iterable<Resource> {
	PrimaryIndex<String, Resource> primaryKey;

	/**
	 * Set of active resources (waiting or monitored)
	 */
	TreeMap<String, Resource> actives = new TreeMap<String, Resource>();

	/**
	 * Initialise the set of resources
	 * @param dbStore
	 * @throws DatabaseException
	 */
	public Resources(EntityStore dbStore) throws DatabaseException {
		primaryKey = dbStore.getPrimaryIndex(String.class, Resource.class);
	}

	/**
	 * Store the resource in the database - called when an entity has changed
	 * 
	 * @param resource
	 * @throws DatabaseException
	 */
	public void put(Resource resource) throws DatabaseException {
		// Add or remove from the active list
		if (resource.isActive()) {
			actives.put(resource.getIdentifier(), resource);
		} else {
			actives.remove(resource.getIdentifier());
		}

		// Store in database
		primaryKey.put(resource);
	}

	/**
	 * Get a resource
	 * 
	 * @param id
	 * @return The resource or null if no such entities exist
	 * @throws DatabaseException
	 */
	public Resource get(String id) throws DatabaseException {
		Resource resource = actives.get(id);
		if (resource != null)
			return resource;
		return primaryKey.get(id);
	}

	/**
	 * Get an iterator on active resources
	 */
	Iterable<Resource> actives() {
		return actives.values();
	}

	@Override
	public Iterator<Resource> iterator() {
		try {
			return new AbstractIterator<Resource>() {
				EntityCursor<Resource> iterator = primaryKey.entities();

				@Override
				protected boolean storeNext() {
					try {
						value = iterator.next();
						if (value != null) {
							// Override with active resource
							Resource resource = actives.get(value.identifier);
							if (resource != null)
								value = resource;
						}
					} catch (DatabaseException e) {
						throw new RuntimeException(e);
					}
					return value != null;
				}
			};
		} catch (DatabaseException e) {
			throw new RuntimeException(e);
		}
	};

}
