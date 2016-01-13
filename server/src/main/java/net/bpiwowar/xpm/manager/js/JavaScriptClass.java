package net.bpiwowar.xpm.manager.js;

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

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaClass;
import org.mozilla.javascript.Scriptable;
import net.bpiwowar.xpm.manager.scripting.ClassDescription;
import net.bpiwowar.xpm.manager.scripting.ConstructorFunction;
import net.bpiwowar.xpm.manager.scripting.MethodFunction;

import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * A javascript object representing a class
 */
public class JavaScriptClass extends NativeJavaClass {
    private final ClassDescription description;
    private Class<?> javaClass;

    public JavaScriptClass(Class<?> javaClass) {
        this.description = ClassDescription.analyzeClass(javaClass);
        this.javaClass = javaClass;
    }

    @Override
    public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
        ClassDescription description = ClassDescription.analyzeClass((Class) javaClass);
        String className = ClassDescription.getClassName((Class) javaClass);
        ConstructorFunction constructorFunction = new ConstructorFunction(className, description.getConstructors());
        final JavaScriptContext jcx = new JavaScriptContext(cx, scope);
        Object object = constructorFunction.call(jcx, null, null, args);
        return new JavaScriptObject(object);
    }

    @Override
    public Object get(String name, Scriptable start) {
        // Search for a function
        ArrayList<Method> function = description.getMethods().get(name);
        if (!function.isEmpty()) {
            final MethodFunction method = new MethodFunction(name);
            method.add(function);
            return new JavaScriptFunction(null, method);
        }

        return NOT_FOUND;
    }


}
