package sf.net.experimaestro.manager.js;

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

import org.apache.commons.lang.NotImplementedException;
import org.mozilla.javascript.*;
import sf.net.experimaestro.exceptions.XPMRhinoException;
import sf.net.experimaestro.manager.scripting.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Base class for all JS objects implementations
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
abstract public class JSBaseObject implements Scriptable, JSConstructable, Callable {
    private ClassDescription classDescription;
    private Scriptable prototype;
    private Scriptable parentScope;

    public JSBaseObject() {
        final Class<? extends JSBaseObject> aClass = getClass();
        this.classDescription = ClassDescription.analyzeClass(aClass);
    }

    public JSBaseObject(Class<?> wrappedClass) {
        this.classDescription = ClassDescription.analyzeClass(wrappedClass);
    }

    /**
     * Defines a new class.
     * <p/>
     * Used in order to plug our class constructor {@linkplain sf.net.experimaestro.manager.js.JSBaseObject.MyNativeJavaClass}
     * if the object is a {@linkplain sf.net.experimaestro.manager.js.JSBaseObject}
     *
     * @param scope The scope
     * @param aClass The class
     * @throws IllegalAccessException
     * @throws java.lang.reflect.InvocationTargetException
     * @throws InstantiationException
     */
    public static void defineClass(Scriptable scope, Class<?> aClass) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        if (Scriptable.class.isAssignableFrom(aClass)) {
            // If not a JSObject descendent, we handle this with standard JS procedure
            if (JSConstructable.class.isAssignableFrom(aClass)) {
                // Use our own constructor
                final String name = ClassDescription.getClassName(aClass);
                scope = ScriptableObject.getTopLevelScope(scope);
                final NativeJavaClass nativeJavaClass = new MyNativeJavaClass(scope, (Class<? extends Scriptable>) aClass);
                scope.put(name, scope, nativeJavaClass);
            } else {
                ScriptableObject.defineClass(scope, (Class<? extends Scriptable>) aClass);
            }
        } else {
            // Wraps the class
            final String name = ClassDescription.getClassName(aClass);
            scope.put(name, scope, new JavaScriptClass(aClass));
        }
    }


    @Override
    public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        MethodFunction function = getMethodFunction(ExposeMode.CALL);
        if (function.isEmpty()) {
            throw new XPMRhinoException("Cannot call object of type %s", getClassName());
        }
        JavaScriptContext jcx = new JavaScriptContext(cx, scope);
        return JavaScriptRunner.wrap(jcx, function.call(jcx, thisObj, null, args));
    }

    @Override
    public String getClassName() {
        return ClassDescription.getClassName(classDescription.getWrappedClass());
    }

    @Override
    public Object get(String name, Scriptable start) {
        // Search for a function
        MethodFunction function = getMethodFunction(name);
        if (!function.isEmpty()) {
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

    private MethodFunction getMethodFunction(Object key) {
        MethodFunction function = new MethodFunction(key);

        ArrayList<Method> methods = this.classDescription.getMethods().get(key);
        if (methods != null && !methods.isEmpty())
            function.add(methods);

        // Walk the prototype chain
        for (Scriptable prototype = getPrototype(); prototype != null; prototype = prototype.getPrototype()) {
            if (prototype instanceof JSBaseObject) {
                JSBaseObject jsPrototype = (JSBaseObject) prototype;
                methods = jsPrototype.classDescription.getMethods().get(key);
                if (methods != null && !methods.isEmpty())
                    function.add(methods);
            }
        }
        return function;
    }



    /**
     * Returns the underlying object
     *
     * @return An object
     */
    protected Object thisObject() {
        return this;
    }

    @Override
    public Object get(int index, Scriptable start) {
        MethodFunction function = getMethodFunction(ExposeMode.INDEX);
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
    public Object getDefaultValue(Class<?> hint) {
        return toString();
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
            return (Scriptable)JavaScriptRunner.wrap(cx, scope, obj);
        }

    }

    private static class MyNativeJavaClass extends NativeJavaClass {
        public MyNativeJavaClass(Scriptable scriptable, Class<? extends Scriptable> aClass) {
            super(scriptable, aClass);
        }

        @Override
        public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
            ClassDescription description = ClassDescription.analyzeClass((Class) javaObject);
            String className = ClassDescription.getClassName((Class) javaObject);
            ConstructorFunction constructorFunction = new ConstructorFunction(className, description.getConstructors());
            JavaScriptContext jcx = new JavaScriptContext(cx, scope);
            Object object = constructorFunction.call(jcx, null, null, args);

            return (Scriptable) object;

        }
    }
}
