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
	private Map<QName, TaskFactory> factories = new HashMap<QName, TaskFactory>();

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
	 * @param factory
	 */
	public void addFactory(TaskFactory factory) {
		LOGGER.info("Registering experiment %s", factory.id);
		TaskFactory oldFactory = factories.put(factory.id, factory);
		if (oldFactory != null) {
			LOGGER.info("Redefined old factory");
			oldFactory.getModule().remove(oldFactory);
		}

		Module module = factory.getModule();
		if (module == null)
			factory.setModule(mainModule);
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

		// Add to the main module if no parent
		if (module.getParent() == null)
			module.setParent(mainModule);
	}

	Module mainModule = new Module(new QName(Manager.EXPERIMAESTRO_NS, "main"));

	public Module getModule(QName qName) {
		if (qName == null)
			return mainModule;
		return modules.get(qName);
	}

	public Module getMainModule() {
		return mainModule;
	}

}
