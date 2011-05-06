package sf.net.experimaestro.manager;

import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import sf.net.experimaestro.exceptions.ExperimaestroException;
import sf.net.experimaestro.utils.XMLUtils;

/**
 *
 * Handles alternatives
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class AlternativeValue extends Value {

	/**
	 * 
	 */
	private  AlternativeInput alternativeInput;

	/**
	 * The real task
	 */
	private Task task;

	/**
	 * The returned value
	 */
	private Document value = null;

	
	public AlternativeValue() {
	}
	
	@Override
	protected void init(Value _other) {
		AlternativeValue other = (AlternativeValue)_other;
		super.init(other);
		
		alternativeInput = other.alternativeInput;
		// Copy the task if it has been set
		if (task != null)
			task = task.copy();
	}
	
	/**
	 * Creates an alternative task object
	 * @param alternativeInput TODO
	 * 
	 * @param information
	 */
	protected AlternativeValue(AlternativeInput alternativeInput, AlternativeType type) {
		super(alternativeInput);
		this.alternativeInput = alternativeInput;
	}

	@Override
	public void set(DotName id, Document value) {
		if (id.size() == 0) {
			final Map<QName, TaskFactory> factories = this.alternativeInput.alternativeType.factories;
			
			final Element element = value.getDocumentElement();
			String key = element.getAttributeNS(Manager.EXPERIMAESTRO_NS,
					"value");
			QName qname = XMLUtils.parseQName(key, element,
					Manager.PREDEFINED_PREFIXES);
			TaskFactory subFactory = factories.get(qname);
			if (subFactory == null)
				throw new ExperimaestroException(
						"Could not find an alternative with name [%s]", key);
			AlternativeInput.LOGGER.info("Creating a task [%s]", subFactory.id);
			task = subFactory.create();
			AlternativeInput.LOGGER.info("Created the task for alternative [%s]", key);
		} else {
			task.setParameter(id, value);
		}
	}

	@Override
	public void process() {
		// If the task has not been set, try to use default value
		if (task == null && input.defaultValue != null)
			set(DotName.EMPTY, input.defaultValue);
		
		if (task == null)
			throw new ExperimaestroException(
					"Alternative task has not been set");
		AlternativeInput.LOGGER.info("Running the alternative task [%s]",
				task.factory != null ? "n/a" : task.factory.id);
		value = task.run();
	}

	@Override
	public Document get() {
		return value;
	}

}