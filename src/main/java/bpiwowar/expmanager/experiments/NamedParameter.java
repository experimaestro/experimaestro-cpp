package bpiwowar.expmanager.experiments;

/**
 * A type
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
	Type type;
	
	/**
	 * The name of this parameter
	 */
	String name;
	
	/**
	 * Documentation for this parameter
	 */
	String documentation;
}
