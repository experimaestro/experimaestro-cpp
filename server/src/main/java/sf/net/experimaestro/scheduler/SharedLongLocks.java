package sf.net.experimaestro.scheduler;

/*
 * This file is part of experimaestro.
 * Copyright (c) 2014 B. Piwowarski <benjamin@bpiwowar.net>
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

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

import java.util.function.Function;

/**
 * Sets of locks indexed by long integer IDs
 */
final public class SharedLongLocks {
    /**
     * The list of locks
     * A negative value is used for shared locks
     */
    private Long2IntOpenHashMap locks = new Long2IntOpenHashMap();

    /**
     * A lock (shared or exclusive)
     */
    private class Lock implements EntityLock {
        private long id;
        boolean shared;

        public Lock(long id, boolean shared) {
            this.id = id;
            this.shared = shared;
        }

        @Override
        public boolean isShared() {
            assert id >= 0;

            return shared;
        }

        @Override
        public void makeExclusive(long timeout) {
            assert id >= 0;

            if (shared) {
                synchronized (locks) {
                    int value = locks.get(id);
                    if (value == 1) {
                        locks.put(id, -1);
                        shared = false;
                    } else {
                        throw new RuntimeException("Cannot escalade to shared: other have taken the lock");
                    }
                }

            }
        }

        @Override
        public void close() {
            if (id >= 0) {
                synchronized (locks) {
                    if (shared) {
                        final int value = locks.get(id);
                        assert value > 0;
                        locks.put(id, value - 1);
                        if (value == 1) {
                            locks.notifyAll();
                        }
                    } else {
                        final int remove = locks.remove(id);
                        assert remove < 0;
                        locks.notifyAll();
                    }
                }
            }
            id = -1;
        }

        @Override
        public boolean isClosed() {
            return id < 0;
        }
    }


    public EntityLock lock(long id, boolean exclusive, long timeout) {
        if (exclusive) {
            return exclusiveLock(id, timeout);
        }
        return sharedLock(id, timeout);
    }


    protected Lock getLock(long id, long timeout, Function<Integer, Boolean> shouldWait, Function<Integer, Integer> computeValue, boolean shared) {
        synchronized (locks) {
            int value;
            final long startTime = System.currentTimeMillis();

            while (shouldWait.apply(value = locks.get(id))) {

                if (timeout > 0 && System.currentTimeMillis() - startTime >= timeout) {
                    return null;
                }

                try {
                    locks.wait(timeout);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            // Add us
            locks.put(id, computeValue.apply(value).intValue());
        }
        return new Lock(id, shared);
    }


    /**
     * Acquire a shared lock
     *
     * @param id      The ID of the resource
     * @param timeout An optional timeout, or 0 if no timeout
     * @return A lock, or null if timeout is greater than 0 and the lock could not be acquired
     */
    public Lock sharedLock(long id, long timeout) {
        return getLock(id, timeout, x -> x < 0, x -> x + 1, true);
    }

    /**
     * Acquire an exclusive lock
     *
     * @param id      The ID of the resource
     * @param timeout An optional timeout, or 0 if no timeout
     * @return A lock, or null if timeout is greater than 0 and the lock could not be acquired
     */
    public Lock exclusiveLock(long id, long timeout) {
        return getLock(id, timeout, x -> x != 0, x -> -1, true);
    }
}
