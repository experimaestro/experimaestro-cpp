package sf.net.experimaestro.scheduler;

import com.sleepycat.persist.model.Entity;

/**
 * Represents some data that can be produced by a given job
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Entity
public abstract class Data extends Resource {

	public Data(Scheduler taskManager, String identifier, LockMode mode) {
		super(taskManager, identifier, mode);
	}

	/**
	 * The job that can or has generated this data (if any)
	 */
	transient Job generatingJob = null;

}
