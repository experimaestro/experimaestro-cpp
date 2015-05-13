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

import org.apache.commons.lang.NotImplementedException;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import sf.net.experimaestro.manager.scripting.MethodFunction;

/**
 * Wrapper for a function in javascript
 */
public class JavaScriptFunction implements Function {
    private final MethodFunction function;

    public JavaScriptFunction(MethodFunction function) {
        this.function = function;
    }

    @Override
    public Object call(Context context, Scriptable scope, Scriptable thisObj, Object[] objects) {
        JavaScriptContext jcx = new JavaScriptContext(context, scope);
        XPMObject xpm = XPMObject.getThreadXPM();
        return function.call(jcx, xpm != null ? xpm.getScriptContext() : null, thisObj, objects);
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
        throw new NotImplementedException();
    }

    @Override
    public Object get(int index, Scriptable start) {
        throw new NotImplementedException();
    }

    @Override
    public boolean has(String name, Scriptable start) {
        return false;
    }

    @Override
    public boolean has(int index, Scriptable start) {
        throw new NotImplementedException();
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        throw new NotImplementedException();
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
        return null;
    }

    @Override
    public void setPrototype(Scriptable prototype) {
        throw new NotImplementedException();
    }

    @Override
    public Scriptable getParentScope() {
        throw new NotImplementedException();
    }

    @Override
    public void setParentScope(Scriptable parent) {
        throw new NotImplementedException();
    }

    @Override
    public Object[] getIds() {
        return new Object[]{};
    }

    @Override
    public Object getDefaultValue(Class<?> hint) {
        throw new NotImplementedException();
    }

    @Override
    public boolean hasInstance(Scriptable instance) {
        throw new NotImplementedException();
    }
}
