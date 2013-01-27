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

import com.google.common.collect.AbstractIterator;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.PrimaryIndex;
import sf.net.experimaestro.exceptions.ExperimaestroCannotOverwrite;
import sf.net.experimaestro.utils.CloseableIterable;
import sf.net.experimaestro.utils.log.Logger;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.WeakHashMap;

import static sf.net.experimaestro.scheduler.XPMTransaction.transaction;


/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
abstract public class CachedEntitiesStore<Key, Value> {
    final static private Logger LOGGER = Logger.getLogger();

    /**
     * The index
     */
    protected PrimaryIndex<Key, Value> index;

    /**
     * A cache to get track of values in memory
     * FIXME: This weak hash map is not really weak!
     */
    private WeakHashMap<Key, WeakReference<Value>> cache = new WeakHashMap<>();


    /**
     * Returns whether the value can be overriden
     *
     * @param old   The previous value (guaranteed to be not null)
     * @param value The value
     * @return True or false
     */
    protected abstract boolean canOverride(Value old, Value value);

    /**
     * Returns the key of the entity. This copy should be pointed
     * to by the object
     */
    protected abstract Key getKey(Value value);

    /**
     * Init a given value
     *
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
     * @return The old value (if existing)
     * @throws com.sleepycat.je.DatabaseException
     *                                      If an error occurs while putting the value in the database. In this case,
     *                                      the cache is not updated (which is OK since the cache is really updated
     *                                      when overwritting the resource)
     * @throws ExperimaestroCannotOverwrite If the resource cannot be overwriten
     */
    synchronized public Value put(XPMTransaction txn, final Value value) throws DatabaseException, ExperimaestroCannotOverwrite {
        // Check if overriding a running value (unless it is the same object)
        final Key key = getKey(value);

        // Retrieve an old (and different) value
        final Value _old = get(key);
        final Value old = _old == null || _old != value ? null : _old;


        if (old != null && !canOverride(old, value))
            throw new ExperimaestroCannotOverwrite("Resource %s", key);

        // Store in database...
        index.put(txn != null ? txn.getTransaction() : null, value);

        // and then in cache
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                // Clean the old task
                if (old != value && old != null && value instanceof Cleaneable) {
                    ((Cleaneable) old).clean();
                }
                cache.put(key, new WeakReference<>(value));
                LOGGER.info("Stored value [%s@%s] in database", System.identityHashCode(value), key);
            }
        };

        if (txn != null)
            txn.addCommitAction(runnable);
        else
            runnable.run();

        // OK, we did updateFromStatusFile
        return old;
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
        synchronized (cache) {
            // Try to get from cache first
            WeakReference<Value> reference = cache.get(id);
            if (reference != null) {
                Value value = reference.get();
                if (value != null)
                    return value;
            }
        }
        return null;
    }


    public CloseableIterable<Value> values() {
        return wrap(index.entities());
    }


    public void delete(XPMTransaction txn, Key key) {
        index.delete(transaction(txn), key);
        cache.remove(key);
    }

    /**
     * Retrieve a corresponding Value in the database, and returns it. Otherwise,
     * put the Value in the cache and return it.
     *
     * @param value
     * @return
     */
    protected Value cached(Value value) {
        synchronized (cache) {
            final Key key = getKey(value);
            final Value fromCache = getFromCache(key);
            if (fromCache != null)
                return fromCache;

            cache.put(key, new WeakReference<>(value));
            return value;
        }
    }

    /**
     * Wraps an iterable over values
     */
    protected class IterableWrapper implements CloseableIterable<Value> {
        EntityCursor<Value> entities;

        public IterableWrapper(EntityCursor<Value> v) {
            entities = v;
        }

        @Override
        public void close() {
            entities.close();
        }

        @Override
        public Iterator<Value> iterator() {
            return new AbstractIterator<Value>() {
                final Iterator<Value> iterator = entities.iterator();

                @Override
                protected Value computeNext() {
                    if (!iterator.hasNext())
                        return endOfData();

                    return cached(iterator.next());
                }
            };
        }
    }

    protected CloseableIterable<Value> wrap(EntityCursor<Value> cursor) {
        return new IterableWrapper(cursor);
    }


}
