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

package sf.net.experimaestro.manager.js;

import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.manager.Manager;
import sf.net.experimaestro.manager.Module;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.manager.Repository;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.log.Logger;

public class JSModule extends JSBaseObject {
    final static private Logger LOGGER = Logger.getLogger();
    private final XPMObject xpm;

    Module module;

    /**
     * Creates a new module from a JavaScript description
     *
     * @param jsScope  The scope where the object was created
     * @param jsObject The object itself
     */
    @JSFunction
    public JSModule(XPMObject xpm, Repository repository, Scriptable jsScope,
                    NativeObject jsObject) {
        this.xpm = xpm;
        module = new Module(JSUtils.get(jsScope, "id", jsObject));

        module.setName(JSUtils.toString(JSUtils.get(jsScope, "name", jsObject)));
        module.setDocumentation(JSUtils
                .toDocument(jsScope, JSUtils.get(jsScope, "description", jsObject),
                        new QName(Manager.EXPERIMAESTRO_NS, "documentation")));

        // Set the parent
        Module parent = getModule(repository, JSUtils.get(jsScope, "parent", jsObject, null));
        if (parent != null)
            module.setParent(parent);

    }

    static public enum Type {
        SCHEMA,
        RELAXNG
    }

    private static final String DEFAULT_OUTPUT_ENCODING = "UTF-8";
    private static final int DEFAULT_LINE_LENGTH = 72;
    private static final int DEFAULT_INDENT = 2;


    static public Module getModule(Repository repository, Object parent) {
        if (parent == null)
            return null;

        if (parent instanceof Module)
            return (Module) parent;

        if (parent instanceof QName) {
            Module module = repository.getModules().get(parent);
            if (module == null)
                throw new XPMRuntimeException("No module of name [%s]",
                        parent);
            return module;
        }


        throw new XPMRuntimeException(
                "Cannot search for module with type %s [%s]",
                parent.getClass(), parent);
    }

}
