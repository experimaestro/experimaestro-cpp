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

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaClass;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;
import sf.net.experimaestro.manager.scripting.ClassDescription;
import sf.net.experimaestro.manager.scripting.ConstructorFunction;

/**
 * An object with exposed fields
 */
@JSObjectDescription(name = "@WrappedJavaObject")
public class JavaScriptObject extends JSBaseObject implements Wrapper {
    private final Object object;

    public JavaScriptObject(Context cx, Scriptable scope, Object object) {
        super(object.getClass());
        this.object = object;
    }

    @Override
    public Object unwrap() {
        return object;
    }

    @Override
    protected Object thisObject() {
        return this.object;
    }

    @Override
    public Object getDefaultValue(Class<?> hint) {
        return getClassName();
    }

    public static class WrappedClass extends NativeJavaClass {
        private Class<?> javaClass;

        public WrappedClass(Class<?> javaClass) {
            this.javaClass = javaClass;
        }

        @Override
        public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
            ClassDescription description = ClassDescription.analyzeClass((Class) javaClass);
            String className = ClassDescription.getClassName((Class) javaClass);
            ConstructorFunction constructorFunction = new ConstructorFunction(className, description.getConstructors());
            JavaScriptContext jcx = new JavaScriptContext(cx, scope);
            Object object = constructorFunction.call(jcx, /* FIXME */ null, null, args);

            return new JavaScriptObject(cx, scope, object);

        }

    }
}
