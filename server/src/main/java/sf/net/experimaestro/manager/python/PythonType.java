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
import org.python.core.PyType;
import sf.net.experimaestro.manager.scripting.ConstructorFunction;
import sf.net.experimaestro.manager.scripting.ClassDescription;
import sf.net.experimaestro.utils.log.Logger;

/**
 * Corresponds to a python type
 */
class PythonType extends PyType {
    final static private Logger LOGGER = Logger.getLogger();
    final ClassDescription description;

    protected PythonType(Class<?> aClass) {
        super(PyObject.TYPE);
        this.description = ClassDescription.analyzeClass(aClass);
    }

    @Override
    public String fastGetName() {
        return description.getClassName();
    }

    @Override
    public PyObject __call__(PyObject[] args, String[] keywords) {
        final ConstructorFunction constructorFunction = new ConstructorFunction(description.getClassName(), description.getConstructors());

        final Object result = constructorFunction.call(null, null, args);
        return PythonContext.wrap(result);
    }

    @Override
    public String getName() {
        return description.getClassName();
    }
}
