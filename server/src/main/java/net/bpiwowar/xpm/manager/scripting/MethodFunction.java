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

import com.google.common.collect.Iterables;
import net.bpiwowar.xpm.exceptions.XPMRuntimeException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Represents all the methods with the same name within the same object
 */
public class MethodFunction extends GenericFunction {
    final static private Logger LOGGER = LogManager.getFormatterLogger();

    final Object key;
    final ArrayList<Group> groups = new ArrayList<>();

    public MethodFunction(Object key) {
        this.key = key;
    }

    public boolean isEmpty() {
        return groups.stream().allMatch(g -> g.getMethods().isEmpty());
    }

    @Override
    public String getKey() {
        return key.toString();
    }

    @Override
    public Iterable<MethodDeclaration> declarations() {
        return Iterables.concat(new AbstractList<Iterable<MethodDeclaration>>() {
            @Override
            public Iterable<MethodDeclaration> get(final int index) {
                Group group = groups.get(index);
                return Iterables.transform(group.getMethods(),
                        m -> new MethodDeclaration(m));
            }

            @Override
            public int size() {
                return groups.size();
            }
        });
    }

    public void add(Collection<Method> methods) {
        groups.add(new Group(methods));
    }


    /**
     * Add all methods from another function
     *
     * @param other The other function
     */
    public void add(MethodFunction other) {
        assert key.equals(other.key);
        this.groups.addAll(other.groups);
    }

    /**
     * Represent all the methods from a given ancestor (or self)
     */
    static class Group {
        private Collection<Method> methods = new ArrayList<>();

        Group(Collection<Method> methods) {
            this.methods = methods;
        }

        public Collection<Method> getMethods() {
            return methods;
        }
    }

    static public class MethodDeclaration extends Declaration<Method> {
        final Method method;

        public MethodDeclaration(Method method) {
            super(method);
            this.method = method;
        }

        @Override
        public Object invoke(Object thisObj, Object[] transformedArgs) throws InvocationTargetException, IllegalAccessException {
            boolean isStatic = (method.getModifiers() & Modifier.STATIC) != 0;
            try {
                if (!isStatic && thisObj == null) {
                    throw new AssertionError("Method " + method.getName() + " is not static, but the object is null");
                }
                return method.invoke(isStatic ? null : thisObj, transformedArgs);
            } catch (InvocationTargetException | IllegalArgumentException e) {
                throw new XPMRuntimeException(e, "Error [%s] while invoking method %s", e, method);
            } catch (XPMRuntimeException e) {
                throw e.addContext("While invoking method %s", method);
            }
        }
    }


}
