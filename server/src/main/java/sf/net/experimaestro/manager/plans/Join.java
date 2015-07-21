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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.apache.commons.lang.mutable.MutableInt;
import sf.net.experimaestro.manager.scripting.ScriptContext;
import sf.net.experimaestro.utils.CartesianProduct;
import sf.net.experimaestro.utils.log.Logger;

import java.io.PrintStream;
import java.util.*;

import static java.lang.StrictMath.max;
import static java.lang.System.identityHashCode;

/**
 * Join various inputs together on a common subset of operators
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class Join extends Product {
    final static private Logger LOGGER = Logger.getLogger();

    /**
     * The list of streams on which we join
     */
    ArrayList<JoinReference> joins = new ArrayList<>();

    public Join(ScriptContext sc) {
        super(sc);
    }

    @Override
    protected Iterator<ReturnValue> _iterator() {
        return new JoinIterator(scriptContext);
    }

    @Override
    protected void ensureConnections(Map<Operator, Operator> map) {
        for (JoinReference reference : joins)
            reference.operator = Operator.getSimplified(map, reference.operator);
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
    public boolean printDOT(PrintStream out, HashSet<Operator> planNodes, Map<Operator, MutableInt> counts) {
        if (super.printDOT(out, planNodes, counts)) {
            for (JoinReference join : joins)
                out.format("p%s -> p%s [style=\"dotted\"];%n", identityHashCode(join.operator), identityHashCode(this));

        }
        return false;
    }

    @Override
    protected void doPostInit(List<Map<Operator, Integer>> parentStreams) {
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

        Collections.sort(joins, (o1, o2) -> Integer.compare(rankMap.get(o1.operator), rankMap.get(o2.operator)));


        // Get the context index for each join & stream
        int joinRank = 0;
        for (JoinReference reference : joins) {
            reference.rank = joinRank++;
            reference.contextIndices = new int[parents.size()];
            for (int i = 0; i < parentStreams.size(); i++) {
                Map<Operator, Integer> map = parentStreams.get(i);
                Integer index = map.get(reference.operator);
                reference.contextIndices[i] = index.intValue();
            }
        }


    }

    /**
     * Add a new operator to join on
     *
     * @param operator The operator
     */
    public void addJoin(Operator operator) {
        joins.add(new JoinReference(operator));
    }

    /**
     * The object representing the join
     */
    static public class JoinReference {
        int rank;

        // The operator
        Operator operator;

        /**
         * The indices in the various inputs
         */
        int[] contextIndices;

        public JoinReference(Operator operator) {
            this.operator = operator;
        }
    }

    private class JoinIterator extends Product.AbstractProductIterator {
        // An iterator
        Iterator<Value[]> productIterator = ImmutableList.<Value[]>of().iterator();

        /**
         * Positions for the various joined stream
         * The index corresponds to that of {@link #joins}
         */
        long positions[];

        boolean last = false;

        /**
         * Used to store values when there is some context == -1
         */
        TreeSet<Value> stored[] = new TreeSet[parents.size()];

        {
            for (int i = 0; i < stored.length; i++) {
                stored[i] = new TreeSet<>(new ContextComparator(i));
            }
        }

        private JoinIterator(ScriptContext simulate) {
            super(simulate);
        }


        @Override
        boolean next(int i) {
            if (!super.next(i))
                return false;


            // Add the value to the set
            final Value value = current[i];
            for (long c : value.context) {
                if (c == -1) {
                    stored[i].add(value);
                    break;
                }
            }

            return true;
        }

        private Iterable<Value> jokers(final int streamIndex) {
            TreeSet<Value> set = stored[streamIndex];

            // Clean up unuseful values
            long[] streamPositions = new long[parents.get(streamIndex).contextMappings.size()];
            for (int i = 0; i < joins.size(); i++) {
                JoinReference reference = joins.get(i);
                final int ix = reference.contextIndices[streamIndex];
                streamPositions[ix] = positions[i];
            }

            set.headSet(new Value(streamPositions), true).clear();

            return Iterables.filter(set, input -> {
                for (JoinReference reference : joins) {
                    long pos = input.context[reference.contextIndices[streamIndex]];
                    if (pos != -1 && pos != positions[reference.rank])
                        return false;
                }
                return true;
            });
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
                        positions[i] = max(positions[i], current[j].context[joins.get(i).contextIndices[j]]);
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
                joinLoop:
                for (int joinIndex = 0; joinIndex < joins.size(); ) {
                    JoinReference join = joins.get(joinIndex);

                    for (int streamIndex = 0; streamIndex < parents.size(); streamIndex++) {
                        int contextIndex = join.contextIndices[streamIndex];
                        LOGGER.debug("[%s] Context %d of stream %d is %d (position = %d)",
                                identityHashCode(Join.this),
                                contextIndex, streamIndex, current[streamIndex].context[contextIndex],
                                positions[joinIndex]);

                        if (current[streamIndex].context[contextIndex] > positions[joinIndex]) {
                            int minRank = checkChanges(streamIndex, positions, joinIndex + 1);
                            assert minRank >= 0;
                            joinIndex = minRank;
                            LOGGER.trace("[%s] Restarting the join with context: ",
                                    identityHashCode(Join.this),
                                    Arrays.toString(positions));
                            continue joinLoop;
                        }

                        while (current[streamIndex].context[contextIndex] < positions[joinIndex]) {
                            if (!next(streamIndex))
                                return endOfData();

                            int minRank = checkChanges(streamIndex, positions, joinIndex + 1);

                            // A join current index changed: go back to the main loop on joins
                            if (minRank != -1) {
                                joinIndex = minRank;
                                if (LOGGER.isTraceEnabled())
                                    LOGGER.trace("[%s] Restarting the join with context: ",
                                            identityHashCode(Join.this),
                                            Arrays.toString(positions));
                                continue joinLoop;
                            }

                            LOGGER.debug("[%s] Context[a] %d of stream %d is %d (position = %d)",
                                    identityHashCode(Join.this),
                                    contextIndex, streamIndex, current[streamIndex].context[contextIndex],
                                    positions[joinIndex]);
                        }


                        // Asserts that we arrived at the right position
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

                        // Add all compatible joker
                        for (Value value : jokers(streamIndex)) {
                            lists[streamIndex].add(new Value(positions, value.nodes));
                        }
                    }
                }

                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("[%s] Selected context: %s", identityHashCode(Join.this), Arrays.toString(positions));
                if (LOGGER.isTraceEnabled()) {
                    for (int streamIndex = 0; streamIndex < parents.size(); streamIndex++) {
                        for (Value value : lists[streamIndex]) {
                            LOGGER.trace("[%s] stream %d, %s with value id %d", identityHashCode(Join.this),
                                    streamIndex, Arrays.toString(value.context), value.id);
                        }
                    }
                }

                // Set the current position
                positions = newPositions;
                productIterator = CartesianProduct.of(Value.class, lists).iterator();

            }

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
            final Value value = current[streamIndex];

            for (int i = maxRank; --i >= 0; ) {
                JoinReference join = joins.get(i);
                long resultId = value.context[join.contextIndices[streamIndex]];
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


        private class ContextComparator implements Comparator<Value> {
            private final int stream;

            public ContextComparator(int stream) {
                this.stream = stream;
            }

            @Override
            public int compare(Value o1, Value o2) {
                for (JoinReference reference : joins) {
                    final int ix = reference.contextIndices[stream];
                    int z = compare(o1.context[ix], o2.context[ix]);
                    if (z != 0)
                        return z;
                }
                return Long.compare(o1.id, o2.id);
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
