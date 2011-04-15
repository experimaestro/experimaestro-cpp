package sf.net.experimaestro.manager;

import static java.lang.String.format;

import java.util.Map;

/**
 * Information about an experiment
 * 
 * @author B. Piwowarski
 */
public abstract class TaskInformation {
	/**
	 * The identifier of this experiment
	 */
	String id;

	/**
	 * The version
	 */
	String version;

	/**
	 * The group
	 */
	String group;

	public TaskInformation(String id, String version, String group) {
		this.id = id;
		this.version = version;
		this.group = group;
	}

	/**
	 * Creates a new experiment
	 */
	abstract Task create();

	/**
	 * Documentation in XHTML format
	 */
	String getDocumentation() {
		return format("<p>No documentation found for experiment %s</p>", id);
	}

	/**
	 * Get the list of (potential) parameters
	 * 
	 * @return a map of mappings from a qualified name to a named parameter or
	 *         null if non existent
	 */
	abstract public Map<QName, NamedParameter> getParameters();

}
