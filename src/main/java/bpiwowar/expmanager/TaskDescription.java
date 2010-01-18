package bpiwowar.expmanager;

import java.util.ArrayList;

/**
 * A tool description
 * 
 * <pre>
 * </pre>
 * 
 * @author Benjamin Piwowarski <benjamin@bpiwowar.net>
 */
public abstract class TaskDescription {
	/**
	 * The fully qualified id of the tool
	 */
	String id;

	/**
	 * A short name for display
	 */
	String name;

	/**
	 * The list of outputs
	 */
	ArrayList<Object> outputs;
	

}
