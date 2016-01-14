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

import net.bpiwowar.xpm.exceptions.XPMRhinoException;
import net.bpiwowar.xpm.manager.scripting.ExposeMode;
import net.bpiwowar.xpm.manager.scripting.MethodFunction;
import net.bpiwowar.xpm.manager.scripting.Wrapper;
import net.bpiwowar.xpm.utils.JSUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * A wrapper for exposed java objects
 */
public class JavaScriptObject extends JSBaseObject implements Wrapper {
    private final Object object;

    public JavaScriptObject(Object object) {
        super(object.getClass());
        this.object = object;
    }

    @Override
    public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        MethodFunction function = getMethodFunction(ExposeMode.CALL);
        if (function.isEmpty()) {
            throw new XPMRhinoException("Cannot call object of type %s", getClassName());
        }
        JavaScriptContext jcx = new JavaScriptContext(cx, scope);
        return JavaScriptRunner.wrap(jcx, function.call(jcx, object, null, args));
    }

    @Override
    public Object unwrap() {
        return object instanceof Wrapper ? ((Wrapper) object).unwrap() : object;
    }

    @Override
    protected Object thisObject() {
        return this.object;
    }

    @Override
    public Object getDefaultValue(Class<?> hint) {
        return getClassName();
    }

    @Override
    public String toString() {
        return unwrap().toString();
    }
}
