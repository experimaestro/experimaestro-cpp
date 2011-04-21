package sf.net.experimaestro.manager;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import sf.net.experimaestro.exceptions.ExperimaestroException;
import sf.net.experimaestro.utils.XMLUtils;
import sf.net.experimaestro.utils.log.Logger;

/**
 * A configuration triggered by a value
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class AlternativeTask extends Task {
	final static private Logger LOGGER = Logger.getLogger();

	/**
	 * The real task
	 */
	private Task task;

	/**
	 * Our factory
	 */
	private final AlternativeType type;

	/**
	 * Creates an alternative task object
	 * 
	 * @param information
	 */
	protected AlternativeTask(AlternativeType type) {
		// no factory
		super(null);
		this.type = type;
	}

	@Override
	public boolean setParameter(DotName id, Element value, boolean direct) {
		if (id.size() == 0) {
			String key = value
					.getAttributeNS(Manager.EXPERIMAESTRO_NS, "value");
			QName qname = XMLUtils.parseQName(key, value,
					Manager.PREDEFINED_PREFIXES);
			TaskFactory subFactory = type.factories.get(qname);
			if (subFactory == null)
				throw new ExperimaestroException(
						"Could not find an alternative with name [%s]", key);
			task = subFactory.create();
			LOGGER.info("Created the task for alternative [%s]", key);
			return true;
		}

		// Otherwise, set the parameter of the task
		LOGGER.info("Setting parameter [%s] for subtask", id);
		return task.setParameter(id, value);
	}

	@Override
	public Document doRun() {
		if (task == null)
			throw new ExperimaestroException(
					"Alternative task has not been set");
		LOGGER.info("Running the alternative task [%s]",
				task.factory != null ? "n/a" : task.factory.id);
		return task.run();
	}

}
