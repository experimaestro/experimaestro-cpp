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

package sf.net.experimaestro.manager.js;

import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import sf.net.experimaestro.exceptions.ExperimaestroException;
import sf.net.experimaestro.manager.Manager;
import sf.net.experimaestro.manager.Module;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.manager.Repository;
import sf.net.experimaestro.utils.JSUtils;

public class JSModule extends Module {
	/**
	 * Creates a new module from a JavaScript description
	 * 
	 * @param jsScope
	 *            The scope where the object was created
	 * @param jsObject
	 *            The object itself
	 */
	public JSModule(Repository repository, Scriptable jsScope,
			NativeObject jsObject) {
		id = JSUtils.get(jsScope, "id", jsObject);
		name = JSUtils.get(jsScope, "name", jsObject);
		documentation = JSUtils
				.toDocument(JSUtils.get(jsScope, "description", jsObject),
						new QName(Manager.EXPERIMAESTRO_NS, "documentation"));

		// Set the parent
		Object parent = JSUtils.get(jsScope, "parent", jsObject, null);
		Module module = getModule(repository, parent);
		if (module != null)
			setParent(module);

	}

	static public Module getModule(Repository repository, Object parent) {
		if (parent == null)
			return null;

		if (parent instanceof Module)
			return (Module) parent;
		
		if (parent instanceof QName) {
			Module module = repository.getModules().get(parent);
			if (module == null)
				throw new ExperimaestroException("No module of name [%s]",
						parent);
			return module;
		}
		

		throw new ExperimaestroException(
				"Cannot search for module with type %s [%s]",
				parent.getClass(), parent);
	}
}
