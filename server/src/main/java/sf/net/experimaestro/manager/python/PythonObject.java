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
import sf.net.experimaestro.manager.scripting.MethodFunction;
import sf.net.experimaestro.manager.scripting.ClassDescription;

import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * A python object for exposed
 */
class PythonObject extends PyObject {
    private final ClassDescription description;
    private final Object object;

    public PythonObject(Object object) {
        PythonType pythonType;
        this.object = object;
        objtype = pythonType = new PythonType(object.getClass());
        description = pythonType.description;
    }

    @Override
    public PyObject __findattr_ex__(String name) {
        final ArrayList<Method> methods = description.getMethods().get(name);
        if (methods != null) {
            final MethodFunction methodFunction = new MethodFunction(name);
            methodFunction.add(object, methods);
            return new PythonMethod(methodFunction);
        }
        return null;
    }

    @Override
    public PyObject __finditem__(PyObject key) {
        return super.__finditem__(key);
    }


    @Override
    public PyObject __call__(PyObject[] args, String[] keywords) {
        return super.__call__(args, keywords);
    }

}
