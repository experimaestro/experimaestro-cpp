package sf.net.experimaestro.utils;

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
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.manager.Namespace;
import sf.net.experimaestro.manager.scripting.Wrapper;

import javax.lang.model.element.Name;
import javax.xml.namespace.NamespaceContext;
import java.util.Iterator;

/**
 * A namespace context built from the current scope
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 6/3/13
 */
public class JSNamespaceContext implements NamespaceContext {
    private final Scriptable scope;

    public JSNamespaceContext(Scriptable scope) {
        this.scope = scope;
    }

    @Override
    public String getNamespaceURI(String prefix) {

        // Try the javascript scope
        Object o = JSUtils.unwrap(JSUtils.get(scope, prefix));
        if (o == null) {
            // Look at default namespaces

            return null;
        }

        if (o instanceof Scriptable) {
            Scriptable jsObject = (Scriptable) o;
            if (o instanceof Wrapper) {
                o = ((Wrapper) o).unwrap();
                if (o instanceof Namespace) {
                    return ((Namespace) o).getURI();
                }
            }
        }

        if (o instanceof Namespace) {
            return ((Namespace)o).getURI();
        }

        throw new XPMRuntimeException("%s is bound to a non namespace object (%s)", prefix, o.getClass());
    }

    @Override
    public String getPrefix(String namespaceURI) {
        throw new NotImplementedException();
    }

    @Override
    public Iterator getPrefixes(String namespaceURI) {
        throw new NotImplementedException();
    }
}
