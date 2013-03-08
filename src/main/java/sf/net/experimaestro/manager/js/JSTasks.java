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
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Ref;
import org.mozilla.javascript.RefCallable;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.XPMRhinoException;
import sf.net.experimaestro.exceptions.ValueMismatchException;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.manager.TaskFactory;
import sf.net.experimaestro.utils.JSUtils;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 7/2/13
 */
public class JSTasks extends JSBaseObject implements RefCallable {
    XPMObject xpm;

    public JSTasks(XPMObject xpm) {
        this.xpm = xpm;
    }

    @JSFunction(value = "set", scope = true)
    public JSTaskFactory set(Context cx, Scriptable scope, String qname, NativeObject definition) {
        QName id = QName.parse(qname, JSUtils.getNamespaceContext(scope));
        return new TaskRef(id).set(cx, definition);
    }

    @JSFunction(value = "get", scope = true)
    public Object get(Context cx, Scriptable scope, String qname) {
        QName id = QName.parse(qname, JSUtils.getNamespaceContext(scope));
        return new TaskRef(id).get(cx);
    }

    @Override
    public String getClassName() {
        return "Tasks";
    }


    @Override
    public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (args.length != 1)
            throw new IllegalArgumentException("Expected only one argument");

        QName id = QName.parse(JSUtils.toString(args[0]), JSUtils.getNamespaceContext(scope));

        return new TaskRef(id).get(cx);
    }

    @Override
    public Ref refCall(Context cx, Scriptable scope, Object[] args) {
        if (args.length != 1)
            throw new IllegalArgumentException("Expected only one argument");

        QName id = QName.parse(JSUtils.toString(args[0]), JSUtils.getNamespaceContext(scope));

        return new TaskRef(id);
    }

    class TaskRef extends Ref {
        private final QName id;

        public TaskRef(QName id) {
            this.id = id;
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
        public JSTaskFactory set(Context cx, Object _value) {
            NativeObject value = (NativeObject) _value;
            final JSTaskFactory factory;
            try {
                factory = new JSTaskFactory(id, value.getParentScope(), value, xpm.getRepository());
            } catch (ValueMismatchException e) {
                throw new XPMRhinoException(e);
            }
            xpm.getRepository().addFactory(factory.factory);
            return factory;
        }
    }
}
