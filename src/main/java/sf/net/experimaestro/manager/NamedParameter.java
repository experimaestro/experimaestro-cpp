package sf.net.experimaestro.manager;

import javax.xml.namespace.QName;

/**
 * A parameter definition
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class NamedParameter {
	/**
	 * Defines an optional parameter
	 */
	boolean optional;
	
	/**
	 * The type of the parameter
	 */
	String type;

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
	
	public String getType() {
		return type;
	}

	public NamedParameter(String type, boolean optional, String documentation) {
		this.type = type;
		this.optional = optional;
		this.documentation = documentation;
	}
	
	
}
