package net.bpiwowar.xpm.manager.python;

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

import net.bpiwowar.xpm.exceptions.XPMScriptRuntimeException;
import net.bpiwowar.xpm.manager.scripting.ClassDescription;
import net.bpiwowar.xpm.manager.scripting.ExposeMode;
import net.bpiwowar.xpm.manager.scripting.MethodFunction;
import net.bpiwowar.xpm.manager.scripting.PropertyAccess;
import org.python.core.PyObject;
import org.python.core.PyString;

import java.util.Iterator;

/**
 * A python object for exposed
 */
class PythonObject extends PyObject {
    private static final Object NOT_FOUND = new Object();

    private final ClassDescription description;
    final Object object;

    public PythonObject(Object object) {
        PythonType pythonType;
        this.object = object;
        objtype = pythonType = new PythonType(object.getClass());
        description = pythonType.description;
    }


    @Override
    public PyObject __findattr_ex__(String name) {
        // Search for a function
        final PyObject attribute = PythonUtils.getAttribute(name, object, description);
        if (attribute == null) {
            noAttributeError(name);
        }
        return attribute;
    }


    @Override
    public Object __tojava__(Class<?> c) {
        if (c == Object.class) {
            return object;
        }
        return super.__tojava__(c);
    }

    @Override
    public PyObject __finditem__(PyObject key) {
        MethodFunction function = description.getMethod(ExposeMode.FIELDS);
        if (function != null && !function.isEmpty()) {
            final PythonContext pcx = new PythonContext();
            return PythonRunner.wrap(function.call(pcx, object, null, key));
        }

        return super.__finditem__(key);
    }

    @Override
    public void __setattr__(String name, PyObject value) {
        MethodFunction function = description.getMethod(ExposeMode.FIELDS);
        if (function != null && !function.isEmpty()) {
            final PythonContext pcx = new PythonContext();
            function.call(pcx, object, null, name, value);
        } else {
            // Search for a property
            final PropertyAccess propertyAccess = description.getFields().get(name);
            if (propertyAccess != null) {
                if (!propertyAccess.canSet()) {
                    throw new XPMScriptRuntimeException("Cannot set field " + name + " in " + fastGetClass());
                }
                final PythonContext pcx = new PythonContext();
                propertyAccess.set(object, pcx.toJava(value));
                return;
            }

            super.__setattr__(name, value);
        }
    }

    @Override
    public void __setitem__(PyObject key, PyObject value) {
        MethodFunction function = description.getMethod(ExposeMode.FIELDS);
        if (function != null && !function.isEmpty()) {
            final PythonContext pcx = new PythonContext();
            PythonRunner.wrap(function.call(pcx, object, null, key, value));
        } else {
            super.__setattr__(key.toString(), value);
        }
    }

    public boolean __contains__(PyObject o) {
        // FIXME: should have a FIELD_EXIST
        MethodFunction function = description.getMethod(ExposeMode.FIELDS);
        if (function != null && !function.isEmpty()) {
            final PythonContext pcx = new PythonContext();
            final Object call = function.call(pcx, object, null, o);
            return call != null;
        }

        return super.__contains__(o);
    }

    @Override
    public PyObject __iter__() {
        MethodFunction function = description.getMethod(ExposeMode.ITERATOR);
        if (function != null && !function.isEmpty()) {
            final PythonContext pcx = new PythonContext();
            final Iterator iterator = (Iterator) function.call(pcx, object, null);

            return PythonUtils.wrapIterator(iterator);
        }

        return super.__iter__();
    }

    @Override
    public PyObject __call__(PyObject[] args, String[] keywords) {
        final MethodFunction methods = description.getMethods().get(ExposeMode.CALL);
        if (methods != null) {
            final PythonContext pcx = new PythonContext();
            return PythonRunner.wrap(methods.call(pcx, object, null, args));
        }

        return super.__call__(args, keywords);
    }

    @Override
    public int __len__() {
        final Object result = runFunction(ExposeMode.LENGTH);
        if (result != NOT_FOUND) {
            return (Integer) result;
        }

        return super.__len__();
    }

    private Object runFunction(ExposeMode mode, Object... args) {
        final MethodFunction methods = description.getMethods().get(mode);
        if (methods != null) {
            final PythonContext pcx = new PythonContext();
            return methods.call(pcx, object, null, args);
        }
        return NOT_FOUND;
    }

    @Override
    public PyObject __finditem__(int key) {
        final Object result = runFunction(ExposeMode.FIELDS, key);
        if (result != NOT_FOUND) {
            return (PyObject) result;
        }

        return super.__finditem__(key);
    }

    @Override
    public PyString __str__() {
        return new PyString(object.toString());
    }

    @Override
    public PyString __repr__() {
        return new PyString(object.toString());
    }
}
