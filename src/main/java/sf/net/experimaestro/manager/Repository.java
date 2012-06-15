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
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import sf.net.experimaestro.scheduler.Locator;
import sf.net.experimaestro.utils.log.Logger;

import com.sun.org.apache.xerces.internal.impl.xs.XSElementDecl;
import com.sun.org.apache.xerces.internal.xs.XSConstants;
import com.sun.org.apache.xerces.internal.xs.XSModel;
import com.sun.org.apache.xerces.internal.xs.XSNamedMap;

/**
 * Repository for all possible tasks
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class Repository extends AbstractRepository {
	final static private Logger LOGGER = Logger.getLogger();

    /**
     * Dependencies
     */
    ArrayList<Locator> sources = new ArrayList<Locator>();

	/**
	 * The list of available task factories
	 */
	private Map<QName, TaskFactory> factories = new TreeMap<QName, TaskFactory>();

	/**
	 * The list of of input types
	 */
	Map<QName, Type> types = new HashMap<QName, Type>();

	/**
	 * The list of of input types
	 */
	Map<QName, Module> modules = new HashMap<QName, Module>();
	
	/**
	 * List of XML types
	 */
	Map<QName, XSElementDecl> xmlElements = new TreeMap<QName, XSElementDecl>();

    /**
     * The experimaestro default module
     */
    Module defaultModule = new Module(new QName(Manager.EXPERIMAESTRO_NS, "main"));

    public Repository(Locator identifier) {
        super(identifier);
    }

    /**
	 * @return
	 */
	public Iterable<TaskFactory> factories() {
		return factories.values();
	}


    /**
     * Returns a task factory
     *
     * @param name The qualified name of the factory
     * @return A TaskFactory object or null if not found
     */
    @Override
    public TaskFactory getFactory(QName name) {
		return factories.get(name);
	}

	@Override
    public Type getType(QName name) {
		return types.get(name);
	}

	/**
	 * Register new experiment information
	 * 
	 * @param factory
	 */
	public void addFactory(TaskFactory factory) {
		LOGGER.info("Registering experiment %s", factory.id);
		TaskFactory oldFactory = factories.put(factory.id, factory);
		if (oldFactory != null) {
			LOGGER.info("Redefined old factory");
			oldFactory.getModule().remove(oldFactory);
		}

		Module module = factory.getModule();
		if (module == null)
			factory.setModule(defaultModule);
	}

	public void addType(Type type) {
		Type old = types.put(type.getId(), type);
		if (old != null)
			LOGGER.warn("Redefining type %s", type.getId());

	}

	public Map<QName, Module> getModules() {
		return modules;
	}

	public void addModule(Module module) {
		Module old = modules.put(module.getId(), module);
		if (old != null)
			LOGGER.warn("Redefining type %s", module.getId());

		// Add to the main module if no parent
		if (module.getParent() == null)
			module.setParent(defaultModule);
	}


	public Module getModule(QName qName) {
		if (qName == null)
			return defaultModule;
		return modules.get(qName);
	}

	public Module getDefaultModule() {
		return defaultModule;
	}

	
	/**
	 * Add a new XML Schema declaration
	 * 
	 * @param module
	 * @param xsModel
	 */
	public void addSchema(Module module, XSModel xsModel) {
		// Add the schema to the module
		module.addSchema(xsModel);
		
		// Add the different elements
		XSNamedMap components = xsModel
		.getComponents(XSConstants.ELEMENT_DECLARATION);
		for (int i = 0; i < components.getLength(); i++) {
			final XSElementDecl item = (XSElementDecl) components.item(i);
			QName qName = new QName(item.getNamespace(), item.getName());
			LOGGER.debug("New XML element [%s]", qName);
			xmlElements.put(qName, item);
		}
	}

	public XSElementDecl getXMLElement(QName type) {
		return xmlElements.get(type);
	}

	/**
	 * Close the repository 
	 */
	public void close() {
	}

}
