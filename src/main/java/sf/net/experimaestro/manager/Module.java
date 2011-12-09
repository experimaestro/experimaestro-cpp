/*
 *
 *  This file is part of experimaestro.
 *  Copyright (c) 2011 B. Piwowarski <benjamin@bpiwowar.net>
 *
 *  experimaestro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  experimaestro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package sf.net.experimaestro.manager;

import java.util.ArrayList;

import org.w3c.dom.Document;

import com.sun.org.apache.xerces.internal.xs.XSModel;

/**
 * A module groups tasks
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class Module implements Comparable<Module> {
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

	/**
	 * List of XML Schemas
	 */
	ArrayList<XSModel> xsModels = new ArrayList<XSModel>();
	
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

	@Override
	public int compareTo(Module other) {
		return id.compareTo(other.id);
	}

	public void addSchema(XSModel xsModel) {
		xsModels.add(xsModel);
	}
}
