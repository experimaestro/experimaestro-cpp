package sf.net.experimaestro.manager;

import javax.xml.namespace.QName;

/**
 * A parameter definition
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class Input {
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

	public boolean isOptional() {
		return optional;
	}

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
	public Input(QName type, boolean optional, String documentation) {
		this.type = type;
		this.optional = optional;
		this.documentation = documentation;
	}

}
