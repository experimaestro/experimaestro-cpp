/*
 * This file is part of experimaestro.
 * Copyright (c) 2013 B. Piwowarski <benjamin@bpiwowar.net>
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

package sf.net.experimaestro.utils;

import org.apache.commons.lang.NotImplementedException;
import org.mozilla.javascript.Scriptable;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;

import javax.xml.namespace.NamespaceContext;
import java.util.Iterator;

/**
 * A namespace context built from the current scope
* @author B. Piwowarski <benjamin@bpiwowar.net>
* @date 6/3/13
*/
class JSNamespaceContext implements NamespaceContext {
    private final Scriptable scope;

    public JSNamespaceContext(Scriptable scope) {
        this.scope = scope;
    }

    @Override
    public String getNamespaceURI(String prefix) {

        Object o = JSUtils.unwrap(JSUtils.get(scope, prefix));
        if (o == null)
            return null;
        if (o instanceof Scriptable) {
            Scriptable jsObject = (Scriptable) o;
            if ("Namespace".equals(jsObject.getClassName()))
                return jsObject.get("uri", jsObject).toString();
        }

        throw new ExperimaestroRuntimeException("%s is bound to a non namespace object (%s)", prefix, o.getClass());
    }

    @Override
    public String getPrefix(String namespaceURI) {
        // TODO: implement getPrefix
        throw new NotImplementedException();
    }

    @Override
    public Iterator getPrefixes(String namespaceURI) {
        // TODO: implement getPrefixes
        throw new NotImplementedException();
    }
}
