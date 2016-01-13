package net.bpiwowar.xpm.manager.plans;

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

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableSet;
import net.bpiwowar.xpm.manager.Manager;
import net.bpiwowar.xpm.manager.json.Json;
import net.bpiwowar.xpm.manager.plans.functions.Function;
import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.manager.scripting.ScriptContext;

import java.util.Iterator;
import java.util.Map;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Exposed
public class FunctionOperator extends UnaryOperator {
    Function function;

    public FunctionOperator(ScriptContext scriptContext, Function function) {
        super(scriptContext);
        this.function = function;
    }

    protected FunctionOperator(ScriptContext scriptContext) {
        super(scriptContext);
    }

    @Override
    protected String getName() {
        return "Function " + function;
    }

    @Override
    protected Operator doCopy(boolean deep, Map<Object, Object> map) {
        FunctionOperator copy = new FunctionOperator(ScriptContext.get(), function);
        return super.copy(deep, map, copy);
    }

    @Override
    protected Iterator<ReturnValue> _iterator() {
        return new AbstractIterator<ReturnValue>() {
            Iterator<Value> iterator = input.iterator(scriptContext);
            public Iterator<? extends Json> innerIterator = ImmutableSet.<Json>of().iterator();
            DefaultContexts contexts;

            @Override
            protected ReturnValue computeNext() {
                while (true) {
                    if (innerIterator.hasNext())
                        return new ReturnValue(contexts, Manager.wrap(innerIterator.next()));
                    if (!iterator.hasNext())
                        return endOfData();

                    Value value = iterator.next();
                    innerIterator = function.apply(value.nodes);

                    contexts = new DefaultContexts(value.context);
                }
            }
        };
    }
}
