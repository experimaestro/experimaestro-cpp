/*
 * This file is part of experimaestro.
 * Copyright (c) 2013 B. Piwowarski <benjamin@bpiwowar.net>
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

package sf.net.experimaestro.manager.plans;

import com.google.common.collect.AbstractIterator;
import org.w3c.dom.Document;
import sf.net.experimaestro.utils.log.Logger;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Cartesian product of inputs
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 20/2/13
 */
public class Product extends NAryOperator {
    final static private Logger LOGGER = Logger.getLogger();

    @Override
    protected Iterator<ReturnValue> _iterator() {
        return new ProductIterator();
    }


    @Override
    protected String getName() {
        return "product";
    }


    public abstract class AbstractProductIterator extends AbstractIterator<ReturnValue> {
        final Iterator<Value>[] inputs;
        boolean first;
        Value[] current;

        public AbstractProductIterator() {
            inputs = new Iterator[parents.size()];
            for (int i = 0; i < parents.size(); i++)
                inputs[i] = parents.get(i).iterator();
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
            assert value.nodes.length == 1;
            current[i] = value;
            if (LOGGER.isTraceEnabled())
                LOGGER.trace("New token: [%d] %d: %s", i, value.id, Arrays.toString(value.context));
            return true;
        }

        ReturnValue getReturnValue(Value[] current) {
            Document[] nodes = new Document[parents.size()];
            final long[][] contexts = new long[parents.size()][];
            for (int j = 0; j < contexts.length; j++) {
                contexts[j] = current[j].context;
                nodes[j] = current[j].nodes[0];
            }

            return new ReturnValue(new DefaultContexts(contexts), nodes);
        }
    }

    private class ProductIterator extends AbstractProductIterator {
        @Override
        protected ReturnValue computeNext() {
            // First loop
            if (first)
                if (computeFirst()) return getReturnValue(current);
                else return endOfData();

            for (int i = 0; i < parents.size(); i++) {
                if (next(i)) {
                    for (int j = i; --j >= 0; ) {
                        inputs[j] = parents.get(j).iterator();
                        next(j);
                    }

                    return getReturnValue(current);
                }
            }

            return endOfData();
        }

    }


}
