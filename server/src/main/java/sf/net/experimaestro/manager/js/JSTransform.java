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
import sf.net.experimaestro.manager.plans.Operator;
import sf.net.experimaestro.manager.plans.ProductReference;
import sf.net.experimaestro.manager.plans.functions.Function;
import sf.net.experimaestro.manager.scripting.Expose;
import sf.net.experimaestro.utils.JSUtils;

import java.util.Iterator;

/**
 * JS function to transform inputs in a operators
 */
public class JSTransform extends JSAbstractOperator implements Function {
    protected final Context cx;
    protected final Scriptable scope;
    protected final Callable f;

    private final FunctionOperator operator;

    @Expose
    public JSTransform(Context cx, Scriptable scope, Callable f, JSAbstractOperator[] operators) {
        this.cx = cx;
        this.scope = scope;
        this.f = f;

        Operator inputOperator;
        if (operators.length == 1)
            inputOperator = operators[0].getOperator();
        else {
            ProductReference pr = new ProductReference();
            for (JSAbstractOperator operator : operators)
                pr.addParent(operator.getOperator());
            inputOperator = pr;
        }

        operator = new FunctionOperator(this);
        operator.addParent(inputOperator);

    }

    @Override
    public String toString() {
        return "JS function";
    }

    public Iterator<Json> apply(Json[] parameters) {
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++)
            args[i] = new JSJson(parameters[i]);
        Json result = JSUtils.toJSON(scope, f.call(cx, scope, null, args));

        if (result instanceof JsonArray)
            return ((JsonArray) result).iterator();

        return ImmutableList.of(result).iterator();
    }

    @Override
    Operator getOperator() {
        return operator;
    }
}
