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

package sf.net.experimaestro.scheduler;

import com.sleepycat.persist.model.Persistent;
import sf.net.experimaestro.locks.LockType;

/**
 * What is the status of a dependency This class stores the previous status
 * (satisfied or not) in order to update the number of blocking resources
 */
@Persistent
public class Dependency {
	/**
	 * Type of lock that we request on the dependency 
	 */
	LockType type = null;
	
	/**
	 * Was this dependency satisfied when we last checked?
	 */
	boolean isSatisfied = false;

    /**
     * Previous state
     */
    ResourceState state;

	protected Dependency() {
	}

	public Dependency(LockType type, boolean isSatisfied, ResourceState state) {
		this.type = type;
		this.isSatisfied = isSatisfied;
        this.state = state;
    }

	public LockType getType() {
		return type;
	}

}