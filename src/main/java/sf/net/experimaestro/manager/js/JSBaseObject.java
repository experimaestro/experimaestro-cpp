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
import org.mozilla.javascript.NativeJavaClass;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.WrapFactory;
import org.w3c.dom.Node;
import sf.net.experimaestro.exceptions.XPMRhinoException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
abstract public class JSBaseObject implements Scriptable, JSConstructable, Callable {
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
     *
     * @param aClass
     * @return
     */
    static Map<String, MethodFunction> analyzeClass(Class<?> aClass) {
        Map<String, MethodFunction> methods = METHODS.get(aClass);
        synchronized (METHODS) {
            if (methods == null) {
                METHODS.put(aClass, methods = new HashMap<>());
                for (Method method : aClass.getDeclaredMethods()) {
                    final JSFunction annotation = method.getAnnotation(JSFunction.class);

                    if (annotation != null) {
                        if ((method.getModifiers() & Modifier.PUBLIC) == 0)
                            throw new AssertionError("The method " + method + " is not public");
                        String jsName = annotation.value();
                        if (annotation.call()) {
                            assert jsName.equals("");
                            jsName = null;
                        } else if ("".equals(jsName))
                            jsName = method.getName();

                        MethodFunction methodFunction = methods.get(jsName);
                        if (methodFunction == null) {
                            methods.put(jsName, methodFunction = new MethodFunction(jsName));
                        }
                        methodFunction.add(method);
                    }
                }

                Class<?> superclass = aClass.getSuperclass();
                if (JSBaseObject.class.isAssignableFrom(superclass)) {
                    Map<String, MethodFunction> superclassMethods = analyzeClass(superclass);
                    for (MethodFunction superclassMethod : superclassMethods.values()) {
                        MethodFunction previous = methods.get(superclassMethod.name);
                        if (previous != null)
                            previous.addAll(superclassMethod);
                        else
                            methods.put(superclassMethod.name, superclassMethod);
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
        JSObjectDescription annotation = aClass.getAnnotation(JSObjectDescription.class);
        if (annotation != null && !"".equals(annotation.name()))
            return annotation.name();

        assert aClass.getSimpleName().startsWith("JS");
        return aClass.getSimpleName().substring(2);
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
     *
     * @throws InstantiationException
     */
    public static void defineClass(Scriptable scope, Class<? extends Scriptable> aClass) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // If not a JSObject descendent, we handle this with standard JS procedure
        if (JSConstructable.class.isAssignableFrom(aClass)) {
            // Use our own constructor
            final String name = JSBaseObject.getClassName(aClass);
            scope = ScriptableObject.getTopLevelScope(scope);
            final NativeJavaClass nativeJavaClass = new MyNativeJavaClass(scope, aClass);
            scope.put(name, scope, nativeJavaClass);
        } else {
            ScriptableObject.defineClass(scope, aClass);
        }
    }

    @Override
    public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (methods.containsKey(null))
            return methods.get(null).call(cx, scope, this, args);
        throw new XPMRhinoException("Cannot call object of type %s", getClassName());
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
        return NOT_FOUND;
    }

    @Override
    public boolean has(String name, Scriptable start) {
        return methods.containsKey(name);
    }

    @Override
    public boolean has(int index, Scriptable start) {
        return false;
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        throw new UnsupportedOperationException("Setting the value of a sealed object (" + getClassName() + ")");
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
        throw new UnsupportedOperationException("Enumerate JS object: " + getClassName());
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
        }


        @Override
        public Object wrap(Context cx, Scriptable scope, Object obj, Class<?> staticType) {
            return super.wrap(cx, scope, obj, staticType);
        }

        @Override
        public Scriptable wrapNewObject(Context cx, Scriptable scope, Object obj) {
            if (obj instanceof JSBaseObject)
                return (JSBaseObject) obj;

            if (obj instanceof JSTasks.TaskRef) {
                return (Scriptable) ((JSTasks.TaskRef) obj).get(cx);
            }

            if (obj instanceof Node)
                return new JSNode((Node) obj);


            return super.wrapNewObject(cx, scope, obj);
        }

    }

    static private class MyNativeJavaObject extends NativeJavaObject {
        private MyNativeJavaObject(Scriptable scope, Object javaObject, Class<?> staticType, boolean isAdapter) {
            super(scope, javaObject, staticType, isAdapter);
        }

        @Override
        public String getClassName() {
            return JSBaseObject.getClassName(this.staticType);
        }
    }

    private static class MyNativeJavaClass extends NativeJavaClass {
        public MyNativeJavaClass(Scriptable scriptable, Class<? extends Scriptable> aClass) {
            super(scriptable, aClass);
        }

        @Override
        public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
            return super.construct(cx, scope, args);
        }
    }
}
