package sf.net.experimaestro.manager;

import java.util.Map;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import sf.net.experimaestro.exceptions.ExperimaestroException;
import sf.net.experimaestro.utils.XMLUtils;
import sf.net.experimaestro.utils.log.Logger;

public class AlternativeInput extends Input {
	final static private Logger LOGGER = Logger.getLogger();
	
	private AlternativeType alternativeType;

	public AlternativeInput(QName type, boolean optional, String documentation,
			AlternativeType alternativeType) {
		super(type, optional, documentation);
		this.alternativeType = alternativeType;
	}

	@Override
	Value newValue() {
		return new AlternativeValue(alternativeType);
	}

	public class AlternativeValue extends Value {

		/**
		 * The real task
		 */
		private Task task;

		/**
		 * The returned value
		 */
		private Document value = null;

		/**
		 * Creates an alternative task object
		 * 
		 * @param information
		 */
		protected AlternativeValue(AlternativeType type) {
			// no factory
			super(AlternativeInput.this);
		}

		@Override
		public void set(DotName id, Document value) {
			if (id.size() == 0) {
				final Map<QName, TaskFactory> factories = AlternativeInput.this.alternativeType.factories;
				
				final Element element = value.getDocumentElement();
				String key = element.getAttributeNS(Manager.EXPERIMAESTRO_NS,
						"value");
				QName qname = XMLUtils.parseQName(key, element,
						Manager.PREDEFINED_PREFIXES);
				TaskFactory subFactory = factories.get(qname);
				if (subFactory == null)
					throw new ExperimaestroException(
							"Could not find an alternative with name [%s]", key);
				LOGGER.info("Creating a task [%s]", subFactory.id);
				task = subFactory.create();
				LOGGER.info("Created the task for alternative [%s]", key);
			} else {
				task.setParameter(id, value);
			}
		}

		@Override
		public void process() {
			if (task == null)
				throw new ExperimaestroException(
						"Alternative task has not been set");
			LOGGER.info("Running the alternative task [%s]",
					task.factory != null ? "n/a" : task.factory.id);
			value = task.run();
		}

		@Override
		public Document get() {
			return value;
		}

	}

}
