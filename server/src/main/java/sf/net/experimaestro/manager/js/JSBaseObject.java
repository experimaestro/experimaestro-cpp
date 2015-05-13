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
import org.w3c.dom.Node;
import sf.net.experimaestro.exceptions.XPMRhinoException;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.manager.scripting.ClassDescription;
import sf.net.experimaestro.manager.scripting.ConstructorFunction;
import sf.net.experimaestro.manager.scripting.Exposed;
import sf.net.experimaestro.manager.scripting.MethodFunction;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;

import static java.lang.String.format;

/**
 * Base class for all JS objects implementations
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 27/11/12
 */
abstract public class JSBaseObject implements Scriptable, JSConstructable, Callable {
    private XPMObject xpm;

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
     * Create a new JS object
     */
    public static <T> T newObject(Context cx, Scriptable scope, Class<T> aClass, Object... args) {
        return (T) cx.newObject(scope, ClassDescription.getClassName(aClass), args);
    }

    /**
     * Get the XPM object (thread)
     */
    final static XPMObject xpm() {
        return XPMObject.getThreadXPM();
    }

    /**
     * Defines a new class.
     * <p/>
     * Used in order to plug our class constructor {@linkplain sf.net.experimaestro.manager.js.JSBaseObject.MyNativeJavaClass}
     * if the object is a {@linkplain sf.net.experimaestro.manager.js.JSBaseObject}
     *
     * @param scope
     * @param aClass
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
            final String name = ClassDescription.getClassName(aClass);
            scope.put(name, scope, new WrappedJavaObject.WrappedClass(aClass));
        }
    }


    @Override
    public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        MethodFunction function = getMethodFunction(null);
        if (function.isEmpty())
            throw new XPMRhinoException("Cannot call object of type %s", getClassName());
        JavaScriptContext jcx = new JavaScriptContext(cx, scope);
        XPMObject xpm = XPMObject.getThreadXPM();
        return function.call(jcx, xpm != null ? xpm.getScriptContext() : null, thisObj, args);
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
            return new JavaScriptFunction(function);
        }

        // Search for a property
        final Field field = classDescription.getFields().get(name);
        if (field != null) {
            try {
                return field.get(this);
            } catch (IllegalAccessException e) {
                throw new XPMRhinoException("Illegal access to field [%s]", field.toString());
            }
        }
        return NOT_FOUND;
    }

    private MethodFunction getMethodFunction(String name) {
        MethodFunction function = new MethodFunction(name);

        ArrayList<Method> methods = this.classDescription.getMethods().get(name);
        if (methods != null && !methods.isEmpty())
            function.add(thisObject(), methods);

        // Walk the prototype chain
        for (Scriptable prototype = getPrototype(); prototype != null; prototype = prototype.getPrototype()) {
            if (prototype instanceof JSBaseObject) {
                JSBaseObject jsPrototype = (JSBaseObject) prototype;
                methods = jsPrototype.classDescription.getMethods().get(name);
                if (methods != null && !methods.isEmpty())
                    function.add(jsPrototype, methods);
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
        final Field field = classDescription.getFields().get(name);
        if (field != null) {
            if (classDescription.getFields().containsKey(name)) {
                try {
                    field.set(this, value);
                    return;
                } catch (IllegalAccessException e) {
                    throw new XPMRhinoException("Illegal access to field [%s]", field.toString());
                }
            }
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

    protected JSBaseObject setXPM(XPMObject xpm) {
        this.xpm = xpm;
        return this;
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
            if (obj == null) {
                return Undefined.instance;
            }

            if (obj instanceof Path)
                return new JSPath((Path) obj).setXPM(XPMObject.getXPM(scope));
            if (obj instanceof Node) {
                return new JSNode((Node) obj).setXPM(XPMObject.getXPM(scope));
            }
            if (obj instanceof Json) {
                return new JSJson((Json) obj).setXPM(XPMObject.getXPM(scope));
            }
            if (obj.getClass().getAnnotation(Exposed.class) != null) {
                return new WrappedJavaObject(cx, scope, obj);
            }
            return super.wrap(cx, scope, obj, staticType);
        }

        @Override
        public Scriptable wrapNewObject(Context cx, Scriptable scope, Object obj) {
            if (obj instanceof JSBaseObject)
                return (JSBaseObject) obj;

            if (obj instanceof JSTasks.TaskRef) {
                return (Scriptable) ((JSTasks.TaskRef) obj).get(cx);
            }

            if (obj instanceof Json) {
                return new JSJson((Json) obj).setXPM(XPMObject.getXPM(scope));
            }

            if (obj instanceof Node)
                return new JSNode((Node) obj).setXPM(XPMObject.getXPM(scope));

            if (obj instanceof Path) {
                return new JSPath((Path) obj).setXPM(XPMObject.getXPM(scope));
            }

            return super.wrapNewObject(cx, scope, obj);
        }

    }

    static private class MyNativeJavaObject extends NativeJavaObject {
        private MyNativeJavaObject(Scriptable scope, Object javaObject, Class<?> staticType, boolean isAdapter) {
            super(scope, javaObject, staticType, isAdapter);
        }

        @Override
        public String getClassName() {
            return ClassDescription.getClassName(this.staticType);
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
            XPMObject threadXPM = XPMObject.getThreadXPM();
            Object object = constructorFunction.call(jcx,
                    threadXPM != null ? threadXPM.getScriptContext() : null, null, args);

            return (Scriptable) object;

        }
    }
}
