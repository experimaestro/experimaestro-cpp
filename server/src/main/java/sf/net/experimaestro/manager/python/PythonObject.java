package sf.net.experimaestro.manager.python;

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

import org.python.core.PyObject;
import sf.net.experimaestro.manager.scripting.ClassDescription;
import sf.net.experimaestro.manager.scripting.ExposeMode;
import sf.net.experimaestro.manager.scripting.MethodFunction;
import sf.net.experimaestro.manager.scripting.PropertyAccess;

import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * A python object for exposed
 */
class PythonObject extends PyObject {
    private final ClassDescription description;
    final Object object;

    public PythonObject(Object object) {
        PythonType pythonType;
        this.object = object;
        objtype = pythonType = new PythonType(object.getClass());
        description = pythonType.description;
    }


    private MethodFunction getMethodFunction(Object key) {
        MethodFunction function = new MethodFunction(key);

        ArrayList<Method> methods = description.getMethods().get(key);
        if (methods != null && !methods.isEmpty())
            function.add(object, methods);


        return function;
    }



    @Override
    public PyObject __findattr_ex__(String name) {
        // Search for a function
        MethodFunction function = getMethodFunction(name);
        if (!function.isEmpty()) {
            return new PythonMethod(function);
        }

        // Search for a property
        final PropertyAccess propertyAccess = description.getFields().get(name);
        if (propertyAccess != null) {
            return PythonRunner.wrap(propertyAccess.get(object));
        }

        // Search for property accessor
        function = getMethodFunction(ExposeMode.FIELDS);
        if (function != null && !function.isEmpty()) {
            final PythonContext pcx = new PythonContext();
            return PythonRunner.wrap(function.call(pcx, this, null, name));
        }


        noAttributeError(name);
        return null;
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
        MethodFunction function = getMethodFunction(ExposeMode.FIELDS);
        if (function != null && !function.isEmpty()) {
            final PythonContext pcx = new PythonContext();
            return PythonRunner.wrap(function.call(pcx, this, null, key));
        }

        return super.__finditem__(key);
    }


    @Override
    public PyObject __call__(PyObject[] args, String[] keywords) {
        final ArrayList<Method> methods = description.getMethods().get(ExposeMode.CALL);
        if (methods != null) {
            final MethodFunction methodFunction = new MethodFunction("()");
            methodFunction.add(object, methods);
            final PythonContext pcx = new PythonContext();
            return PythonRunner.wrap(methodFunction.call(pcx, this, null, args));
        }

        return super.__call__(args, keywords);
    }

}
