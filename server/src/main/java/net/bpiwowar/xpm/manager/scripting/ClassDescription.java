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
import net.bpiwowar.xpm.utils.Functional;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static net.bpiwowar.xpm.manager.scripting.Functions.format;

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

    /**
     * Super class (or null)
     */
    ClassDescription superDescription;

    private ConstructorFunction constructors;

    /**
     * Maps a string or ExposeMode enum value to a list of methods
     */
    private Map<Object, MethodFunction> methods = new HashMap<>();

    private Map<String, PropertyAccess> fields = new HashMap<>();

    private <T> ClassDescription(Class<T> aClass) {
        final Exposed exposed = aClass.getAnnotation(Exposed.class);
        if (exposed == null) {
            throw new AssertionError(format("Class %s is not exposed", aClass));
        }
        classname = exposed.value().isEmpty() ? aClass.getSimpleName() : exposed.value();

        this.wrappedClass = aClass;

        // Add fields
        for (Field field : aClass.getDeclaredFields()) {
            Property annotation = field.getAnnotation(Property.class);
            if (annotation != null) {
                final String name = annotation.value().equals("") ? field.getName() : annotation.value();
                fields.put(name, new PropertyAccess.FieldAccess(field));
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
                        PropertyAccess propertyAccess = fields.get(name);
                        if (propertyAccess == null) {
                            fields.put(name, propertyAccess = new PropertyAccess());
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
                        addMethod(method, expose.value(), expose.mode());
                        break;
                }
            }
        }

        // Adds all the ancestors methods
        Class<?> superclass = aClass.getSuperclass();
        if (superclass != null && superclass.getAnnotation(Exposed.class) != null) {
            this.superDescription = analyzeClass(superclass);

            // Add methods
            superDescription.getMethods().forEach((k, m) -> {
                final MethodFunction methodFunction = methods.get(k);
                if (methodFunction == null) {
                    methods.put(k, methodFunction);
                } else {
                    methodFunction.add(m);
                }
            });
        }

        // Add constructors
        ArrayList<Constructor<?>> list = new ArrayList<>();
        for (Constructor<?> constructor : aClass.getConstructors()) {
            final Expose expose = constructor.getAnnotation(Expose.class);
            if (expose != null) {
                list.add(constructor);
            }
        }

        constructors = new ConstructorFunction(classname, list);


    }

    /**
     * Analyze a class and returns the multi-map of names to methods
     *
     * @param aClass The class to analyze
     * @return The object representing the class for scripting languages
     */
    public static ClassDescription analyzeClass(Class<?> aClass) {
        ClassDescription description = CLASS_DESCRIPTION.get(aClass);

        synchronized (CLASS_DESCRIPTION) {
            if (description == null) {
                CLASS_DESCRIPTION.put(aClass, description = new ClassDescription(aClass));
            }
        } // synchronized
        return description;
    }

    /**
     * @param method The method
     * @param name   The name of the method (or empty if using the method name)
     * @param mode   Whether this method is used when the object is "called"
     */
    private void addMethod(Method method, String name, ExposeMode mode) {
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
        addMethod(method, key);
    }

    private void addMethod(Method method, Object key) {
        MethodFunction methods = this.methods.get(key);
        if (methods == null) {
            this.methods.put(key, methods = new MethodFunction(key));
        }
        methods.add(Collections.singleton(method));
    }

    /**
     * The class methods
     */
    public Map<Object, MethodFunction> getMethods() {
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
    public ConstructorFunction getConstructors() {
        return constructors;
    }

    public Class<?> getWrappedClass() {
        return wrappedClass;
    }

    /**
     * Returns the class name for a script
     *
     * @return A string representing the class
     */
    public String getClassName() {
        return classname;
    }

    public MethodFunction getMethod(Object key) {
        final MethodFunction methodFunction = methods.get(key);
        if (methodFunction == null && superDescription != null) {
            return superDescription.getMethod(key);
        }
        return methodFunction;
    }
}
