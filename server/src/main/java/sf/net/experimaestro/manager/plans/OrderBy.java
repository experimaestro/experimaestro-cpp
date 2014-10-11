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
import com.google.common.base.Predicate;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.mutable.MutableInt;

import javax.xml.xpath.XPathExpressionException;
import java.io.PrintStream;
import java.util.*;

/**
 * Order the input using some operators
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 21/2/13
 */
public class OrderBy extends UnaryOperator {
    /** The order over streams (might be shared by different order-by before a join */
    Order<Operator> order;

    /** The subset of order operators that we use to sort here. Null if using them all. */
    Set<Operator> operators;

    /** The order for the context, computed when initializing this operator */
    int contextOrder[];

    /**
     * New order by operator
     * @param order The order shared by different order by operators
     * @param operators A subset of operators from order or <tt>null</tt> for all
     */
    public OrderBy(Order<Operator> order, Set<Operator> operators) {
        this.order = order;
        this.operators = operators == null ? null: new HashSet<>(operators);
    }

    public OrderBy() {
    }

    @Override
    protected Operator doCopy(boolean deep, Map<Object, Object> map) {
        OrderBy copy = new OrderBy();

        copy.operators = operators != null ? Sets.newHashSet(Operator.copy(operators, deep, map)) : null;
        copy.order = new Order<>();
        for (Set<Operator> set : order.list) {
            copy.order.list.add(Sets.newHashSet(Operator.copy(set, deep, map)));
        }

        return super.copy(deep, map, copy);
    }

    public int size() {
        return operators == null ? Iterables.size(order.items()) : operators.size();
    }

    @Override
    protected Iterator<ReturnValue> _iterator(final PlanContext planContext) {
        return new AbstractIterator<ReturnValue>() {
            public Iterator<Value> iterator;

            @Override
            protected ReturnValue computeNext() {
                if (iterator == null) {
                    ObjectArrayList<Value> list = new ObjectArrayList<>(new Value[0]);
                    {
                        Iterator<Value> iterator = input.iterator(planContext);
                        while (iterator.hasNext()) {
                            list.add(iterator.next());
                        }
                    }

                    Value values[] = list.toArray(new Value[list.size()]);
                    Arrays.sort(values, (o1, o2) -> {
                        for (int index : contextOrder) {
                            int z = Long.compare(o1.context[index], o2.context[index]);
                            if (z != 0)
                                return z;
                        }
                        return 0;
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
    public boolean printDOT(PrintStream out, HashSet<Operator> planNodes, Map<Operator, MutableInt> counts) {
        if (super.printDOT(out, planNodes, counts)) {
            for (Operator operator: order.items())
                out.format("p%s -> p%s [style=\"dashed\", color=\"#ddddff\"];%n", System.identityHashCode(operator), System.identityHashCode(this));
        }
        return false;
    }

    @Override
    protected String getName() {
        if (contextOrder != null) {
            return String.format("OrderBy (%s)", Output.toString(", ", ArrayUtils.toObject(contextOrder)));
        }
        return String.format("OrderBy (%d contexts)", order.size());
    }

    @Override
    protected void doPostInit(List<Map<Operator, Integer>> parentStreams) {
        outputSize = getParent(0).outputSize();

        IntArrayList contextOrder = new IntArrayList();
        Map<Operator, Integer> inputContext = parentStreams.get(0);

        order.flatten();

        // Loop over the operators we have to sort on, and get their index
        Predicate<Operator> predicate = input -> operators == null || operators.contains(input);
        for (Operator operator : Iterables.filter(order.items(), predicate)) {
            Integer contextIndex = inputContext.get(operator);
            if (contextIndex == null) {
                throw new AssertionError("The context index is null");
            }
            contextOrder.add(contextIndex);
        }
        this.contextOrder = contextOrder.toIntArray();
    }

    @Override
    protected void ensureConnections(Map<Operator, Operator> map) {
        for(Set<Operator> set: order.list) {
            Operator.ensureConnections(map, set);
        }
        if (operators != null)
            Operator.ensureConnections(map, operators);
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
