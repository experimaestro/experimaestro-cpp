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


import com.google.common.collect.ImmutableList;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.manager.json.JsonArray;
import sf.net.experimaestro.manager.plans.FunctionOperator;
import sf.net.experimaestro.manager.plans.NAryOperator;
import sf.net.experimaestro.manager.plans.Operator;
import sf.net.experimaestro.manager.plans.ProductReference;
import sf.net.experimaestro.manager.plans.UnaryOperator;
import sf.net.experimaestro.manager.plans.functions.Function;
import sf.net.experimaestro.manager.scripting.Expose;
import sf.net.experimaestro.manager.scripting.Exposed;
import sf.net.experimaestro.manager.scripting.LanguageContext;
import sf.net.experimaestro.manager.scripting.ScriptContext;
import sf.net.experimaestro.utils.JSUtils;

import java.util.Iterator;
import java.util.Map;

/**
 * JS function to transform inputs in a operators
 */
@Exposed
public class JSTransform implements Function {
    protected final Scriptable scope;
    protected final Callable f;
    private final Context cx;

    @Expose(context = true)
    public JSTransform(JavaScriptContext jcx, Object f, Operator[] operators) {
        this.cx = jcx.context();
        this.scope = jcx.scope();
        this.f = (Callable) f;
    }

    @Override
    public String toString() {
        return "JS function";
    }

    public Iterator<Json> apply(Json[] parameters) {
        final Object call = f.call(cx, scope, null, parameters);
        Json result = JSUtils.toJSON(scope, call);

        if (result instanceof JsonArray)
            return ((JsonArray) result).iterator();

        return ImmutableList.of(result).iterator();
    }
}
