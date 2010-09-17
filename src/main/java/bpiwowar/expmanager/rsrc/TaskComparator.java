package bpiwowar.expmanager.rsrc;

import java.util.Comparator;

/**
 * Used to order tasks within the heap; compares first on blocking data, then on
 * priority, and eventually on timestamp
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class TaskComparator implements Comparator<Task> {
	/**
	 * A public instance
	 */
	public static final Comparator<Task> INSTANCE = new TaskComparator();

	/**
	 * Private since the public instance should be used in all cases
	 */
	private TaskComparator() {
	}

	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	public int compare(Task a, Task o) {
		// Jobs are in a heap, where the smaller element is at the top
		// so smaller means run before

		// First, if one is ready to start (dependencies are OK)
		if (a.nbUnsatisfied == 0 ^ o.nbUnsatisfied == 0)
			return Integer.signum(o.nbUnsatisfied - a.nbUnsatisfied);

		// Then check on priority
		int i = Integer.signum(a.priority - o.priority);
		if (i != 0)
			return i;

		// Then check on timestamp
		i = Long.signum(a.timestamp - o.timestamp);
		if (i != 0)
			return i;

		// Otherwise we use the pointer
		return Integer.signum(System.identityHashCode(a)
				- System.identityHashCode(o));
	}

}
