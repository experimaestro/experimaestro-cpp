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

import bpiwowar.argparser.utils.Output;
import com.google.common.collect.AbstractIterator;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.apache.commons.lang.ArrayUtils;

import javax.xml.xpath.XPathExpressionException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Order the input using some operators
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 21/2/13
 */
public class OrderBy extends UnaryOperator {
    Order<Operator> order;
    int contextOrder[];

    public OrderBy(Order<Operator> order) {
        this.order = order;
    }



    @Override
    protected Iterator<ReturnValue> _iterator() {
        return new AbstractIterator<ReturnValue>() {
            public Iterator<Value> iterator;

            @Override
            protected ReturnValue computeNext() {
                if (iterator == null) {
                    ObjectArrayList<Value> list = new ObjectArrayList<>(new Value[0]);
                    {
                        Iterator<Value> iterator = input.iterator();
                        while (iterator.hasNext()) {
                            list.add(iterator.next());
                        }
                    }

                    Value values[] = list.toArray(new Value[list.size()]);
                    Arrays.sort(values, new Comparator<Value>() {
                        @Override
                        public int compare(Value o1, Value o2) {
                            for (int index : contextOrder) {
                                int z = Long.compare(o1.context[index], o2.context[index]);
                                if (z != 0)
                                    return z;
                            }
                            return 0;
                        }
                    });
                    iterator = Arrays.asList(values).iterator();
                }
                if (iterator.hasNext()) {
                    Value value = iterator.next();
                    return new ReturnValue(new DefaultContexts(value.context), value.nodes);
                }
                return endOfData();
            }
        };
    }


    @Override
    protected String getName() {
        if (contextOrder != null) {
            return String.format("OrderBy (%s)", Output.toString(", ", ArrayUtils.toObject(contextOrder)));
        }
        return String.format("OrderBy (%d contexts)", order.size());
    }

    @Override
    protected void doPostInit(List<Map<Operator, Integer>> parentStreams) throws XPathExpressionException {
        IntArrayList contextOrder = new IntArrayList();
        Map<Operator, Integer> inputContext = parentStreams.get(0);
        // TODO: We should flatten taking the parent order into account (possibly the parents of
        // operators sharing the same)
        order.flatten();
        for (Operator operator : order.items()) {
            Integer contextIndex = inputContext.get(operator);
            contextOrder.add(contextIndex);
        }
        this.contextOrder = contextOrder.toIntArray();
    }

    @Override
    protected void ensureConnections(HashMap<Operator, Operator> simplified) {
        for(Set<Operator> set: order.list) {
            Operator.ensureConnections(simplified, set);
        }
    }

    @Override
    protected void addNeededStreams(Collection<Operator> streams) {
        super.addNeededStreams(streams);

        // Add all the operators that we need to sort
        for (Operator operator : order.items())
            if (!streams.contains(operator))
                streams.add(operator);
    }

    @Override
    protected Order<Operator> doComputeOrder(Order<Operator> childrenOrder) {
        // TODO: we might do better by adding some information from children orders?
        return order;
    }


}
