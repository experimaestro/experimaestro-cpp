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


import com.google.common.collect.ImmutableList;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import net.bpiwowar.xpm.manager.json.Json;
import net.bpiwowar.xpm.manager.json.JsonArray;
import net.bpiwowar.xpm.manager.plans.FunctionOperator;
import net.bpiwowar.xpm.manager.plans.NAryOperator;
import net.bpiwowar.xpm.manager.plans.Operator;
import net.bpiwowar.xpm.manager.plans.ProductReference;
import net.bpiwowar.xpm.manager.plans.UnaryOperator;
import net.bpiwowar.xpm.manager.plans.functions.Function;
import net.bpiwowar.xpm.manager.scripting.Expose;
import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.manager.scripting.LanguageContext;
import net.bpiwowar.xpm.manager.scripting.ScriptContext;
import net.bpiwowar.xpm.utils.JSUtils;

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
