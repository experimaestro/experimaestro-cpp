package sf.net.experimaestro.manager.js;

import javax.xml.namespace.QName;

import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;

import sf.net.experimaestro.exceptions.ExperimaestroException;
import sf.net.experimaestro.manager.Manager;
import sf.net.experimaestro.manager.Module;
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
