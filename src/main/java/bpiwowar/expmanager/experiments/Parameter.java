package bpiwowar.expmanager.experiments;

/**
 * The parameter of an experiment
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class Parameter {
	/**
	 * True if this is an option
	 */
	boolean option;

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
