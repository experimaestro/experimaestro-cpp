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

package sf.net.experimaestro.locks;

import com.sleepycat.je.DatabaseException;
import sf.net.experimaestro.scheduler.Scheduler;

/**
 * A lock that can be removed.
 * 
 * The lock is taken during the object construction which is dependent on the
 * actual {@link Lock} implementation.
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public interface Lock {
	/**
	 * Dispose of the resource - returns true if the resource was properly
	 * unlocked
	 */
	boolean dispose();

	/**
	 * Change ownership
	 *
     * @param pid
     *            The new owner PID
     */
	void changeOwnership(String pid);

    /** Initialize the lock when restored from database */
    void init(Scheduler scheduler) throws DatabaseException;
}
