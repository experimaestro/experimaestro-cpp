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
import com.sleepycat.je.Transaction;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.PrimaryIndex;
import sf.net.experimaestro.utils.iterators.AbstractIterator;
import sf.net.experimaestro.utils.log.Logger;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.WeakHashMap;


/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
abstract public class CachedEntitiesStore<Key, Value> implements Iterable<Value> {
    final static private Logger LOGGER = Logger.getLogger();

    /**
     * The index
     */
    protected PrimaryIndex<Key, Value> index;

    /**
     * A cache to get track of values in memory
     */
    private WeakHashMap<Key, WeakReference<Value>> cache = new WeakHashMap<>();


    /**
     * Returns whether the value can be overriden
     * @param old The previous value (guaranteed to be not null)
     * @param value The value
     * @return True or false
     */
    protected abstract boolean canOverride(Value old, Value value);

    /**
     * Returns a copy of the key of the entity
     */
    protected abstract Key getKey(Value value);

    /**
     * Init a given value
     * @param value
     */
    protected abstract void init(Value value);


    /**
     * Initialise the set of values
     *
     * @param index The index
     * @throws com.sleepycat.je.DatabaseException
     *
     */
    public CachedEntitiesStore(PrimaryIndex<Key, Value> index)
            throws DatabaseException {
        this.index = index;
    }

    /**
     * Store the value in the database - called when an entity has changed
     *
     * @param value The value to append
     * @return True if the insertion was successful, or false if the value
     *         was not updated (e.g. because it is a running job)
     * @throws com.sleepycat.je.DatabaseException
     *          If an error occurs while putting the value in the database
     */
    synchronized public boolean put(Transaction txn, Value value) throws DatabaseException {
        // Check if overriding a running value (unless it is the same object)
        final Key key = getKey(value);
        Value old = get(key);
        if (old != null) {
            // Check if we can override
            if (!canOverride(old, value)) {
                return false;
            }
        }


        // Store in database and in cache
        try {
            index.put(txn, value);
        } catch(DatabaseException e) {
            // TODO: We should have a memory cache for failed updates
            // so that those are kept in memory for a while
            LOGGER.error("Caught a database exception (%s) while updating %s", e, value);
        }
        cache.put(key, new WeakReference<>(value));
        LOGGER.debug("Stored value [%s@%s] in database", key, value.hashCode());
        // OK, we did updateFromStatusFile
        return true;
    }



    /**
     * Get a value
     *
     * @param id
     * @return The value or null if no such entities exist
     * @throws com.sleepycat.je.DatabaseException
     *
     */
    final synchronized public Value get(Key id) throws DatabaseException {
        Value value = getFromCache(id);
        if (value != null) {
            LOGGER.debug("Retrieved [%s] from cache [%s]", id, value.hashCode());
            return value;
        }

        // Get from the database
        value = index.get(id);
        if (value != null) {
            LOGGER.debug("Retrieved [%s] from database [%s]", id, value.hashCode());
            init(value);
            // Store in cache
            cache.put(getKey(value), new WeakReference<>(value));
        }
        return value;
    }

    private Value getFromCache(Key id) {
        // Try to get from cache first
        WeakReference<Value> reference = cache.get(id);
        if (reference != null) {
            Value value = reference.get();
            if (value != null)
                return value;
        }
        return null;
    }


    protected void cache(Value value) {
        cache.put(getKey(value), new WeakReference<>(value));
    }

    @Override
    public Iterator<Value> iterator() {

        try {
            return new AbstractIterator<Value>() {
                EntityCursor<Key> iterator = index.keys();

                @Override
                protected void finalize() throws Throwable {
                    super.finalize();
                    if (iterator != null)
                        iterator.close();
                }

                @Override
                protected boolean storeNext() {
                    try {
                        Key id = iterator.next();
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
                            LOGGER.error(e, "Could not close the cursor on values");
                        }

                    }

                    return false;
                }
            };
        } catch (DatabaseException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(Transaction txn, Key key) {
        index.delete(txn, key);
    }
}
