package sf.net.experimaestro.manager;

import java.util.ArrayList;

import javax.xml.namespace.QName;

/**
 * A module groups tasks
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class Module {
	/**
	 * Module unique identifier
	 */
	QName id;

	/**
	 * Parent module 
	 */
	Module parent;
	
	/**
	 * The list of submodules
	 */
	ArrayList<Module> submodules;
	
	public Module getParent() {
		return parent;
	}

	public void setParent(Module parent) {
		this.parent = parent;
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
}
