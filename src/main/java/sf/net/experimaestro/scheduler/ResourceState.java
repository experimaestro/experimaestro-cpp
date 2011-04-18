package sf.net.experimaestro.scheduler;

/**
 * The resource state
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public enum ResourceState {
	/**
	 * Waiting state (jobs)
	 */
	WAITING,
	
	/**
	 * The job is currently running
	 */
	RUNNING, 
	
	/**
	 * The job is on hold
	 */
	ON_HOLD,
	
	/**
	 * The job ran but did not complete
	 */
	ERROR,
	
	/**
	 * Completed (for a job) or generated (for a data resource) 
	 */
	DONE;
}
