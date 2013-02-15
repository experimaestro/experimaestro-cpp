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
import org.mozilla.javascript.*;
import org.mozilla.javascript.xml.XMLObject;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.manager.TaskFactory;
import sf.net.experimaestro.utils.JSUtils;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 7/2/13
 */
public class JSTasks extends XMLObject implements JSConstructable {
    XPMObject xpm;

    public JSTasks(XPMObject xpm) {
        this.xpm = xpm;
    }

    @Override
    public String getClassName() {
        return "Tasks";
    }

    @Override
    public boolean has(Context cx, Object id) {
        throw new NotImplementedException();
    }

    @Override
    public Object get(Context cx, Object id) {
        throw new NotImplementedException();
    }

    @Override
    public void put(Context cx, Object id, Object value) {
        throw new NotImplementedException();
    }

    @Override
    public boolean delete(Context cx, Object id) {
        throw new NotImplementedException();
    }

    @Override
    public Object getFunctionProperty(Context cx, String name) {
        throw new NotImplementedException();
    }

    @Override
    public Object getFunctionProperty(Context cx, int id) {
        throw new NotImplementedException();
    }


    @Override
    public Scriptable getExtraMethodSource(Context cx) {
        throw new NotImplementedException();
    }

    @Override
    public Ref memberRef(Context cx, Object elem, int memberTypeFlags) {
        return new TaskRef(null, JSUtils.toString(elem));
    }

    @Override
    public Ref memberRef(Context cx, Object namespace, Object elem, int memberTypeFlags) {
        return new TaskRef(JSUtils.toString(namespace), JSUtils.toString(elem));
    }

    @Override
    public Object get(String name, Scriptable start) {
        return new TaskRef(null, name);
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        new TaskRef(null, name).set(Context.getCurrentContext(), value);
    }

    @Override
    public NativeWith enterWith(Scriptable scope) {
        throw new NotImplementedException();
    }

    @Override
    public NativeWith enterDotQuery(Scriptable scope) {
        throw new NotImplementedException();
    }

    class TaskRef extends Ref {
        private final QName id;

        public TaskRef(String namespace, String name) {
            this.id = new QName(namespace, name);
        }

        @Override
        public Object get(Context cx) {
            final TaskFactory factory = xpm.getRepository().getFactory(id);
            if (factory == null)
                return NOT_FOUND;
            if (factory instanceof JSTaskFactory.FactoryImpl)
               return new JSTaskFactory((JSTaskFactory.FactoryImpl)factory);

            throw new NotImplementedException();
        }

        @Override
        public Object set(Context cx, Object _value) {
            NativeObject value = (NativeObject) _value;
            final JSTaskFactory factory = new JSTaskFactory(id, value.getParentScope(), value, xpm.getRepository());
            xpm.getRepository().addFactory(factory.factory);
            return factory;
        }
    }
}
