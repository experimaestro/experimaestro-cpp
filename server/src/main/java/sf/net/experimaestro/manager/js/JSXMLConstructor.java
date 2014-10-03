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

package sf.net.experimaestro.manager.js;

import org.apache.commons.lang.NotImplementedException;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.xml.sax.SAXException;
import sf.net.experimaestro.exceptions.XPMRhinoException;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.XMLUtils;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

/**
 * A class used to construct XML
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 6/3/13
 */
public class JSXMLConstructor implements Scriptable, JSConstructable, Callable {
    @Override
    public String getClassName() {
        return "XMLConstructor";
    }

    @Override
    public Object get(String name, Scriptable start) {
        // TODO: implement get
        throw new NotImplementedException();
    }

    @Override
    public Object get(int index, Scriptable start) {
        // TODO: implement get
        throw new NotImplementedException();
    }

    @Override
    public boolean has(String name, Scriptable start) {
        // TODO: implement has
        throw new NotImplementedException();
    }

    @Override
    public boolean has(int index, Scriptable start) {
        // TODO: implement has
        throw new NotImplementedException();
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        // TODO: implement put
        throw new NotImplementedException();
    }

    @Override
    public void put(int index, Scriptable start, Object value) {
        // TODO: implement put
        throw new NotImplementedException();
    }

    @Override
    public void delete(String name) {
        // TODO: implement delete
        throw new NotImplementedException();
    }

    @Override
    public void delete(int index) {
        // TODO: implement delete
        throw new NotImplementedException();
    }

    @Override
    public Scriptable getPrototype() {
        // TODO: implement getPrototype
        throw new NotImplementedException();
    }

    @Override
    public void setPrototype(Scriptable prototype) {
        // TODO: implement setPrototype
        throw new NotImplementedException();
    }

    @Override
    public Scriptable getParentScope() {
        // TODO: implement getParentScope
        throw new NotImplementedException();
    }

    @Override
    public void setParentScope(Scriptable parent) {
        // TODO: implement setParentScope
        throw new NotImplementedException();
    }

    @Override
    public Object[] getIds() {
        // TODO: implement getIds
        throw new NotImplementedException();
    }

    @Override
    public Object getDefaultValue(Class<?> hint) {
        // TODO: implement getDefaultValue
        throw new NotImplementedException();
    }

    @Override
    public boolean hasInstance(Scriptable instance) {
        // TODO: implement hasInstance
        throw new NotImplementedException();
    }

    @Override
    public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        NamespaceContext ns = JSUtils.getNamespaceContext(scope);

        if (args.length != 1)
            throw new IllegalArgumentException("Expected only one argument");

        String s = JSUtils.toString(args[0]);

        try {
            return XMLUtils.parseString(s);
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new XPMRhinoException(e, "Error while parsing the XML document: %s", e.toString());
        }
    }
}
