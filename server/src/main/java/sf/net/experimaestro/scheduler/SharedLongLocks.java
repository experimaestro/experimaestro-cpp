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
        private final long id;
        boolean shared;

        public Lock(long id, boolean exclusive) {
            this.id = id;
            this.shared = exclusive;
        }

        @Override
        public boolean isShared() {
            return shared;
        }

        @Override
        public void makeExclusive()  {
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
    }


    public EntityLock lock(long id, boolean exclusive) {
        if (exclusive) {
            return exclusiveLock(id);
        }
        return sharedLock(id);
    }

    public Lock sharedLock(long id) {
        synchronized (locks) {
            int value;
            while ((value = locks.get(id)) < 0) {
                try {
                    locks.wait();
                } catch (InterruptedException e) {
                }
            }

            // Add us
            locks.put(id, value + 1);
        }
        return new Lock(id, true);
    }

    public Lock exclusiveLock(long id) {
        synchronized (locks) {
            while (locks.get(id) != 0) {
                try {
                    locks.wait();
                } catch (InterruptedException e) {
                }
            }

            // Add us
            locks.put(id, -1);
        }
        return new Lock(id, false);
    }
}
