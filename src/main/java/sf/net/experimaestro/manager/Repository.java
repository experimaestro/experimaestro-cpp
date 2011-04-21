package sf.net.experimaestro.manager;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import sf.net.experimaestro.utils.log.Logger;

/**
 * Repository for all possible tasks
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class Repository {
	final static private Logger LOGGER = Logger.getLogger();

	/**
	 * The list of available task factories
	 */
	Map<QName, TaskFactory> taskFactories = new HashMap<QName, TaskFactory>();

	/**
	 * The list of of input types
	 */
	Map<QName, Type> types = new HashMap<QName, Type>();

	/**
	 * @return
	 */
	public Iterable<TaskFactory> tasks() {
		return taskFactories.values();
	}

	/**
	 * Return information about an experiment
	 * 
	 * @param name
	 * @return
	 */
	public TaskFactory getFactory(QName name) {
		return taskFactories.get(name);
	}

	public Type getType(QName name) {
		return types.get(name);
	}

	/**
	 * Register new experiment information
	 * 
	 * @param information
	 */
	public void register(TaskFactory information) {
		LOGGER.info("Registering experiment %s", information.id);
		taskFactories.put(information.id, information);
	}

	public void addType(Type type) {
		Type old = types.put(type.getQName(), type);
		if (old != null)
			LOGGER.warn("Redefining type %s", type.getQName());

	}
}
