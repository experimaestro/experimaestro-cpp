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

import it.unimi.dsi.fastutil.ints.IntLinkedOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.apache.commons.lang.mutable.MutableInt;


/**
 * A set of locks indexed by integers
 */
public class IntLocks {
    private static IntSet locks = new IntLinkedOpenHashSet();

    private static MutableInt CURRENT_LOCK_ID = new MutableInt();

    public static int newLockID() {
        synchronized (CURRENT_LOCK_ID) {
            CURRENT_LOCK_ID.increment();
            final int lockID = CURRENT_LOCK_ID.intValue();
            synchronized (locks) {
                locks.add(lockID);
            }
            return lockID;
        }
    }

    public static void removeLock(int lockID) {
        synchronized (locks) {
            locks.remove(lockID);
            locks.notifyAll();
        }
    }

    public static void waitLockID(int lockID) {
        while (locks.contains(lockID)) {
            synchronized (locks) {
                try {
                    locks.wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }
}
