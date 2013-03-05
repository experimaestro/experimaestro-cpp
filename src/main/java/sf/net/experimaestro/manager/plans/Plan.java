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

import com.google.common.base.Function;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multimap;
import com.sun.istack.internal.Nullable;
import org.apache.log4j.Level;
import org.w3c.dom.Node;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.manager.DotName;
import sf.net.experimaestro.manager.Task;
import sf.net.experimaestro.manager.TaskFactory;
import sf.net.experimaestro.utils.CartesianProduct;
import sf.net.experimaestro.utils.io.LoggerPrintStream;
import sf.net.experimaestro.utils.log.Logger;

import javax.xml.xpath.XPathExpressionException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;

/**
 * An experimental plan.
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 7/2/13
 */
public class Plan {
    final static private Logger LOGGER = Logger.getLogger();
    /**
     * The data
     */
    private Data data;

    /**
     * Creates a new plan
     *
     * @param factory
     */
    public Plan(TaskFactory factory) {
        this.data = new Data(factory);
    }

    private Plan(Data data) {
        this.data = data;
    }

    /**
     * Add a join over those subplans
     *
     * @param plans
     */
    public void addJoin(List<Plan[]> plans) {
        verifyPaths(plans);

        data = data.addJoin(plans);
    }


    public void groupBy(List<Plan[]> plans) {
        verifyPaths(plans);
        data = data.groupBy(plans);
    }

    private void verifyPaths(List<Plan[]> plans) {
        for (Plan[] path : plans)
            if (sub(path, 0) == null)
                throw new ExperimaestroRuntimeException("Subpath cannot be found");
    }

    private Plan sub(Plan[] path, int index) {
        if (index == path.length) return this;
        for (Plan subplan : data.getSubPlans())
            if (subplan == path[index])
                return subplan.sub(path, index + 1);

        return null;
    }

    @Override
    public boolean equals(Object other) {
        // Two plans are equal if holding the same data
        return this == other || (other instanceof Plan && data == ((Plan) other).data);
    }

    /**
     * Run this plan
     *
     * @return An iterator over the generated XML nodes
     */
    public Iterator<Node> run() throws XPathExpressionException {
        return data.run(this);
    }


    public void printPlan(PrintStream out) throws XPathExpressionException {
        final Operator planNode = data.planGraph(this, new PlanMap(), new OperatorMap());
        planNode.printDOT(out);
    }

    /**
     * Create a task
     *
     * @return
     */
    public Task createTask() {
        return data.factory.create();
    }

    public TaskFactory getFactory() {
        return data.factory;
    }


    public Plan copy() {
        return new Plan(data.copy());
    }

    public void set(DotName id, Operator operator) {
        data.set(id, operator);
    }

    public Operator planGraph(PlanMap map, OperatorMap opMap) throws XPathExpressionException {
        return data.planGraph(this, map, opMap);
    }


    /**
     * The data associated to a plan. It is a distinct object since a plan
     * can be either directly equal to another (same object) or can share
     * the same data, which is used to perform joins
     */
    static private class Data {
        /**
         * The task factory for this plan
         */
        TaskFactory factory;

        /**
         * Mappings to either list of plans or operators
         */
        Multimap<DotName, Operator> inputs = ArrayListMultimap.create();


        /**
         * Joins to perform
         */
        ArrayList<List<Plan[]>> joins = new ArrayList<>();

        /**
         * How to group by
         */
        List<Plan[]> groupBy = null;


        /**
         * Number of distinct plans that share this data
         */
        int count = 1;

        public Data(TaskFactory factory) {
            this.factory = factory;
        }

        /**
         * Add a join between a set of nodes
         *
         * @param paths
         * @return
         */
        synchronized public Data addJoin(List<Plan[]> paths) {
            final Data data = ensureOne();
            if (data != this)
                return data.addJoin(paths);

            joins.add(paths);
            return this;
        }

        /**
         * Ensure we do not share anything with other plans
         *
         * @return
         */
        synchronized private Data ensureOne() {
            if (count > 1) {
                Data data = new Data(factory);
                data.joins.addAll(data.joins);
                data.inputs.putAll(data.inputs);
                data.groupBy.addAll(data.groupBy);
                return data;
            }

            return this;
        }


        /**
         * Run this plan
         *
         * @param plan
         * @return
         */
        Iterator<Node> run(Plan plan) throws XPathExpressionException {
            // Creates the TaskNode
            Operator operator = planGraph(plan, new PlanMap(), new OperatorMap());
            if (LOGGER.isTraceEnabled())
                try (LoggerPrintStream out = new LoggerPrintStream(LOGGER, Level.TRACE)) {
                    out.println("After creation");
                    operator.printDOT(out);
                }


            operator = Operator.simplify(operator);
            if (LOGGER.isTraceEnabled())
                try (LoggerPrintStream out = new LoggerPrintStream(LOGGER, Level.TRACE)) {
                    out.println("After simplification");
                    operator.printDOT(out);
                }

            operator.init();
            if (LOGGER.isTraceEnabled())
                try (LoggerPrintStream out = new LoggerPrintStream(LOGGER, Level.TRACE)) {
                    out.println("After initialisation");
                    operator.printDOT(out);
                }

            // Now run
            final Iterator<Value> iterator = operator.iterator();

            return Iterators.transform(iterator, new Function<Value, Node>() {
                @Override
                public Node apply(@Nullable Value from) {
                    assert from.getNodes().length == 1;
                    return from.getNodes()[0];
                }
            });

        }


        /**
         * Returns the graph corresponding to this plan
         *
         * @param plan
         * @param map  The current plan path (containg joins in input, and operators in output)
         * @return The node that is the root (sink) of the DAG
         */
        synchronized private Operator planGraph(Plan plan, PlanMap map, OperatorMap opMap) throws XPathExpressionException {
            // Check if a plan was not already generated
            Operator old = map.get();
            if (old != null)
                return old;


            // --- Handle joins
            for (List<Plan[]> list : joins) {
                assert list.size() > 1;

                final Plan[] first = list.get(0);
                Plan refPlan = first[first.length - 1];
                PlanMap ref = map.sub(first, true);

                // Find it (will be kth of current)
                for (Plan[] path : list.subList(1, list.size())) {
                    // Verify that the two plans to be joined are compatible
                    if (!refPlan.equals(path[path.length - 1]))
                        throw new ExperimaestroRuntimeException("Cannot join two distinct plans");

                    // Join
                    map.sub(path, true).join(ref);
                }
            }

            // --- Loop over the cartesian product of the inputs
            DotName ids[] = new DotName[inputs.keySet().size()];
            OperatorIterable values[] = new OperatorIterable[inputs.keySet().size()];
            {

                int index = 0;
                for (Map.Entry<DotName, Collection<Operator>> input : inputs.asMap().entrySet()) {
                    ids[index] = input.getKey();
                    values[index] = new OperatorIterable(input.getValue(), map, opMap);
                    index++;
                }
                assert index == ids.length;
            }

            // Create a new operator
            Union union = new Union();
            map.set(union);

            for (Operator[] singleInputs : CartesianProduct.of(Operator.class, values)) {
                // Create our node
                TaskNode self = new TaskNode(plan);

                // Find LCAs and store them in a map operator ID -> inputs
                BitSet[] joins = new BitSet[singleInputs.length];
                for (int i = 0; i < joins.length; i++) {
                    joins[i] = new BitSet();
                    opMap.add(singleInputs[i]);
                }
                for (int i = 0; i < ids.length - 1; i++) {
                    for (int j = i + 1; j < ids.length; j++) {
                        // TODO: handle without join the case where the same operator corresponds
                        // to two or more inputs (but is not an LCA of higher order)
                        ArrayList<Operator> lca = opMap.findLCAs(singleInputs[i], singleInputs[j]);
                        for (Operator operator : lca) {
                            int key = opMap.get(operator);
                            joins[i].set(key);
                            joins[j].set(key);
                        }
                    }
                }

                // Build the trie strucutre for product/joins
                TrieNode trie = new TrieNode();
                for (int i = 0; i < joins.length; i++) {
                    trie.add(joins[i], singleInputs[i]);
                }

                TrieNode.MergeResult merge = trie.merge(opMap);
                self.addParent(merge.operator);

                // Associate streams with names
                Map<DotName,Integer> mappings = new TreeMap<>();
                for (int i = 0; i < ids.length; i++) {
                    mappings.put(ids[i], merge.map.get(singleInputs[i]));
                }
                self.setMappings(mappings);


                // --- Handle group by

                if (groupBy == null) {
                    union.addParent(self);
                } else {
                    GroupBy groupBy = new GroupBy();


                    // Get all the ancestor to group by
                    for (Plan[] path : this.groupBy) {
                        final PlanMap submap = map.sub(path, false);
                        final Operator taskNode = submap.get();

                        // Should not happen since they are defined paths
                        assert taskNode != null : "Cannot group by: no associated operator";

                        // Add to the operator

                        // Problem: this can be expanded if it was an union!
                        groupBy.add(taskNode);
                    }

                    // Order using the operators we should group by
                    Order<Operator> order = new Order();
                    for (Operator op : groupBy.operators)
                        order.add(op, false);
                    OrderBy orderBy = new OrderBy(order);
                    orderBy.addParent(self);

                    groupBy.addParent(orderBy);
                    union.addParent(groupBy);
                }
            }

            return union;
        }


        synchronized protected Data copy() {
            count++;
            return this;
        }


        /**
         * Add groups by
         *
         * @param plans
         */
        synchronized public Data groupBy(List<Plan[]> plans) {
            final Data data = ensureOne();
            if (data != this)
                return data.groupBy(plans);

            groupBy = plans;
            return this;
        }

        public void set(DotName id, Operator object) {
            final Data data = ensureOne();
            if (data != this)
                data.set(id, object);
            else {
                inputs.put(id, object);
            }
        }

        public Iterable<? extends Plan> getSubPlans() {
            Set<Plan> set = new HashSet<>();
            for (Operator operator : inputs.values()) {
                operator.addSubPlans(set);
            }
            return set;
        }

    }


    /**
     * Iterates over the diffent inputs
     */
    static private class OperatorIterable implements Iterable<Operator> {
        Collection<Operator> collection;
        PlanMap map;
        OperatorMap opMap;

        public OperatorIterable(Collection<Operator> collection, PlanMap map, OperatorMap opMap) {
            this.collection = collection;
            this.map = map;
            this.opMap = opMap;
        }

        @Override
        public Iterator<Operator> iterator() {
            // We unroll everything below
            final Stack<Iterator<? extends Operator>> iterators = new Stack<>();


            iterators.add(collection.iterator());

            return new AbstractIterator<Operator>() {

                // We put all "simple" (constants) values in a big constant to simplify the plan
                Constant constant = null;

                @Override
                protected Operator computeNext() {
                    while (true) {
                        // Search for a valid iterator in the stack
                        while (true) {
                            if (iterators.peek().hasNext()) break;
                            iterators.pop();
                            if (iterators.empty())
                                if (constant == null)
                                    return endOfData();
                                else {
                                    Constant r = constant;
                                    constant = null;
                                    iterators.push(Iterators.<Operator>emptyIterator());
                                    return r;
                                }
                        }

                        // Get the next item and process
                        Operator source = iterators.peek().next();

                        // Transform the operator (in case it is a plan reference)
                        source = source.init(map, opMap);

//                        if (source instanceof Union) {
//                            // We unroll an union
//                            iterators.add(source.getParents().iterator());
//                            continue;
//                        }

                        if (source instanceof Constant) {
                            if (constant == null)
                                constant = new Constant();
                            constant.add((Constant) source);
                            continue;
                        }

                        return source;
                    }
                }
            };
        }


    }

}
