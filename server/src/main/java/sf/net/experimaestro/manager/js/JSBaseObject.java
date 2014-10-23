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
import org.apache.commons.vfs2.FileObject;
import org.mozilla.javascript.*;
import org.w3c.dom.Node;
import sf.net.experimaestro.annotations.Expose;
import sf.net.experimaestro.annotations.Exposed;
import sf.net.experimaestro.exceptions.XPMRhinoException;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.scheduler.Resource;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

/**
 * Base class for all JS objects implementations
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 27/11/12
 */
abstract public class JSBaseObject implements Scriptable, JSConstructable, Callable {
    final static private HashMap<Class<?>, ClassDescription> CLASS_DESCRIPTION = new HashMap<>();
    private XPMObject xpm;

    private ClassDescription classDescription;
    private Scriptable prototype;
    private Scriptable parentScope;

    public JSBaseObject() {
        final Class<? extends JSBaseObject> aClass = getClass();
        this.classDescription = analyzeClass(aClass);
    }

    public JSBaseObject(Class<?> wrappedClass) {
        this.classDescription = analyzeClass(wrappedClass);
    }

    /**
     * Create a new JS object
     */
    public static <T> T newObject(Context cx, Scriptable scope, Class<T> aClass, Object... args) {
        return (T) cx.newObject(scope, getClassName(aClass), args);
    }

    /**
     * Get the XPM object (thread)
     */
    final static XPMObject xpm() {
        return XPMObject.getThreadXPM();
    }

    /**
     * Analyze a class and returns the multi-map of names to methods
     * <p/>
     * Any constructor annotated with JSFunction is a valid functoin
     *
     * @param aClass
     * @return
     */
    static ClassDescription analyzeClass(Class<?> aClass) {
        ClassDescription description = CLASS_DESCRIPTION.get(aClass);
        Map<String, ArrayList<Method>> map;
        synchronized (CLASS_DESCRIPTION) {
            if (description == null) {
                description = new ClassDescription();
                CLASS_DESCRIPTION.put(aClass, description);

                map = description.methods;
                for (Method method : aClass.getDeclaredMethods()) {
                    // Js function case
                    final JSFunction jsFunction = method.getAnnotation(JSFunction.class);

                    if (jsFunction != null) {
                        addMethod(map, method, jsFunction.value(), jsFunction.call());
                        continue;
                    }

                    // Exposed case
                    final Expose expose = method.getAnnotation(Expose.class);
                    if (expose != null) {
                        addMethod(map, method, expose.value(), false);
                        continue;
                    }


                }

                // Adds all the ancestors methods
                Class<?> superclass = aClass.getSuperclass();
                if (JSBaseObject.class.isAssignableFrom(superclass)) {
                    Map<String, ArrayList<Method>> superclassMethods = analyzeClass(superclass).methods;
                    for (Map.Entry<String, ArrayList<Method>> entry : superclassMethods.entrySet()) {
                        ArrayList<Method> previous = map.get(entry.getKey());
                        if (previous != null)
                            previous.addAll(entry.getValue());
                        else
                            map.put(entry.getKey(), entry.getValue());
                    }
                }

                // Add constructors
                for (Constructor<?> constructor : aClass.getConstructors()) {
                    final JSFunction annotation = constructor.getAnnotation(JSFunction.class);
                    if (annotation != null) {
                        description.constructors.add(constructor);
                        continue;
                    }

                    // Exposed case
                    final Expose expose = constructor.getAnnotation(Expose.class);
                    if (expose != null) {
                        description.constructors.add(constructor);
                        continue;
                    }

                }
                if (description.constructors.isEmpty() && JSBaseObject.class.isAssignableFrom(aClass)) {
                    try {
                        description.constructors.add(aClass.getConstructor());
                    } catch (NoSuchMethodException e) {
                        throw new XPMRuntimeException("Could not find any constructor in %s", aClass);
                    }
                }


                // Add fields
                for (Field field : aClass.getFields()) {
                    JSProperty annotation = field.getAnnotation(JSProperty.class);
                    if (annotation != null) {
                        final String name = annotation.value().equals("") ? field.getName() : annotation.value();
                        description.fields.put(name, field);
                    }
                }

            }
        }
        return description;
    }

    /**
     * @param map    The list of methods
     * @param method The method
     * @param name   The name of the method (or empty if using the method name)
     * @param call   Whether this method is used when the object is "called"
     */
    private static void addMethod(Map<String, ArrayList<Method>> map, Method method, String name, boolean call) {
        if ((method.getModifiers() & Modifier.PUBLIC) == 0)
            throw new AssertionError("The method " + method + " is not public");
        if (call) {
            if (!name.equals("")) {
                throw new AssertionError("Method " + method + " should not defined a name since" +
                        "it is will be called directly.");
            }
            name = null;
        } else if ("".equals(name)) {
            name = method.getName();
        }

        ArrayList<Method> methods = map.get(name);
        if (methods == null) {
            map.put(name, methods = new ArrayList<>());
        }
        methods.add(method);
    }

    /**
     * Returns the class name
     */
    static String getClassName(Class<?> aClass) {
        JSObjectDescription annotation = aClass.getAnnotation(JSObjectDescription.class);
        if (annotation != null && !"".equals(annotation.name()))
            return annotation.name();

        Exposed exposed = aClass.getAnnotation(Exposed.class);
        if (exposed != null) {
            return aClass.getSimpleName();
        }

        if (!aClass.getSimpleName().startsWith("JS")) {
            throw new AssertionError(format("Class %s does not start with JS as it should", aClass.getName()));
        }

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
     * @throws InstantiationException
     */
    public static void defineClass(Scriptable scope, Class<?> aClass) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        if (Scriptable.class.isAssignableFrom(aClass)) {
            // If not a JSObject descendent, we handle this with standard JS procedure
            if (JSConstructable.class.isAssignableFrom(aClass)) {
                // Use our own constructor
                final String name = JSBaseObject.getClassName(aClass);
                scope = ScriptableObject.getTopLevelScope(scope);
                final NativeJavaClass nativeJavaClass = new MyNativeJavaClass(scope, (Class<? extends Scriptable>) aClass);
                scope.put(name, scope, nativeJavaClass);
            } else {
                ScriptableObject.defineClass(scope, (Class<? extends Scriptable>) aClass);
            }
        } else {
            final String name = JSBaseObject.getClassName(aClass);
            scope.put(name, scope, new WrappedJavaObject.WrappedClass(aClass));
        }
    }


    @Override
    public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        MethodFunction function = getMethodFunction(null);
        if (function.isEmpty())
            throw new XPMRhinoException("Cannot call object of type %s", getClassName());
        return function.call(cx, scope, thisObj, args);
    }

    @Override
    public String getClassName() {
        return JSBaseObject.getClassName(this.getClass());
    }

    @Override
    public Object get(String name, Scriptable start) {
        // Search for a function
        MethodFunction function = getMethodFunction(name);
        if (!function.isEmpty())
            return function;

        // Search for a property
        final Field field = classDescription.fields.get(name);
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

        ArrayList<Method> methods = this.classDescription.methods.get(name);
        if (methods != null && !methods.isEmpty())
            function.add(thisObject(), methods);

        // Walk the prototype chain
        for (Scriptable prototype = getPrototype(); prototype != null; prototype = prototype.getPrototype()) {
            if (prototype instanceof JSBaseObject) {
                JSBaseObject jsPrototype = (JSBaseObject) prototype;
                methods = jsPrototype.classDescription.methods.get(name);
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
        return classDescription.methods.containsKey(name) || classDescription.fields.containsKey(name);
    }

    @Override
    public boolean has(int index, Scriptable start) {
        return false;
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        final Field field = classDescription.fields.get(name);
        if (field != null) {
            if (classDescription.fields.containsKey(name)) {
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

    static public class ClassDescription {
        /**
         * The constructors
         */
        ArrayList<Constructor<?>> constructors = new ArrayList<>();

        /**
         * The class methods
         */
        Map<String, ArrayList<Method>> methods = new HashMap<>();

        /**
         * Properties
         */
        Map<String, Field> fields = new HashMap<>();
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
            if (obj instanceof FileObject)
                return new JSFileObject((FileObject) obj).setXPM(XPMObject.getXPM(scope));
            if (obj instanceof Node) {
                return new JSNode((Node) obj).setXPM(XPMObject.getXPM(scope));
            }
            if (obj instanceof Json) {
                return new JSJson((Json) obj).setXPM(XPMObject.getXPM(scope));
            }
            if (obj instanceof Resource) {
                return new JSResource((Resource) obj).setXPM(XPMObject.getXPM(scope));
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

            if (obj instanceof FileObject) {
                return new JSFileObject((FileObject) obj).setXPM(XPMObject.getXPM(scope));
            }

            if (obj instanceof Resource)
                return new JSResource((Resource) obj).setXPM(XPMObject.getXPM(scope));


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
            ClassDescription description = analyzeClass((Class) javaObject);
            String className = JSBaseObject.getClassName((Class) javaObject);
            ConstructorFunction constructorFunction = new ConstructorFunction(className, description.constructors);
            Object object = constructorFunction.call(cx, scope, null, args);

            return (Scriptable) object;

        }
    }
}
