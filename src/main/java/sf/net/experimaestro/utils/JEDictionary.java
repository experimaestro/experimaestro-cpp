package sf.net.experimaestro.utils;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.SecondaryIndex;
import com.sleepycat.persist.StoreConfig;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import com.sleepycat.persist.model.Relationship;
import com.sleepycat.persist.model.SecondaryKey;

/**
 * Dictionary backed up by the JE database
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class JEDictionary {

	/**
	 * Our entities: a string and a long
	 * @author B. Piwowarski <benjamin@bpiwowar.net>
	 */
	@Entity
	static public class Entry {
		@PrimaryKey
		String key;

		@SecondaryKey(relate = Relationship.ONE_TO_ONE)
		long id;

		public Entry() {
		}

		public Entry(String key, long id) {
			this.key = key;
			this.id = id;
		}

	}

	private EntityStore store;
	private PrimaryIndex<String, Entry> index;
	private SecondaryIndex<Long, String, Entry> entries;

	static public final int NULL_ID = -1;

	/**
	 * The size of the store (used to give a new id to a new entry)
	 */
	private long size;

	public JEDictionary(Environment environment, String name)
			throws DatabaseException {
		StoreConfig storeConfig = new StoreConfig();
		storeConfig.setReadOnly(environment.getConfig().getReadOnly());

		store = new EntityStore(environment, name, storeConfig);
		index = store.getPrimaryIndex(String.class, Entry.class);
		entries = store.getSecondaryIndex(index, Long.TYPE, "id");
		size = index.count();
	}

	public void shutdown() throws DatabaseException {
		if (store != null) {
			store.close();
			store = null;
		}
	}

	/**
	 * @param key
	 * @return
	 * @throws DatabaseException
	 */
	long getId(String key) throws DatabaseException {
		Entry entry = index.get(key);
		if (entry != null)
			return entry.id;
		return NULL_ID;
	}

	/**
	 * Get the key for a given id
	 * 
	 * @param id
	 * @return
	 * @throws DatabaseException
	 */
	String getKey(long id) throws DatabaseException {
		Entry entry = entries.get(id);
		if (entry != null)
			return entry.key;
		return null;
	}

	/**
	 * Create (if not existent) a new entry
	 * 
	 * @param key The key to insert
	 * @return The id of the key
	 * @throws DatabaseException
	 */
	long create(String key) throws DatabaseException {
		long id = getId(key);
		if (id == NULL_ID) {
			id = size;
			index.put(new Entry(key, id));
			size++;
		}
		return id;
	}

	@Override
	protected void finalize() throws Throwable {
		shutdown();
	}
}
