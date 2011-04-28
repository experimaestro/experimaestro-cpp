package sf.net.experimaestro.manager;

import java.util.ArrayList;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;

/**
 * A module groups tasks
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class Module {
	/**
	 * Module unique identifier
	 */
	protected QName id;

	/**
	 * The module human readable name
	 */
	protected String name;

	/**
	 * The documentation in XHTML
	 */
	protected Document documentation;

	/**
	 * Parent module
	 */
	Module parent;

	/**
	 * The list of submodules
	 */
	ArrayList<Module> submodules = new ArrayList<Module>();

	/**
	 * List of tasks
	 */
	ArrayList<TaskFactory> factories = new ArrayList<TaskFactory>();

	public Module() {
	}
	
	public Module(QName id) {
		this.id = id;
	}

	public Module getParent() {
		return parent;
	}

	public void setParent(Module parent) {
		this.parent = parent;
		parent.submodules.add(this);
	}

	public QName getId() {
		return id;
	}

	public ArrayList<Module> getSubmodules() {
		return submodules;
	}

	public void addSubmodule(Module module) {
		submodules.add(module);
	}

	public ArrayList<TaskFactory> getFactories() {
		return factories;
	}

	public void addFactory(TaskFactory factory) {
		factories.add(factory);
	}

	public void remove(TaskFactory oldFactory) {
		factories.remove(oldFactory);
	}
}
