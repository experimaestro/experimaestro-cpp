package sf.net.experimaestro.manager;

import java.util.Map;
import java.util.TreeMap;

import sf.net.experimaestro.utils.log.Logger;


/**
 * Repository for all possible tasks
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class TaskRepository {
	final static private Logger LOGGER = Logger.getLogger();
	
	/**
	 * The list of available experiments
	 */
	Map<String, TaskInformation> experiments = new TreeMap<String, TaskInformation>();
	
	/**
	 * @return
	 */
	public Iterable<TaskInformation> experiments() {
		return experiments.values();
	}

	/**
	 * Return information about an experiment
	 * 
	 * @param name
	 * @return
	 */
	public TaskInformation get(String name) {
		return experiments.get(name);
	}

	/**
	 * Register new experiment information
	 * 
	 * @param information
	 */
	public void register(TaskInformation information) {
		LOGGER.info("Registering experiment %s", information.id);
		experiments.put(information.id, information);
	}

}
