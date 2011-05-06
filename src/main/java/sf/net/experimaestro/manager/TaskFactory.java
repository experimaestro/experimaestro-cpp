package sf.net.experimaestro.manager;

import static java.lang.String.format;

import java.util.Map;


/**
 * Information about an experiment
 * 
 * @author B. Piwowarski
 */
public abstract class TaskFactory {
	/**
	 * The identifier of this experiment
	 */
	protected QName id;

	/**
	 * The version
	 */
	String version;

	/**
	 * The group
	 */
	String group;

	/**
	 * The repository
	 */
	private final Repository repository;

	/**
	 * The module
	 */
	Module module;

	/**
	 * Initialise a task
	 * 
	 * @param repository
	 * 
	 * @param id
	 *            The id of the task
	 * @param version
	 * @param group
	 */
	public TaskFactory(Repository repository, QName id, String version,
			String group) {
		this.repository = repository;
		this.id = id;
		this.version = version;
		this.group = group;
	}

	public Repository getRepository() {
		return repository;
	}

	/**
	 * Documentation in XHTML format
	 */
	public String getDocumentation() {
		return format("<p>No documentation found for experiment %s</p>", id);
	}

	/**
	 * Get the list of (potential) parameters
	 * 
	 * @return a map of mappings from a qualified name to a named parameter or
	 *         null if non existent
	 */
	abstract public Map<String, Input> getInputs();

	/**
	 * Get the list of (potential) parameters
	 * 
	 * @return a map of mappings from a qualified name to a named parameter or
	 *         null if non existent
	 */
	abstract public Map<String, QName> getOutputs();

	/**
	 * Creates a new experiment
	 */
	public abstract Task create();

	/**
	 * Returns the qualified name for this task
	 */
	public QName getId() {
		return id;
	}

	public Object getVersion() {
		return version;
	}

	/**
	 * Finish the initialisation of the factory
	 */
	protected void init() {

	}

	public Module getModule() {
		return module;
	}

	public void setModule(Module module) {
		if (this.module == module)
			return;
		if (this.module != null)
			this.module.remove(this);
		this.module = module;
		module.addFactory(this);
	}
}
