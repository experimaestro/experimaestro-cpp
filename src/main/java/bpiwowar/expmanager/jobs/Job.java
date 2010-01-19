package bpiwowar.expmanager.jobs;

import java.util.Set;

import bpiwowar.expmanager.Data;
import bpiwowar.utils.GenericHelper;
import bpiwowar.utils.HeapElement;
import bpiwowar.utils.Pair;

/**
 * A job to be run
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public abstract class Job implements HeapElement<Job> {
	/**
	 * The index within the heap (used for updates)
	 */
	private int index;

	/**
	 * When was the job submitted (in case the priority is not enough)
	 */
	private long timestamp = System.currentTimeMillis();

	/**
	 * The priority of the job (the higher, the more urgent)
	 */
	private int priority;

	/**
	 * The dependencies for this job (dependencies are on data)
	 */
	private Set<Data> dependencies = GenericHelper.newTreeSet();

	/**
	 * Number of unsatisfied dependencies
	 */
	int nbBlockingData;

	/**
	 * Add a dependency
	 * @param data The data we depend upon
	 */
	void addDependency(Data data) {
		data.register(this);
	}

	@Override
	protected void finalize() throws Throwable {
		for (Data data : dependencies)
			data.unregister(this);
	}
	
	

	public int compareTo(Job o) {
		// Jobs are in a heap, where the smaller element is at the top
		// so smaller means run before

		// First, if one is ready to start (dependencies are OK)
		if (nbBlockingData == 0 ^ o.nbBlockingData == 0)
			return Integer.signum(o.nbBlockingData - nbBlockingData);

		// Then check on priority
		int i = Integer.signum(priority - o.priority);
		if (i != 0)
			return i;

		// Then check on timestamp
		i = Long.signum(timestamp - o.timestamp);
		if (i != 0)
			return i;

		// Otherwise we use the pointer
		return Integer.signum(System.identityHashCode(this)
				- System.identityHashCode(o));
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
