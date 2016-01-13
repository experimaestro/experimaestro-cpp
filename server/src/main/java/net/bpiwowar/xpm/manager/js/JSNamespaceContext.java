package sf.net.experimaestro.manager.js;

/*
 * This file is part of experimaestro.
 * Copyright (c) 2014 B. Piwowarski <benjamin@bpiwowar.net>
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

import org.apache.commons.lang.NotImplementedException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import sf.net.experimaestro.manager.Namespace;
import sf.net.experimaestro.manager.scripting.Wrapper;
import sf.net.experimaestro.utils.JSUtils;

import javax.xml.namespace.NamespaceContext;
import java.util.Iterator;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class JSNamespaceContext implements NamespaceContext {
    private final Scriptable scope;

    public JSNamespaceContext(Scriptable scope) {
        this.scope = scope;
    }

    @Override
    public String getNamespaceURI(String prefix) {
        if (scope == null)
            return null;

        Scriptable currentScope = scope;
        Object object = Scriptable.NOT_FOUND;
        while (currentScope != null && object == Scriptable.NOT_FOUND) {
            object = ScriptableObject.getProperty(currentScope, prefix);
            currentScope = currentScope.getParentScope();
        }

        if (object == Scriptable.NOT_FOUND)
            return null;
        if (object instanceof Wrapper) {
            object = ((Wrapper) object).unwrap();
        }
        if (object instanceof Namespace) {
            return ((Namespace) object).getURI();
        }
        return JSUtils.toString(object);    }

    @Override
    public String getPrefix(String namespaceURI) {
        throw new NotImplementedException();
    }

    @Override
    public Iterator getPrefixes(String namespaceURI) {
        throw new NotImplementedException();
    }

}
