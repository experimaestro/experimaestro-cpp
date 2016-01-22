package net.bpiwowar.xpm.manager.scripting;

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

import net.bpiwowar.xpm.exceptions.XPMRuntimeException;
import net.bpiwowar.xpm.manager.js.JSBaseObject;
import net.bpiwowar.xpm.utils.Functional;

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

    /**
     * Maps a string or ExposeMode enum value to a list of methods
     */
    private Map<Object, ArrayList<Method>> methods = new HashMap<>();

    private Map<String, PropertyAccess> fields = new HashMap<>();

    public ClassDescription(Class<?> theClass) {
        this.wrappedClass = theClass;
    }

    /**
     * Analyze a class and returns the multi-map of names to methods
     *
     * @param aClass The class to analyze
     * @return The object representing the class for scripting languages
     */
    public static ClassDescription analyzeClass(Class<?> aClass) {
        ClassDescription description = CLASS_DESCRIPTION.get(aClass);
        Map<Object, ArrayList<Method>> map;
        synchronized (CLASS_DESCRIPTION) {
            if (description == null) {
                description = new ClassDescription(aClass);
                description.classname = getClassName(aClass);
                CLASS_DESCRIPTION.put(aClass, description);

                map = description.methods;
                // Add fields
                for (Field field : aClass.getDeclaredFields()) {
                    Property annotation = field.getAnnotation(Property.class);
                    if (annotation != null) {
                        final String name = annotation.value().equals("") ? field.getName() : annotation.value();
                        description.fields.put(name, new PropertyAccess.FieldAccess(field));
                    }
                }


                for (Method method : aClass.getDeclaredMethods()) {
                    // Exposed case
                    final Expose expose = method.getAnnotation(Expose.class);
                    if (expose != null) {
                        switch (expose.mode()) {
                            case PROPERTY:
                                // Property
                                String name = expose.value();
                                if (name.isEmpty()) {
                                    throw new XPMRuntimeException("Name cannot be empty for a property [%s]", method);
                                }
                                PropertyAccess propertyAccess = description.fields.get(name);
                                if (propertyAccess == null) {
                                    description.fields.put(name, propertyAccess = new PropertyAccess());
                                }
                                final int nbArguments = method.getParameters().length;
                                if (nbArguments == 0) {
                                    // Getter
                                    propertyAccess.getter = Functional.propagateFunction(x -> method.invoke(x));
                                } else if (nbArguments == 1) {
                                    // Setter
                                    propertyAccess.setter = Functional.propagate((x, v) -> method.invoke(x, v));
                                } else {
                                    throw new XPMRuntimeException("Cannot determine if [%s] is a getter or setter", method);
                                }
                                break;

                            default:
                                addMethod(map, method, expose.value(), expose.mode());
                                break;
                        }
                    }
                }

                // Adds all the ancestors methods
                Class<?> superclass = aClass.getSuperclass();
                if (JSBaseObject.class.isAssignableFrom(superclass) || superclass.getAnnotation(Exposed.class) != null) {
                    Map<Object, ArrayList<Method>> superclassMethods = analyzeClass(superclass).methods;
                    for (Map.Entry<Object, ArrayList<Method>> entry : superclassMethods.entrySet()) {
                        ArrayList<Method> previous = map.get(entry.getKey());
                        if (previous != null)
                            previous.addAll(entry.getValue());
                        else
                            map.put(entry.getKey(), entry.getValue());
                    }
                }

                // Add constructors
                for (Constructor<?> constructor : aClass.getConstructors()) {
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

            } // description == null
        } // synchronized
        return description;
    }

    /**
     * @param map    The list of methods
     * @param method The method
     * @param name   The name of the method (or empty if using the method name)
     * @param mode   Whether this method is used when the object is "called"
     */
    private static void addMethod(Map<Object, ArrayList<Method>> map, Method method, String name, ExposeMode mode) {
        if ((method.getModifiers() & Modifier.PUBLIC) == 0)
            throw new AssertionError("The method " + method + " is not public");

        Object key = name;
        switch (mode) {
            case FIELDS:
            case CALL:
            case ITERATOR:
            case LENGTH:
                if (!name.equals("")) {
                    throw new AssertionError("Method " + method + " should not defined a name since" +
                            "it is will be called directly.");
                }
                key = mode;
                break;
            default:
                if (name.isEmpty()) {
                    key = method.getName();
                }

        }

        // Add the method
        addMethod(map, method, key);
    }

    private static void addMethod(Map<Object, ArrayList<Method>> map, Method method, Object key) {
        ArrayList<Method> methods = map.get(key);
        if (methods == null) {
            map.put(key, methods = new ArrayList<>());
        }
        methods.add(method);
    }

    /**
     * Returns the class name
     */
    public static String getClassName(Class<?> aClass) {
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
    public Map<Object, ArrayList<Method>> getMethods() {
        return methods;
    }

    /**
     * Properties
     */
    public Map<String, PropertyAccess> getFields() {
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

    /**
     * Returns the class name for a script
     * @return A string representing the class
     */
    public String getClassName() {
        return classname;
    }
}
