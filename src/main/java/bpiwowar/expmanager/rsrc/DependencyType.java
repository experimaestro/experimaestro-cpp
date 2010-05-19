package bpiwowar.expmanager.rsrc;

/**
 * Types of dependence of a task to a resource
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public enum DependencyType {
	/**
	 * Data should be generated and readable
	 */
	READ_ACCESS, 
	
	/**
	 * Data should be writable
	 */
	WRITE_ACCESS,
	
	/**
	 * Data should be generated
	 */
	GENERATED
}
