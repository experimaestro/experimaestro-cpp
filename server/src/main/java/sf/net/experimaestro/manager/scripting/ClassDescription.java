package sf.net.experimaestro.manager.scripting;

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

import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.manager.js.JSBaseObject;
import sf.net.experimaestro.manager.js.JSObjectDescription;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

/**
 * An abstract class description
 */
public class ClassDescription {
    final static private HashMap<Class<?>, ClassDescription> CLASS_DESCRIPTION = new HashMap<>();
    /**
     * Base class (real class or wrapped class)
     */
    final Class<?> wrappedClass;

    /**
     * Class name
     */
    String classname;

    private ArrayList<Constructor<?>> constructors = new ArrayList<>();

    private Map<String, ArrayList<Method>> methods = new HashMap<>();

    private Map<String, Field> fields = new HashMap<>();

    public ClassDescription(Class<?> theClass) {
        this.wrappedClass = theClass;
    }

    /**
     * Analyze a class and returns the multi-map of names to methods
     * <p>
     * Any constructor annotated with JSFunction is a valid functoin
     *
     * @param aClass
     * @return
     */
    public static ClassDescription analyzeClass(Class<?> aClass) {
        ClassDescription description = CLASS_DESCRIPTION.get(aClass);
        Map<String, ArrayList<Method>> map;
        synchronized (CLASS_DESCRIPTION) {
            if (description == null) {
                description = new ClassDescription(aClass);
                description.classname = getClassName(aClass);
                CLASS_DESCRIPTION.put(aClass, description);

                map = description.methods;
                for (Method method : aClass.getDeclaredMethods()) {
                    // Js function case
                    final Expose function = method.getAnnotation(Expose.class);

                    if (function != null) {
                        addMethod(map, method, function.value(), function.call());
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
                if (JSBaseObject.class.isAssignableFrom(superclass) || superclass.getAnnotation(Exposed.class) != null) {
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
                    final Expose annotation = constructor.getAnnotation(Expose.class);
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
                    Property annotation = field.getAnnotation(Property.class);
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
    public static String getClassName(Class<?> aClass) {
        JSObjectDescription annotation = aClass.getAnnotation(JSObjectDescription.class);
        if (annotation != null && !"".equals(annotation.name()))
            return annotation.name();

        Exposed exposed = aClass.getAnnotation(Exposed.class);
        if (exposed != null) {
            return aClass.getSimpleName();
        }

        if (!aClass.getSimpleName().startsWith("JS")) {
            throw new AssertionError(format("Class %s is not exposed (or does not start with JS [deprecated]) as it should", aClass.getName()));
        }

        return aClass.getSimpleName().substring(2);
    }

    /**
     * The class methods
     */
    public Map<String, ArrayList<Method>> getMethods() {
        return methods;
    }

    /**
     * Properties
     */
    public Map<String, Field> getFields() {
        return fields;
    }

    /**
     * The constructors
     */
    public ArrayList<Constructor<?>> getConstructors() {
        return constructors;
    }

    public Class<?> getWrappedClass() {
        return wrappedClass;
    }

    public String getClassName() {
        return classname;
    }
}
