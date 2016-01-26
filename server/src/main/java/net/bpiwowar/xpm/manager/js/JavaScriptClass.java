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

import net.bpiwowar.xpm.manager.scripting.ClassDescription;
import net.bpiwowar.xpm.manager.scripting.MethodFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaClass;
import org.mozilla.javascript.Scriptable;

/**
 * A javascript object representing a class
 */
public class JavaScriptClass extends NativeJavaClass {
    private final ClassDescription description;

    public JavaScriptClass(ClassDescription description) {
        this.description = description;
    }

    @Override
    public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
        final JavaScriptContext jcx = new JavaScriptContext(cx, scope);
        Object object = description.getConstructors().call(jcx, null, null, args);
        return new JavaScriptObject(object, description);
    }

    @Override
    public Object get(String name, Scriptable start) {
        // Search for a function
        MethodFunction method = description.getMethods().get(name);
        if (!method.isEmpty()) {
            return new JavaScriptFunction(null, method);
        }

        return NOT_FOUND;
    }


}
