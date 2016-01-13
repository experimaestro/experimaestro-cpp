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

import org.python.core.*;
import sf.net.experimaestro.manager.scripting.ClassDescription;
import sf.net.experimaestro.manager.scripting.ConstructorFunction;
import sf.net.experimaestro.manager.scripting.MethodFunction;
import sf.net.experimaestro.utils.log.Logger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Function;

/**
 * Corresponds to a python type
 */
class PythonType extends PyType {
    final static private Logger LOGGER = Logger.getLogger();
    final ClassDescription description;

    protected PythonType(Class<?> aClass) {
        super(PyObject.TYPE);
        this.description = ClassDescription.analyzeClass(aClass);
        this.builtin = true;
        this.name = description.getClassName();
    }

    @Override
    public String fastGetName() {
        return description.getClassName();
    }

    @Override
    public PyObject __call__(PyObject[] args, String[] keywords) {
        final ConstructorFunction constructorFunction = new ConstructorFunction(description.getClassName(), description.getConstructors());

        final Object result = constructorFunction.call(new PythonContext(), null, null, args);
        return PythonRunner.wrap(result);
    }

    @Override
    public String getName() {
        return description.getClassName();
    }

    static public PyClass getPyClass(Class<?> clazz) {
        ClassDescription cd = ClassDescription.analyzeClass(clazz);
        final PyStringMap dict = new PyStringMap();
        final Function<PyObject, Object> java_object__ = x -> ((PythonObject) x.__getattr__("__java_object__")).object;

        for (Map.Entry<Object, ArrayList<Method>> objectArrayListEntry : cd.getMethods().entrySet()) {
            final Object key = objectArrayListEntry.getKey();
            if (key instanceof String) {
                final String name = (String) key;
                final MethodFunction unique_directory = new MethodFunction(name);
                unique_directory.add(objectArrayListEntry.getValue());
                final PyFrame frame = Py.getThreadState().frame;
                final PyFunction pyFunction = new PyFunction(frame.f_globals, null, new InstanceMethod(name, unique_directory, java_object__), null, null);
                dict.__setitem__(name, pyFunction);
            }
        }
        return (PyClass) PyClass.classobj___new__(new PyString("Task"), new PyTuple(), dict);
    }
}
