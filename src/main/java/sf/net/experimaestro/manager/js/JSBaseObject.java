/*
 * This file is part of experimaestro.
 * Copyright (c) 2012 B. Piwowarski <benjamin@bpiwowar.net>
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
import org.mozilla.javascript.WrappedException;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.utils.JSUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for all JS objects
 * <p/>
 * TODO: change the base class to this one when possible for cleaner documentation and easier
 * implementation
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 27/11/12
 */
public class JSBaseObject extends JSObject implements Scriptable {
    final static private HashMap<Class<?>, Map<String, MethodFunction>> METHODS = new HashMap<>();

    private Map<String, MethodFunction> methods;
    private Scriptable prototype;
    private Scriptable parentScope;

    public JSBaseObject() {
        synchronized (METHODS) {
            methods = METHODS.get(getClass());
            if (methods == null) {
                METHODS.put(getClass(), methods = new HashMap<>());

                for (Method method : getClass().getMethods()) {
                    if (method.getAnnotation(JSFunction.class) != null) {
                        MethodFunction methodFunction = methods.get(method.getName());
                        if (methodFunction == null) {
                            methods.put(method.getName(), methodFunction = new MethodFunction());
                        }
                        methodFunction.methods.add(method);
                    }
                }
            }
        }
    }


    /**
     * Returns the class name
     */
    static String getClassName(Class<?> aClass) {
        assert aClass.getSimpleName().startsWith("JS");
        return aClass.getSimpleName().substring(2);
    }


    @Override
    public String getClassName() {
        return JSBaseObject.getClassName(this.getClass());
    }

    @Override
    public Object get(String name, Scriptable start) {
        final MethodFunction theMethods = methods.get(name);
        if (theMethods == null)
            return NOT_FOUND;
        return theMethods;
    }

    @Override
    public Object get(int index, Scriptable start) {
        throw new NotImplementedException();
    }

    @Override
    public boolean has(String name, Scriptable start) {
        throw new NotImplementedException();
    }

    @Override
    public boolean has(int index, Scriptable start) {
        throw new NotImplementedException();
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        throw new NotImplementedException();
    }

    @Override
    public void put(int index, Scriptable start, Object value) {
        throw new NotImplementedException();
    }

    @Override
    public void delete(String name) {
        throw new NotImplementedException();
    }

    @Override
    public void delete(int index) {
        throw new NotImplementedException();
    }

    @Override
    public Scriptable getPrototype() {
        return prototype;
    }

    @Override
    public void setPrototype(Scriptable prototype) {
        this.prototype = prototype;
    }

    @Override
    public Scriptable getParentScope() {
        return parentScope;
    }

    @Override
    public void setParentScope(Scriptable parent) {
        this.parentScope = parent;
    }

    @Override
    public Object[] getIds() {
        throw new NotImplementedException();
    }

    @Override
    public Object getDefaultValue(Class<?> hint) {
        return toString();
    }

    @Override
    public boolean hasInstance(Scriptable instance) {
        throw new NotImplementedException();
    }

    /**
     * Wraps a method of the object
     */
    private class MethodFunction implements Callable {
        ArrayList<Method> methods = new ArrayList<>();

        public MethodFunction() {
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
                throw new ExperimaestroRuntimeException("Could not find a matching method");

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
            final boolean useScope = method.getAnnotation(JSFunction.class).scope();
            int offset = useScope ? 2 : 0;
            if (useScope) {
                Object [] newArgs = new Object[args.length+2];
                System.arraycopy(args, 0, newArgs, 2, args.length);
                args = newArgs;
                args[0] = cx;
                args[1] = scope;
            }

            final Class<?>[] types = method.getParameterTypes();
            for (int i = offset; i < types.length; i++) {
                 args[i] =  JSUtils.unwrap(args[i]);
            }

            return args;
        }

        private double score(Method method, Object[] args) {
            final boolean scope = method.getAnnotation(JSFunction.class).scope();
            int offset = scope ? 2 : 0;

            final Class<?>[] types = method.getParameterTypes();
            if (args.length != types.length - offset)
                return 0;

            double score = 1;
            for (int i = offset; i < types.length && score > 0; i++) {
                final Object o = JSUtils.unwrap(args[i-offset]);
                if (types[i].isAssignableFrom(o.getClass()))
                    continue;
                score = 0;
                break;
            }

            return score;
        }
    }
}
