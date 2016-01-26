package net.bpiwowar.xpm.manager.js;

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

import net.bpiwowar.xpm.exceptions.XPMRhinoException;
import net.bpiwowar.xpm.manager.scripting.*;
import net.bpiwowar.xpm.manager.scripting.Wrapper;
import org.apache.commons.lang.NotImplementedException;
import org.mozilla.javascript.*;

import java.lang.reflect.InvocationTargetException;

/**
 * A wrapper for exposed java objects
 */
public class JavaScriptObject implements Wrapper, Scriptable, Callable {
    private final Object object;
    private ClassDescription classDescription;
    private Scriptable prototype;
    private Scriptable parentScope;

    public JavaScriptObject(Object object, ClassDescription classDescription) {
        this.object = object;
        this.classDescription = classDescription;
    }

    /**
     * Defines a new class.
     *
     * @param scope  The scope
     * @param aClass The class
     * @throws IllegalAccessException
     * @throws java.lang.reflect.InvocationTargetException
     * @throws InstantiationException
     */
    public static void defineClass(Scriptable scope, Class<?> aClass) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        if (Scriptable.class.isAssignableFrom(aClass)) {
            ScriptableObject.defineClass(scope, (Class<? extends Scriptable>) aClass);
        } else {
            // Wraps the class
            final ClassDescription description = ClassDescription.analyzeClass(aClass);
            scope.put(description.getClassName(), scope, new JavaScriptClass(description));
        }
    }

    @Override
    public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        MethodFunction function = getMethodFunction(ExposeMode.CALL);
        if (function.isEmpty()) {
            throw new XPMRhinoException("Cannot call object of type %s", getClassName());
        }
        JavaScriptContext jcx = new JavaScriptContext(cx, scope);
        return JavaScriptRunner.wrap(jcx, function.call(jcx, object, null, args));
    }

    @Override
    public Object unwrap() {
        return object instanceof Wrapper ? ((Wrapper) object).unwrap() : object;
    }

    protected Object thisObject() {
        return this.object;
    }

    @Override
    public Object getDefaultValue(Class<?> hint) {
        return getClassName();
    }

    @Override
    public String toString() {
        return unwrap().toString();
    }

    @Override
    public String getClassName() {
        return classDescription.getClassName();
    }

    @Override
    public Object get(String name, Scriptable start) {
        // Search for a function
        MethodFunction function = getMethodFunction(name);
        if (function != null) {
            return new JavaScriptFunction(thisObject(), function);
        }

        // Search for a property
        final PropertyAccess propertyAccess = classDescription.getFields().get(name);
        if (propertyAccess != null) {
            final JavaScriptContext jcx = new JavaScriptContext(Context.getCurrentContext(), start);
            return JavaScriptRunner.wrap(Context.getCurrentContext(), start, propertyAccess.get(thisObject()).get(jcx));
        }

        // Search for property accessor
        function = getMethodFunction(ExposeMode.FIELDS);
        if (function != null && !function.isEmpty()) {
            final JavaScriptContext jcx = new JavaScriptContext(Context.getCurrentContext(), start);
            final Object result = function.call(jcx, thisObject(), null, name);
            return JavaScriptRunner.wrap(Context.getCurrentContext(), start, result);
        }

        return NOT_FOUND;
    }

    protected MethodFunction getMethodFunction(Object key) {
        return this.classDescription.getMethods().get(key);
    }

    @Override
    public Object get(int index, Scriptable start) {
        MethodFunction function = getMethodFunction(ExposeMode.FIELDS);
        if (function != null) {
            final JavaScriptContext jcx = new JavaScriptContext(Context.getCurrentContext(), start);
            final Object result = function.call(jcx, thisObject(), null, index);
            return JavaScriptRunner.wrap(Context.getCurrentContext(), start, result);
        }
        return NOT_FOUND;
    }

    @Override
    public boolean has(String name, Scriptable start) {
        return classDescription.getMethods().containsKey(name) || classDescription.getFields().containsKey(name);
    }

    @Override
    public boolean has(int index, Scriptable start) {
        return false;
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        final PropertyAccess field = classDescription.getFields().get(name);
        if (field != null) {
            if (classDescription.getFields().containsKey(name)) {
                field.set(this, value);
                return;
            }
        }

        // Search for property accessor
        MethodFunction function = getMethodFunction(ExposeMode.FIELDS);
        if (function != null) {
            final JavaScriptContext jcx = new JavaScriptContext(Context.getCurrentContext(), start);
            function.call(jcx, thisObject(), null, name, value);
            return;
        }


        throw new XPMRhinoException("Setting the value of a sealed object (" + getClassName() + ")");
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
        return new Object[]{};
    }

    @Override
    public boolean hasInstance(Scriptable instance) {
        throw new NotImplementedException();
    }

    public byte[] getBytes() {
        return toString().getBytes();
    }

    /**
     * The Experimaestro wrap factory to handle special cases
     */
    static public class XPMWrapFactory extends WrapFactory {
        public final static XPMWrapFactory INSTANCE = new XPMWrapFactory();

        private XPMWrapFactory() {
            setJavaPrimitiveWrap(false);
        }

        @Override
        public Scriptable wrapAsJavaObject(Context cx, Scriptable scope, Object javaObject, Class<?> staticType) {
            return super.wrapAsJavaObject(cx, scope, javaObject, staticType);
        }

        @Override
        public Scriptable wrapJavaClass(Context cx, Scriptable scope, Class javaClass) {
            return super.wrapJavaClass(cx, scope, javaClass);
        }

        @Override
        public Object wrap(Context cx, Scriptable scope, Object obj, Class<?> staticType) {
            return JavaScriptRunner.wrap(cx, scope, obj);
        }

        @Override
        public Scriptable wrapNewObject(Context cx, Scriptable scope, Object obj) {
            return (Scriptable) JavaScriptRunner.wrap(cx, scope, obj);
        }

    }
}
