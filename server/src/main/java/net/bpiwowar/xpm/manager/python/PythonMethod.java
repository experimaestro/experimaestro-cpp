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

import org.python.core.PyFunction;
import org.python.core.PyObject;
import net.bpiwowar.xpm.manager.scripting.MethodFunction;

import java.util.Arrays;
import java.util.HashMap;

/**
 * Python wrapper for object methods
 */
class PythonMethod extends PyObject {
    private final Object thisObj;
    private MethodFunction methodFunction;

    public PythonMethod(Object thisObj, MethodFunction methodFunction) {
        super(PyFunction.TYPE);
        this.thisObj = thisObj;
        this.methodFunction = methodFunction;
    }

    @Override
    public String toString() {
        return "Python method of " + methodFunction.getKey();
    }

    @Override
    public PyObject __call__(PyObject[] args, String[] keywords) {
        final MethodFunction methodFunction = this.methodFunction;
        return call(thisObj, args, keywords, methodFunction);
    }

    public static PyObject call(Object thisObj, PyObject[] args, String[] keywords, MethodFunction methodFunction) {
        HashMap<String, Object> options = new HashMap<>();

        final int nbArgs = args.length - keywords.length;
        for (int i = nbArgs; i < args.length; ++i) {
            options.put(keywords[i - nbArgs], PythonRunner.unwrap(args[i]));
        }
        final Object[] unwrap = PythonRunner.unwrap(Arrays.copyOf(args, nbArgs));


        return PythonRunner.wrap(methodFunction.call(new PythonContext(), thisObj, options, unwrap));
    }
}
