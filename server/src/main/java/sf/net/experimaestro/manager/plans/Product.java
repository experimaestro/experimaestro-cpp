package sf.net.experimaestro.manager.plans;

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
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.manager.scripting.Exposed;
import sf.net.experimaestro.manager.scripting.ScriptContext;
import sf.net.experimaestro.utils.log.Logger;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Cartesian product of inputs
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Exposed
public class Product extends NAryOperator {
    final static private Logger LOGGER = Logger.getLogger();

    @Override
    protected Operator doCopy(boolean deep, Map<Object, Object> map) {
        Product copy = new Product();
        return super.copy(deep, map, copy);
    }

    @Override
    protected Iterator<ReturnValue> _iterator(ScriptContext scriptContext) {
        return new ProductIterator(scriptContext);
    }


    @Override
    protected String getName() {
        return "product";
    }

    @Override
    protected void doPostInit(List<Map<Operator, Integer>> parentStreams) {
        super.doPostInit(parentStreams);

        // Computes the output value
        outputSize = 0;
        for (Operator parent : parents)
            outputSize += parent.outputSize();
    }

    public abstract class AbstractProductIterator extends AbstractIterator<ReturnValue> {
        final Iterator<Value>[] inputs;
        final ScriptContext scriptContext;
        boolean first;
        Value[] current;

        public AbstractProductIterator(ScriptContext scriptContext) {
            this.scriptContext = scriptContext;
            inputs = new Iterator[parents.size()];
            for (int i = 0; i < parents.size(); i++)
                inputs[i] = parents.get(i).iterator(scriptContext);
            first = true;
            current = new Value[inputs.length];
        }

        boolean computeFirst() {
            if (first) {
                for (int i = 0; i < parents.size(); i++) {
                    if (!next(i))
                        return false;
                }
                first = false;
            }
            return true;
        }

        boolean next(int i) {
            if (!inputs[i].hasNext())
                return false;
            final Value value = inputs[i].next();
            current[i] = value;
            if (LOGGER.isTraceEnabled())
                LOGGER.trace("New token: [%d] %d: %s", i, value.id, Arrays.toString(value.context));
            return true;
        }

        /**
         * Compute the next value to return
         */
        ReturnValue getReturnValue(Value[] current) {
            Json[] nodes = new Json[Product.this.outputSize()];
            final long[][] contexts = new long[parents.size()][];
            int offset = 0;
            for (int j = 0; j < contexts.length; j++) {
                contexts[j] = current[j].context;
                for (int k = 0, n = current[j].nodes.length; k < n; k++) {
                    nodes[offset] = current[j].nodes[k];
                    assert nodes[offset] != null;
                    offset++;
                }

            }

            return new ReturnValue(new DefaultContexts(contexts), nodes);
        }
    }

    class ProductIterator extends AbstractProductIterator {
        public ProductIterator(ScriptContext scriptContext) {
            super(scriptContext);
        }

        @Override
        protected ReturnValue computeNext() {
            // First loop
            if (first)
                if (computeFirst()) return getReturnValue(current);
                else return endOfData();

            for (int i = 0; i < parents.size(); i++) {
                if (next(i)) {
                    for (int j = i; --j >= 0; ) {
                        inputs[j] = parents.get(j).iterator(scriptContext);
                        next(j);
                    }

                    return getReturnValue(current);
                }
            }

            return endOfData();
        }

    }


}
