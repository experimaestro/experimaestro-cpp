package sf.net.experimaestro.scheduler;



/**
 * A job to be run
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class Job extends Task {
	public Job(TaskManager taskManager, String identifier) {
		super(taskManager, identifier);
	}

}
