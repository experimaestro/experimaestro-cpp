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

/**
 * Possible lock types on a resource
 * 
 * <p>
 * {@link #READ_ACCESS} and {@link #GENERATED} imply that the resource should be
 * generated before use
 * </p>
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public enum LockType {
	/**
	 * Asks for a read access
	 */
	READ_ACCESS,

	/**
	 * Waits for a read/write access
	 */
	WRITE_ACCESS,

	/**
	 * Waits for an exclusive access
	 */
	EXCLUSIVE_ACCESS,

	/**
	 * Just asks that the data be generated
	 */
	GENERATED,
}
