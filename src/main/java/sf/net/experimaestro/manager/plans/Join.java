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
import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import sf.net.experimaestro.utils.CartesianProduct;
import sf.net.experimaestro.utils.log.Logger;

import javax.xml.xpath.XPathExpressionException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.lang.StrictMath.max;

/**
 * Join
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 3/3/13
 */
public class Join extends Product {
    final static private Logger LOGGER = Logger.getLogger();

    ArrayList<JoinReference> joins = new ArrayList<>();

    @Override
    protected Iterator<ReturnValue> _iterator() {
        return new JoinIterator();
    }

    @Override
    protected void ensureConnections(HashMap<Operator, Operator> simplified) {
        for (JoinReference reference : joins)
            reference.operator = Operator.getSimplified(simplified, reference.operator);
    }

    @Override
    protected void addNeededStreams(Collection<Operator> streams) {
        for (JoinReference reference : joins)
            streams.add(reference.operator);
    }

    @Override
    protected String getName() {
        return "join";
    }

    @Override
    public boolean printDOT(PrintStream out, HashSet<Operator> planNodes) {
        if (super.printDOT(out, planNodes)) {
            for (JoinReference join : joins)
                out.format("p%s -> p%s [style=\"dotted\"];%n", System.identityHashCode(join.operator), System.identityHashCode(this));

        }
        return false;
    }

    @Override
    protected void doPostInit(List<Map<Operator, Integer>> parentStreams) throws XPathExpressionException {
        super.doPostInit(parentStreams);

        // Order the joins in function of the orders of streams
        // We can pick any parent since they share the same order
        Order<Operator> order = ((OrderBy) parents.get(0)).order;
        order.flatten();
        int rank = 0;
        final Object2IntOpenHashMap<Operator> rankMap = new Object2IntOpenHashMap<>();
        for (Operator operator : order.items()) {
            rankMap.put(operator, rank++);
        }

        Collections.sort(joins, new Comparator<JoinReference>() {
            @Override
            public int compare(JoinReference o1, JoinReference o2) {
                return Integer.compare(rankMap.get(o1), rankMap.get(o2));
            }
        });


        // Get the context index for each join & stream
        int joinRank = 0;
        for (JoinReference reference : joins) {
            reference.rank = joinRank++;
            reference.contextIndices = new int[parents.size()];
            for (int i = 0; i < parentStreams.size(); i++) {
                Map<Operator, Integer> map = parentStreams.get(i);
                reference.contextIndices[i] = map.get(reference.operator).intValue();
            }
        }


    }

    static public class JoinReference {
        int rank;
        Operator operator;
        int[] contextIndices;

        public JoinReference(Operator operator) {
            this.operator = operator;
        }
    }

    /**
     * Add a new operator to joins
     *
     * @param operator
     */
    public void addJoin(Operator operator) {
        joins.add(new JoinReference(operator));
    }

    private class JoinIterator extends Product.AbstractProductIterator {
        Iterator<Value[]> productIterator = ImmutableList.<Value[]>of().iterator();
        long positions[];
        boolean last = false;
        Iterator<Value> cacheIterator[] = new Iterator[parents.size()];

        /**
         * Used to store values when there is some context == -1
         */
        TreeMap<long[], Value> stored[] = new TreeMap[parents.size()];

        {
            for (int i = 0; i < stored.length; i++) {
                stored[i] = new TreeMap<>(new ContextComparator(i));
            }
        }

        private JoinIterator() {
        }


        @Override
        boolean next(int i) {
            // Clean up the store
            stored[i].headMap(positions, false).clear();

            // If the context has changed, try to use the cache
            if (cacheIterator[i] == null) {
                cacheIterator[i] = filter(i);
            }

            if (cacheIterator[i].hasNext()) {
                current[i] = cacheIterator[i].next();
                return true;
            }

            // Grab the next one
            if (!inputs[i].hasNext())
                return false;

            // Add the value to the set
            final Value value = inputs[i].next();
            for (long c : value.context) {
                if (c == -1) {
                    stored[i].put(value.context, value);
                    break;
                }
            }


            assert value.nodes.length == 1;
            current[i] = value;
            return true;
        }

        /**
         * Filter values in cache given the current position
         *
         * @param stream The index of the stream cache to filter
         * @return An iterator
         */
        private Iterator<Value> filter(final int stream) {
            final Iterator<Value> iterator = stored[stream].tailMap(positions, true).values().iterator();
            return new AbstractIterator<Value>() {
                long context[];

                @Override
                protected Value computeNext() {
                    while (iterator.hasNext()) {
                        Value value = iterator.next();

                        boolean ok = true;
                        for (JoinReference reference : joins) {
                            int i = reference.contextIndices[stream];
                            if (value.context[i] != -1 && value.context[i] != positions[reference.rank]) {
                                ok = false;
                                break;
                            }
                        }

                        if (ok) {
                            // Copy the context and customize it
                            if (context == null || context.length != value.context.length)
                                context = new long[value.context.length];
                            for (int i = context.length; --i >= 0; )
                                context[i] = value.context[i];
                            for (JoinReference reference : joins) {
                                context[reference.contextIndices[stream]] = positions[reference.rank];
                            }

                            Value v = new Value(value.nodes);
                            v.context = context;
                            return v;
                        }
                    }
                    return endOfData();
                }
            };
        }


        @Override
        protected ReturnValue computeNext() {
            // First loop

            if (first) {
                if (!computeFirst()) return endOfData();
                positions = new long[joins.size()];
                for (int i = positions.length; --i >= 0; ) {
                    positions[i] = -1;
                    for (int j = parents.size(); --j >= 0; )
                        positions[i] = max(positions[i], current[0].context[joins.get(i).contextIndices[0]]);
                }
            }

            // Loop until we have a not empty cartesian product with joined values
            while (true) {
                if (productIterator.hasNext()) {
                    return getReturnValue(productIterator.next());
                }

                // If it was the last product iterator, stop now
                if (last)
                    return endOfData();

                // Loop until joins are satisfied
                resetFlags();

                joinLoop:
                for (int joinIndex = 0; joinIndex < joins.size(); ) {
                    JoinReference join = joins.get(joinIndex);

                    for (int streamIndex = 0; streamIndex < parents.size(); streamIndex++) {
                        int contextIndex = join.contextIndices[streamIndex];
                        LOGGER.info("Context %d of stream %d is %d (position = %d)", contextIndex, streamIndex, current[streamIndex].context[contextIndex], positions[joinIndex]);
                        while (current[streamIndex].context[contextIndex] < positions[joinIndex]) {
                            if (!next(streamIndex))
                                return endOfData();

                            int minRank = checkChanges(streamIndex, positions, joinIndex + 1);

                            // A join current index changed: go back to the main loop on joins
                            if (minRank != -1) {
                                joinIndex = minRank;
                                resetFlags();
                                continue joinLoop;
                            }
                        }

                        assert current[streamIndex].context[contextIndex] == positions[joinIndex];
                    }

                    // A join is complete, now we can process next joinIndex
                    joinIndex++;
                }

                // Fill the cartesian product
                List<Value> lists[] = new List[parents.size()];
                long newPositions[] = new long[joins.size()];

                last = true;
                for (int streamIndex = 0; streamIndex < parents.size(); streamIndex++) {
                    lists[streamIndex] = new ArrayList<>();
                    lists[streamIndex].add(current[streamIndex]);
                    while (true) {
                        if (!next(streamIndex)) {
                            break;
                        }
                        if (checkChanges(streamIndex, newPositions, joins.size()) >= 0) {
                            last = false;
                            break;
                        }
                        lists[streamIndex].add(current[streamIndex]);
                    }
                }

                // Set the current position
                positions = newPositions;
                productIterator = CartesianProduct.of(Value.class, lists).iterator();

            }

        }

        private void resetFlags() {
            for (int i = parents.size(); --i >= 0; )
                cacheIterator[i] = null;
        }

        /**
         * Check the changes in joins
         *
         * @param streamIndex
         * @param newPositions
         * @param maxRank      Check until this rank
         * @return
         */
        private int checkChanges(int streamIndex, long[] newPositions, int maxRank) {
            int minRank = -1;
            for (int i = maxRank; --i >= 0; ) {
                JoinReference join = joins.get(i);
                long resultId = current[streamIndex].context[join.contextIndices[streamIndex]];
                if (this.positions[i] < resultId) {
                    newPositions[i] = resultId;
                    minRank = i;
                } else {
                    // Fails if the input stream is not properly ordered
                    // TODO: check is not correct (fails when properly ordered)
//                    assert this.positions[i] == resultId;
                }
            }
            return minRank;
        }


        private class ContextComparator implements Comparator<long[]> {
            private final int stream;

            public ContextComparator(int stream) {
                this.stream = stream;
            }

            @Override
            public int compare(long[] o1, long[] o2) {
                for (JoinReference reference : joins) {
                    final int ix = reference.contextIndices[stream];
                    int z = compare(o1[ix], o2[ix]);
                    if (z != 0)
                        return z;
                }
                return 0;
            }

            /**
             * If the context is -1, it is greater than anything else
             */
            private int compare(long a, long b) {
                if (a == -1)
                    return b == -1 ? 0 : 1;
                if (b == -1)
                    return -1;
                return Long.compare(a, b);
            }
        }
    }

}
