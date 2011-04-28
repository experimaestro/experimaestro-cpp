package sf.net.experimaestro.manager;

import java.io.PrintWriter;
import java.util.ArrayList;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import sf.net.experimaestro.utils.log.Logger;

/**
 * A parameter definition in a task factory / task
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public abstract class Input {
	final static Logger LOGGER = Logger.getLogger();

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
	 * Unnamed option
	 */
	boolean unnamed;

	/**
	 * Returns whether the input is optional or not
	 * 
	 * @return
	 */
	public boolean isOptional() {
		return optional;
	}

	/**
	 * Get the documentation
	 * 
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
	static public class Connection {
		final String path;
		final DotName to;
		private NSContext context;

		public Connection(String path, DotName to, Element element) {
			this.path = path;
			this.to = to;
			this.context = new NSContext(element);
		}

		public NSContext getContext() {
			return context;
		}

	}

	ArrayList<Connection> connections = new ArrayList<Input.Connection>();

	public void addConnection(String path, DotName to, Element element) {
		connections.add(new Connection(path, to, element));
	}

	public boolean isUnnamed() {
		return unnamed;
	}

	public void setUnnamed(boolean unnamed) {
		this.unnamed = unnamed;
	}

}
