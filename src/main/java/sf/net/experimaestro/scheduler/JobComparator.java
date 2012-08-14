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

import java.util.Comparator;

/**
 * Used to order tasks within the heap; compares first on blocking data, then on
 * priority, and eventually on timestamp
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class JobComparator implements Comparator<Job> {
	/**
	 * A public instance
	 */
	public static final Comparator<Job> INSTANCE = new JobComparator();

	/**
	 * Private since the public instance should be used in all cases
	 */
	private JobComparator() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	public int compare(Job a, Job b) {
		// Jobs are in a heap, where the smaller element is at the top
		// so smaller means run before, i.e.
		// returns < 0 if a should run before b
		// return == 0 if no difference
		// returns > 0 if b should run before a

		// First, if one is ready to start (dependencies are OK)
		if (a.nbUnsatisfied == 0 ^ b.nbUnsatisfied == 0)
			return a.nbUnsatisfied > b.nbUnsatisfied ? 1 : -1;

		// Then on locked status
		if (a.locked ^ b.locked)
			return a.locked ? 1 : -1;

		// Then check on priority
		int i = Integer.signum(b.priority - a.priority);
		if (i != 0)
			return i;

		// Then check on timestamp
		i = Long.signum(a.timestamp - b.timestamp);
		if (i != 0)
			return i;

		// Otherwise we use the pointer
		return Integer.signum(System.identityHashCode(a)
				- System.identityHashCode(b));
	}

}
