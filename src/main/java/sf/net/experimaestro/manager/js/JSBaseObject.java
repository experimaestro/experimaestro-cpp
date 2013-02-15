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
import org.mozilla.javascript.Scriptable;

import java.lang.reflect.Method;
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
        final Class<? extends JSBaseObject> aClass = getClass();
        methods = analyzeClass(aClass);
    }

    /**
     * Analyze a class and returns the map
     * @param aClass
     * @return
     */
    static Map<String, MethodFunction> analyzeClass(Class<?> aClass) {
        Map<String, MethodFunction> methods = METHODS.get(aClass);
        synchronized (METHODS) {
            if (methods == null) {
                METHODS.put(aClass, methods = new HashMap<>());

                for (Method method : aClass.getMethods()) {
                    final JSFunction annotation = method.getAnnotation(JSFunction.class);
                    if (annotation != null) {
                        MethodFunction methodFunction = methods.get(method.getName());
                        if (methodFunction == null) {
                            methods.put(annotation.value(), methodFunction = new MethodFunction());
                        }
                        methodFunction.methods.add(method);
                    }
                }
            }
        }
        return methods;
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

}
