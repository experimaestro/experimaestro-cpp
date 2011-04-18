package sf.net.experimaestro.scheduler;

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	public int compare(Task a, Task b) {
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
