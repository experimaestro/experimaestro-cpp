package sf.net.experimaestro.manager;

import org.w3c.dom.Document;

import sf.net.experimaestro.exceptions.ExperimaestroException;
import sf.net.experimaestro.utils.XMLUtils;
import sf.net.experimaestro.utils.log.Logger;

/**
 *
 * A simple XML value
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class XMLValue extends Value {
	final static private Logger LOGGER = Logger.getLogger();
	
	private Document value;

	public XMLValue(Input input) {
		super(input);
	}
	
	@Override
	public void process() {
		// If there is no value, takes the default
		if (value == null && input.defaultValue != null) {
			LOGGER.info("Setting default value [%s]", XMLUtils.toStringObject(input.defaultValue));
			value = (Document) input.defaultValue.cloneNode(true);
		}
	}

	
	@Override
	public void set(DotName id, Document value) {
		if (id.size() != 0)
			throw new ExperimaestroException("Cannot handle qualified names [%s]");
		LOGGER.info("Value set to [%s]", XMLUtils.toString(value));
		this.value = value;
	}

	@Override
	public Document get() {
		return value;
	}

}
