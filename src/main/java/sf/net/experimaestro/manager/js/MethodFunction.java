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

import com.google.common.base.Function;
import com.google.common.base.Functions;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.NotImplementedException;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.XPMRhinoException;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.Output;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import static java.lang.StrictMath.min;

/**
 * Wraps a method of an object
 */
class MethodFunction implements Callable, org.mozilla.javascript.Function {
    String name;
    private ArrayList<Method> methods = new ArrayList<>();

    public MethodFunction(String name) {
        this.name = name;
    }

    void add(Method method) {
        methods.add(method);
    }

    static private final Function IDENTITY = com.google.common.base.Functions.identity();

    @Override
    public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
        throw new NotImplementedException();
    }

    @Override
    public String getClassName() {
        // TODO: implement getClassName
        throw new NotImplementedException();
    }

    @Override
    public Object get(String name, Scriptable start) {
        // TODO: implement get
        throw new NotImplementedException();
    }

    @Override
    public Object get(int index, Scriptable start) {
        // TODO: implement get
        throw new NotImplementedException();
    }

    @Override
    public boolean has(String name, Scriptable start) {
        // TODO: implement has
        throw new NotImplementedException();
    }

    @Override
    public boolean has(int index, Scriptable start) {
        // TODO: implement has
        throw new NotImplementedException();
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        // TODO: implement put
        throw new NotImplementedException();
    }

    @Override
    public void put(int index, Scriptable start, Object value) {
        // TODO: implement put
        throw new NotImplementedException();
    }

    @Override
    public void delete(String name) {
        // TODO: implement delete
        throw new NotImplementedException();
    }

    @Override
    public void delete(int index) {
        // TODO: implement delete
        throw new NotImplementedException();
    }

    @Override
    public Scriptable getPrototype() {
        // TODO: implement getPrototype
        throw new NotImplementedException();
    }

    @Override
    public void setPrototype(Scriptable prototype) {
        // TODO: implement setPrototype
        throw new NotImplementedException();
    }

    @Override
    public Scriptable getParentScope() {
        // TODO: implement getParentScope
        throw new NotImplementedException();
    }

    @Override
    public void setParentScope(Scriptable parent) {
        // TODO: implement setParentScope
        throw new NotImplementedException();
    }

    @Override
    public Object[] getIds() {
        // TODO: implement getIds
        throw new NotImplementedException();
    }

    @Override
    public Object getDefaultValue(Class<?> hint) {
        // TODO: implement getDefaultValue
        throw new NotImplementedException();
    }

    @Override
    public boolean hasInstance(Scriptable instance) {
        // TODO: implement hasInstance
        throw new NotImplementedException();
    }

    @Override
    public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        Method argmax = null;
        int max = Integer.MIN_VALUE;

        Function argmaxConverters[] = new Function[args.length];
        Function converters[] = new Function[args.length];

        for (Method method : methods) {
            int score = score(method, args, converters);
            if (score > max) {
                max = score;
                argmax = method;
                Function tmp[] = argmaxConverters;
                argmaxConverters = converters;
                converters = tmp;
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
            boolean isStatic = (argmax.getModifiers() & Modifier.STATIC) != 0;
            args = transform(cx, scope, argmax, args, argmaxConverters);
            final Object invoke = argmax.invoke(isStatic ? null : thisObj, args);
            return invoke == null ? Undefined.instance : invoke;
        } catch (XPMRhinoException e) {
            throw e;
        } catch (Throwable e) {
            throw new XPMRhinoException(e);
        }

    }

    public void addAll(MethodFunction other) {
        methods.addAll(other.methods);
    }


    static public class Converter {
        int score = Integer.MAX_VALUE;

        Function converter(Object o, Class<?> type) {
            if (o == null) {
                score--;
                return IDENTITY;
            }

            // Assignable: OK
            type = ClassUtils.primitiveToWrapper(type);
            if (type.isAssignableFrom(o.getClass()))
                return IDENTITY;

            // Case of string: anything can be converted, but with different
            // scores
            if (type == String.class) {
                if (o instanceof Scriptable) {
                    switch (((Scriptable) o).getClassName()) {
                        case "String":
                        case "ConsString":
                            return Functions.toStringFunction();
                    }
                } else if (o instanceof CharSequence) {
                    score--;
                } else {
                    score -= 10;
                }
                return Functions.toStringFunction();
            }

            score = Integer.MIN_VALUE;
            return null;
        }

        public boolean isOK() {
            return score != Integer.MIN_VALUE;
        }
    }

    private int score(Method method, Object[] args, Function[] converters) {
        // Get the annotations
        JSFunction annotation = method.getAnnotation(JSFunction.class);
        final boolean scope = annotation.scope();
        int optional = annotation.optional();

        // Start the scoring
        Converter converter = new Converter();
        int offset = (scope ? 2 : 0);

        final Class<?>[] types = method.getParameterTypes();

        final int nbArgs = types.length - offset - (method.isVarArgs() ? 1 : 0);

        // The number of arguments should be in:
        // [nbArgs - optional - 1, ...] if varargs
        // [nbArgs - optional, nbArgs] otherwise

        if (method.isVarArgs()) {
            if (args.length < nbArgs - optional - 1)
                return Integer.MIN_VALUE;
        } else if (args.length < nbArgs - optional || args.length > nbArgs)
            return Integer.MIN_VALUE;


        // Normal arguments
        for (int i = 0; i < args.length && i < nbArgs && converter.isOK(); i++) {
            final Object o = JSUtils.unwrap(args[i]);
            converters[i] = converter.converter(o, types[i + offset]);
        }

        // Var args
        if (method.isVarArgs()) {
            Class<?> type = ClassUtils.primitiveToWrapper(types[types.length - 1].getComponentType());
            int nbVarArgs = args.length - nbArgs;
            for (int i = 0; i < nbVarArgs && converter.isOK(); i++) {
                final Object o = JSUtils.unwrap(args[nbArgs + i]);
                converters[nbArgs + i] = converter.converter(o, type);
            }
        }

        return converter.score;
    }

    /**
     * Transform the arguments
     *
     * @param cx
     * @param scope
     * @param method
     * @param args
     * @return
     */
    private Object[] transform(Context cx, Scriptable scope, Method method, Object[] args, Function[] converters) {
        final Class<?>[] types = method.getParameterTypes();
        Object methodArgs[] = new Object[types.length];

        // --- Add context and scope if needed
        JSFunction annotation = method.getAnnotation(JSFunction.class);

        final boolean useScope = annotation.scope();
        int offset = useScope ? 2 : 0;
        if (useScope) {
            methodArgs[0] = cx;
            methodArgs[1] = scope;
        }

        // --- Copy the non vararg parameters
        final int length = types.length - (method.isVarArgs() ? 1 : 0) - offset;
        int size = min(length, args.length);
        for (int i = 0; i < size; i++) {
            methodArgs[i + offset] = converters[i].apply(JSUtils.unwrap(args[i]));
        }

        // --- Deals with the vararg pararameters
        if (method.isVarArgs()) {
            final Class<?> varargType = types[length].getComponentType();
            int nbVarargs = args.length - length;
            final Object array[] = (Object[]) Array.newInstance(varargType, nbVarargs);
            for (int i = 0; i < nbVarargs; i++) {
                array[i] = converters[i + length].apply(JSUtils.unwrap(args[i + length]));
            }
            methodArgs[methodArgs.length - 1] = array;
        }

        return methodArgs;
    }


}
