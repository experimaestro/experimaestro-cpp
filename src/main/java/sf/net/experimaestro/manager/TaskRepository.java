package sf.net.experimaestro.manager;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import sf.net.experimaestro.log.Logger;


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
	Map<QName, TaskFactory> experiments = new HashMap<QName, TaskFactory>();
	
	/**
	 * @return
	 */
	public Iterable<TaskFactory> experiments() {
		return experiments.values();
	}

	/**
	 * Return information about an experiment
	 * 
	 * @param name
	 * @return
	 */
	public TaskFactory get(QName name) {
		return experiments.get(name);
	}

	/**
	 * Register new experiment information
	 * 
	 * @param information
	 */
	public void register(TaskFactory information) {
		LOGGER.info("Registering experiment %s", information.id);
		experiments.put(information.id, information);
	}

}
