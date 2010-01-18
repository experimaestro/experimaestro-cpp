package bpiwowar.expmanager;

import java.util.Set;

import bpiwowar.utils.GenericHelper;
import bpiwowar.utils.HeapElement;
import bpiwowar.utils.Pair;

/**
 * A Job to be run
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public abstract class Job implements HeapElement<Job> {
	/**
	 * The index within the heap (used for updates)
	 */
	private int index;

	/**
	 * The priority of the job (the higher, the more urgent)
	 */
	private int priority;

	/**
	 * The dependencies for this job
	 */
	Set<Data> dependencies = GenericHelper.newTreeSet();

	/**
	 * Counts the number of dependencies, and within these dependencies the
	 * number of those which are resolved
	 * 
	 * @return A pair of integer, the first is the number of resolved
	 *         dependencies, the second the total number of dependencies
	 */
	public Pair<Integer, Integer> getDependencyCount() {
		int n = 0;
		for(Data data: dependencies)
			if (data.isProduced())
				n++;
		return Pair.create(n, dependencies.size());
	}

	public int compareTo(Job o) {
		// Jobs are in a heap, where the smaller element is at the top
		// so smaller means run before
		

		int i = Integer.signum(o.priority);
		return i;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	/**
	 * @param priority
	 *            the priority to set
	 */
	public void setPriority(int priority) {
		this.priority = priority;
	}

	/**
	 * @return the priority
	 */
	public int getPriority() {
		return priority;
	}

}
