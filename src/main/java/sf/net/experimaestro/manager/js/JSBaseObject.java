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
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.exceptions.XPMRhinoException;
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

    final static private HashMap<Class<?>, ClassDescription> CLASS_DESCRIPTION = new HashMap<>();

    private ClassDescription classDescription;
    private Scriptable prototype;
    private Scriptable parentScope;

    public JSBaseObject() {
        final Class<? extends JSBaseObject> aClass = getClass();
        this.classDescription = analyzeClass(aClass);
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

                        ArrayList<Method> methods = map.get(jsName);
                        if (methods == null) {
                            map.put(jsName, methods = new ArrayList<>());
                        }
                        methods.add(method);
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
                    }
                }
                if (description.constructors.isEmpty()) {
                    try {
                        description.constructors.add(aClass.getConstructor());
                    } catch (NoSuchMethodException e) {
                        throw new ExperimaestroRuntimeException("Could not find any constructor in %s", aClass);
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
     * Returns the class name
     */
    static String getClassName(Class<?> aClass) {
        JSObjectDescription annotation = aClass.getAnnotation(JSObjectDescription.class);
        if (annotation != null && !"".equals(annotation.name()))
            return annotation.name();

        if (!aClass.getSimpleName().startsWith("JS"))
            throw new AssertionError(format("Class %s does not start with JS as it should", aClass.getName()));
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
            function.add(this, methods);

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

    /**
     * The Experimaestro wrap factory to handle special cases
     */
    static public class XPMWrapFactory extends WrapFactory {
        public final static XPMWrapFactory INSTANCE = new XPMWrapFactory();

        private XPMWrapFactory() {
            setJavaPrimitiveWrap(false);
        }

        @Override
        public Object wrap(Context cx, Scriptable scope, Object obj, Class<?> staticType) {
            if (obj instanceof FileObject)
                return new JSFileObject(XPMObject.getXPMObject(scope), (FileObject) obj);
            if (obj instanceof Node) {
                return new JSNode((Node) obj);
            }
            if (obj instanceof Json) {
                return new JSJson((Json) obj);
            }
            if (obj instanceof Resource) {
                return new JSResource((Resource) obj);
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
                return new JSJson((Json) obj);
            }

            if (obj instanceof Node)
                return new JSNode((Node) obj);

            if (obj instanceof FileObject)
                return new JSFileObject(XPMObject.getXPMObject(scope), (FileObject) obj);

            if (obj instanceof Resource)
                return new JSResource((Resource) obj);


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
