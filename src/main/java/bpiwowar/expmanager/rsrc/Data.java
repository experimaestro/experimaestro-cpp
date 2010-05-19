package bpiwowar.expmanager.rsrc;


/**
 * Represents some data that can be produced by a given job
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public abstract class Data extends Resource {

	public Data(TaskManager taskManager, String identifier) {
		super(taskManager, identifier);
	}

	/**
	 * The job that can or has generated this data (if any)
	 */
	transient Job generatingJob = null;
	
	

}
