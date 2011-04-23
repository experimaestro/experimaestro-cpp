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
	Map<QName, TaskFactory> factories = new HashMap<QName, TaskFactory>();

	/**
	 * The list of of input types
	 */
	Map<QName, Type> types = new HashMap<QName, Type>();


	/**
	 * The list of of input types
	 */
	Map<QName, Module> modules = new HashMap<QName, Module>();
	
	/**
	 * @return
	 */
	public Iterable<TaskFactory> factories() {
		return factories.values();
	}

	/**
	 * Return information about an experiment
	 * 
	 * @param name
	 * @return
	 */
	public TaskFactory getFactory(QName name) {
		return factories.get(name);
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
		factories.put(information.id, information);
	}

	public void addType(Type type) {
		Type old = types.put(type.getId(), type);
		if (old != null)
			LOGGER.warn("Redefining type %s", type.getId());

	}
	
	public Map<QName, Module> getModules() {
		return modules;
	}
	
	public void addModule(Module module) {
		Module old = modules.put(module.getId(), module);
		if (old != null)
			LOGGER.warn("Redefining type %s", module.getId());

	}
}
