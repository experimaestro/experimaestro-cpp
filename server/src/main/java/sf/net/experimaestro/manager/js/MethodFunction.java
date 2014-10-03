/*
 * This file is part of experimaestro.
 * Copyright (c) 2013 B. Piwowarski <benjamin@bpiwowar.net>
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

import com.google.common.collect.Iterables;
import org.apache.commons.lang.NotImplementedException;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.AbstractList;
import java.util.ArrayList;

/**
 * Represents all the methods with the same name within the same object
 */
class MethodFunction extends GenericFunction implements org.mozilla.javascript.Function {
    /**
     * Represent all the methods from a given ancestor (or self)
     */
    static class Group {
        final Object thisObject;
        ArrayList<Method> methods = new ArrayList<>();

        Group(Object thisObject, ArrayList<Method> methods) {
            this.thisObject = thisObject;
            this.methods = methods;
        }
    }

    final String name;
    final ArrayList<Group> groups = new ArrayList<>();

    static public class MethodDeclaration extends Declaration<Method> {
        final Object baseObject;
        final Method method;

        public MethodDeclaration(Object baseObject, Method method) {
            super(method);
            this.baseObject = baseObject;
            this.method = method;
        }

        @Override
        public Object invoke(Object[] transformedArgs) throws InvocationTargetException, IllegalAccessException {
            boolean isStatic = (method.getModifiers() & Modifier.STATIC) != 0;
            return method.invoke(isStatic ? null : baseObject, transformedArgs);
        }
    }

    public boolean isEmpty() {
        return groups.isEmpty();
    }

    @Override
    protected String getName() {
        return name;
    }

    @Override
    protected Iterable<MethodDeclaration> declarations() {
        return Iterables.concat(new AbstractList<Iterable<MethodDeclaration>>() {
            @Override
            public Iterable<MethodDeclaration> get(final int index) {
                Group group = groups.get(index);
                return Iterables.transform(group.methods,
                        m -> new MethodDeclaration(group.thisObject, m));
            }

            @Override
            public int size() {
                return groups.size();
            }
        });
    }


    public MethodFunction(String name) {
        this.name = name;
    }

    public void add(Object thisObj, ArrayList<Method> methods) {
        groups.add(new Group(thisObj, methods));
    }

    @Override
    public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
        throw new NotImplementedException();
    }

    @Override
    public String getClassName() {
        return "MethodFunction";
    }

    @Override
    public Object get(String name, Scriptable start) {
        // TODO: implement get
        throw new NotImplementedException();
    }

    @Override
    public Object get(int index, Scriptable start) {
        // TODO: implement get
        throw new NotImplementedException();
    }

    @Override
    public boolean has(String name, Scriptable start) {
        return false;
    }

    @Override
    public boolean has(int index, Scriptable start) {
        // TODO: implement has
        throw new NotImplementedException();
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        // TODO: implement put
        throw new NotImplementedException();
    }

    @Override
    public void put(int index, Scriptable start, Object value) {
        // TODO: implement put
        throw new NotImplementedException();
    }

    @Override
    public void delete(String name) {
        // TODO: implement delete
        throw new NotImplementedException();
    }

    @Override
    public void delete(int index) {
        // TODO: implement delete
        throw new NotImplementedException();
    }

    @Override
    public Scriptable getPrototype() {
        return null;
    }

    @Override
    public void setPrototype(Scriptable prototype) {
        // TODO: implement setPrototype
        throw new NotImplementedException();
    }

    @Override
    public Scriptable getParentScope() {
        // TODO: implement getParentScope
        throw new NotImplementedException();
    }

    @Override
    public void setParentScope(Scriptable parent) {
        // TODO: implement setParentScope
        throw new NotImplementedException();
    }

    @Override
    public Object[] getIds() {
        return new Object[]{};
    }

    @Override
    public Object getDefaultValue(Class<?> hint) {
        // TODO: implement getDefaultValue
        throw new NotImplementedException();
    }

    @Override
    public boolean hasInstance(Scriptable instance) {
        // TODO: implement hasInstance
        throw new NotImplementedException();
    }


}
