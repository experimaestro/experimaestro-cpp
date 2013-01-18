/*
 * This file is part of experimaestro.
 * Copyright (c) 2012 B. Piwowarski <benjamin@bpiwowar.net>
 *
 * experimaestro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * experimaestro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 */

package sf.net.experimaestro.manager;

import org.apache.xerces.xs.XSModel;
import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

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
	ArrayList<Module> submodules = new ArrayList<>();

	/**
	 * List of tasks
	 */
	ArrayList<TaskFactory> factories = new ArrayList<>();

	/**
	 * List of XML Schemas
	 */
	ArrayList<XSModel> xsModels = new ArrayList<>();

    /**
     * Map of namespaces to prefixes
     */
    Map<String, String> prefixes = new TreeMap<>();
	
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

    public ArrayList<XSModel> getSchemas() {
        return xsModels;
    }

    public void setPrefix(String prefix, String url) {
        prefixes.put(url, prefix);
    }

    public String getPrefix(String url) {
        final String prefix = prefixes.get(url);
        if (parent == null || prefix != null)
            return prefix;

        return parent.getPrefix(url);
    }

    public void setDocumentation(Document documentation) {
        this.documentation = documentation;
    }

    public void setName(String name) {
        this.name = name;
    }
}
