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
	QName type;
	
	/**
	 * The name of this parameter
	 */
	String name;
	
	/**
	 * Documentation for this parameter
	 */
	String documentation;
}
