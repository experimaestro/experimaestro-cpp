package sf.net.experimaestro.scheduler;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Timer;
import java.util.TreeMap;
import java.util.WeakHashMap;

import sf.net.experimaestro.utils.iterators.AbstractIterator;
import sf.net.experimaestro.utils.log.Logger;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;

/**
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
/**
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * 
 */
public class Resources implements Iterable<Resource> {
	final static private Logger LOGGER = Logger.getLogger();

	/** The index */
	private PrimaryIndex<String, Resource> index;

	/**
	 * A cache to get track of resources in memory
	 */
	private WeakHashMap<String, WeakReference<Resource>> cache = new WeakHashMap<String, WeakReference<Resource>>();

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
		index = dbStore.getPrimaryIndex(String.class, Resource.class);
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
		Resource old = get(resource.getIdentifier());

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
		cache.put(resource.identifier, new WeakReference<Resource>(resource));

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
	synchronized public Resource get(String id) throws DatabaseException {
		Resource resource = getFromCache(id);
		if (resource != null)
			return resource;

		// Get from the database
		resource = index.get(id);
		if (resource != null) {
			resource.scheduler = scheduler;
		}
		return resource;
	}

	private Resource getFromCache(String id) {
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
							LOGGER.error(e, "Could not close the cursor on resources");
						}

					}

					return false;
				}
			};
		} catch (DatabaseException e) {
			throw new RuntimeException(e);
		}
	};

}
