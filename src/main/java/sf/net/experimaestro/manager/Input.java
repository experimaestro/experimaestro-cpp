package sf.net.experimaestro.manager;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import sf.net.experimaestro.exceptions.ExperimaestroException;
import sf.net.experimaestro.utils.log.Logger;

/**
 * A parameter definition in a task factory / task
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public abstract class Input {
	final static private Logger LOGGER = Logger.getLogger();
	
	/**
	 * Defines an optional parameter
	 */
	boolean optional;

	/**
	 * The type of the parameter
	 */
	QName type;

	/**
	 * Documentation for this parameter
	 */
	String documentation;
	
	/**
	 * Default value
	 */
	Document defaultValue;

	/**
	 * Returns whether the input is optional or not
	 * @return
	 */
	public boolean isOptional() {
		return optional;
	}

	/**
	 * Get the documentation
	 * @return A string in XHTML 
	 */
	public String getDocumentation() {
		return documentation;
	}

	public QName getType() {
		return type;
	}

	/**
	 * New input type
	 * 
	 * @param type
	 * @param optional
	 * @param documentation
	 */
	public Input(QName type) {
		this.type = type;
	}

	
	public void setOptional(boolean optional) {
		this.optional = optional;
	}
	
	public void setDocumentation(String documentation) {
		this.documentation = documentation;
	}
	
	abstract Value newValue();
	
	public void printHTML(PrintWriter out) {
		out.println(documentation);
	}

	public void setDefaultValue(Document defaultValue) {
		this.defaultValue = defaultValue;
	}
	
	/**
	 * Defines a connection to the
	 * 
	 * @author B. Piwowarski <benjamin@bpiwowar.net>
	 */
	static public class Connection implements NamespaceContext {
		final String path;
		final DotName to;
		private Element element;

		public Connection(String path, DotName to, Element element) {
			this.path = path;
			this.to = to;
			// FIXME: would be good to take the map and leave the element
			this.element = element;
		}

		@Override
		public String getNamespaceURI(String prefix) {
			String uri = element.lookupNamespaceURI(prefix);
			if (uri == null)
				uri = Manager.PREDEFINED_PREFIXES.get(prefix);
			if (uri == null)
				throw new ExperimaestroException("Prefix %s not bound", prefix);
			LOGGER.info("Prefix %s maps to %s", prefix, uri);
			return uri;
		}

		@Override
		public String getPrefix(String arg0) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Iterator<?> getPrefixes(String arg0) {
			throw new UnsupportedOperationException();
		}

	}

	ArrayList<Connection> connections = new ArrayList<Input.Connection>();

	public void addConnection(String path, DotName to, Element element) {
		connections.add(new Connection(path, to, element));
	}

}
