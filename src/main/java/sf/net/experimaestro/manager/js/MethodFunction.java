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

import org.apache.commons.lang.ClassUtils;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.WrappedException;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.Output;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Wraps a method of an object
 */
class MethodFunction implements Callable {
    String name;
    ArrayList<Method> methods = new ArrayList<>();

    public MethodFunction(String name) {
        this.name = name;
    }

    @Override
    public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        double max = 0;
        Method argmax = null;
        for (Method method : methods) {
            double score = score(method, args);
            if (score > max) {
                max = score;
                argmax = method;
            }
        }

        if (argmax == null)
            throw ScriptRuntime.typeError(String.format("Could not find a matching method for %s(%s)", name,
                    Output.toString(", ", args, new Output.Formatter<Object>() {
                        @Override
                        public String format(Object o) {
                            return o.getClass().toString();
                        }
                    })));

        // Call the method

        try {
            args = transform(cx, scope, argmax, args);
            final Object invoke = argmax.invoke(thisObj, args);
            return cx.getWrapFactory().wrap(cx, scope, invoke, null);
        } catch (Throwable e) {
            if (e.getCause() != null)
                e = e.getCause();
            throw new WrappedException(e);
        }

    }

    private Object[] transform(Context cx, Scriptable scope, Method method, Object[] args) {
        final Class<?>[] types = method.getParameterTypes();
        Object methodArgs[] = new Object[types.length];

        // --- Add context and scope if needed
        final boolean useScope = method.getAnnotation(JSFunction.class).scope();
        int offset = useScope ? 2 : 0;
        if (useScope) {
            methodArgs[0] = cx;
            methodArgs[1] = scope;
        }

        // --- Copy the non vararg parameters
        final int length = types.length - (method.isVarArgs() ? 1 : 0) - offset;
        for (int i = 0; i < length; i++) {
            methodArgs[i + offset] = JSUtils.unwrap(args[i]);
        }

        // --- Deals with the vararg pararameters
        if (method.isVarArgs()) {
            final Class<?> varargType = types[length].getComponentType();
            int nbVarargs = args.length - length;
            final Object array[] = (Object[]) Array.newInstance(varargType, nbVarargs);
            for (int i = 0; i < nbVarargs; i++) {
                array[i] = JSUtils.unwrap(args[i + length]);
            }
            methodArgs[methodArgs.length - 1] = array;
        }

        return methodArgs;
    }

    private double score(Method method, Object[] args) {
        final boolean scope = method.getAnnotation(JSFunction.class).scope();
        int offset = scope ? 2 : 0;

        final Class<?>[] types = method.getParameterTypes();

        final int nbArgs = types.length - offset - (method.isVarArgs() ? 1 : 0);

        // If the methods is varargs, then we need at least nbArgs - 1 parameters
        if (method.isVarArgs()) {
            if (args.length < nbArgs)
                return 0;
        } else if (args.length != nbArgs)
            return 0;

        double score = 1;

        // Normal arguments
        for (int i = 0; i < nbArgs && score > 0; i++) {
            final Object o = JSUtils.unwrap(args[i]);
            Class<?> type = ClassUtils.primitiveToWrapper(types[i + offset]);
            if (type.isAssignableFrom(o.getClass()))
                continue;
            score = 0;
            break;
        }

        // Var args
        if (method.isVarArgs()) {
            Class<?> type = ClassUtils.primitiveToWrapper(types[types.length - 1].getComponentType());
            int nbVarArgs = args.length - nbArgs;
            for (int i = 0; i < nbVarArgs && score > 0; i++) {
                final Object o = JSUtils.unwrap(args[nbArgs + i]);
                if (type.isAssignableFrom(o.getClass()))
                    continue;
                score = 0;
                break;
            }
        }


        return score;
    }
}
