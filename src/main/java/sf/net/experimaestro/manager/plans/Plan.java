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
import com.google.common.collect.Iterators;
import com.google.common.collect.Multimap;
import org.apache.log4j.Level;
import org.w3c.dom.Node;
import sf.net.experimaestro.manager.DotName;
import sf.net.experimaestro.manager.Task;
import sf.net.experimaestro.manager.TaskFactory;
import sf.net.experimaestro.utils.io.LoggerPrintStream;
import sf.net.experimaestro.utils.log.Logger;

import javax.xml.xpath.XPathExpressionException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

/**
 * A fake operator corresponding to a task factory. This is replaced by
 * {@linkplain TaskNode} when constructing the final plan.
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 7/2/13
 */
public class Plan extends Operator {
    final static private Logger LOGGER = Logger.getLogger();

    /**
     * The task factory associated with this plan
     */
    TaskFactory factory;

    /**
     * Mappings to either list of plans or operators
     */
    List<Multimap<DotName, Operator>> inputsList = new ArrayList();

    /**
     * Creates a new plan
     *
     * @param factory
     */
    public Plan(TaskFactory factory) {
        this.factory = factory;
    }

    /**
     * Run this plan
     *
     * @param runOptions
     * @return An iterator over the generated XML nodes
     */
    public Iterator<Node> run(RunOptions runOptions) throws XPathExpressionException {
        return run(this, runOptions);
    }

    /**
     * Get the operator corresponding to this plan
     *
     * @param simplify
     * @param initialize
     * @return
     * @throws XPathExpressionException
     */
    public Operator getOperator(boolean simplify, boolean initialize) throws XPathExpressionException {
        return getPlanOperator(this, simplify, initialize);
    }

    /**
     * Create a task
     *
     * @return
     */
    public Task createTask() {
        return factory.create();
    }

    public TaskFactory getFactory() {
        return factory;
    }


    /**
     * Prepare an operator
     *
     * @return
     * @throws XPathExpressionException
     */
    @Override
    public Operator prepare() throws XPathExpressionException {
        return planGraph(this, new HashMap<Plan, Operator>(), new OperatorMap());
    }


    /**
     * Run this plan
     *
     * @param plan
     * @param runOptions
     * @return
     */
    Iterator<Node> run(Plan plan, RunOptions runOptions) throws XPathExpressionException {
        Operator operator = getPlanOperator(plan, true, true);


        // Now run
        final Iterator<Value> iterator = operator.iterator(runOptions);

        return Iterators.transform(iterator, new Function<Value, Node>() {
            @Override
            public Node apply(Value from) {
                assert from.getNodes().length == 1;
                return from.getNodes()[0];
            }
        });

    }

    private Operator getPlanOperator(Plan plan, boolean simplify, boolean initialize) throws XPathExpressionException {
        // Creates the TaskNode
        Operator operator = planGraph(plan, new HashMap<Plan, Operator>(), new OperatorMap());
        if (LOGGER.isTraceEnabled())
            try (LoggerPrintStream out = new LoggerPrintStream(LOGGER, Level.TRACE)) {
                out.println("After creation");
                operator.printDOT(out);
            }


        if (simplify) {
            operator = Operator.simplify(operator);
            if (LOGGER.isTraceEnabled())
                try (LoggerPrintStream out = new LoggerPrintStream(LOGGER, Level.TRACE)) {
                    out.println("After simplification");
                    operator.printDOT(out);
                }
        }

        if (initialize) {
            operator.init();
            if (LOGGER.isTraceEnabled())
                try (LoggerPrintStream out = new LoggerPrintStream(LOGGER, Level.TRACE)) {
                    out.println("After initialisation");
                    operator.printDOT(out);
                }
        }
        return operator;
    }


    /**
     * Returns the graph corresponding to this plan
     *
     * @param plan
     * @param map  The current plan path (containg joins in input, and operators in output)
     * @return The node that is the root (sink) of the DAG
     */
    synchronized private Operator planGraph(Plan plan, Map<Plan, Operator> map, OperatorMap opMap) throws XPathExpressionException {
        // Check if a plan was not already generated
        Operator old = map.get(this);
        if (old != null)
            return old;


        // Outputs will contain the list of operators that have
        // to be merged (because we have a series of different inputs)
        ArrayList<Operator> outputs = new ArrayList<>();

        for (Multimap<DotName, Operator> inputs : inputsList) {


            // --- Loop over the cartesian product of the inputs
            DotName ids[] = new DotName[inputs.keySet().size()];
            OperatorIterable inputValues[] = new OperatorIterable[inputs.keySet().size()];
            {

                int index = 0;
                for (Map.Entry<DotName, Collection<Operator>> input : inputs.asMap().entrySet()) {
                    ids[index] = input.getKey();
                    inputValues[index] = new OperatorIterable(input.getValue(), map, opMap);
                    index++;
                }
                assert index == ids.length;
            }

            // Create a new operator
            TaskNode self = new TaskNode(plan);

            Operator inputOperators[] = new Operator[inputValues.length];
            BitSet[] joins = new BitSet[inputOperators.length];

            for (int i = inputValues.length; --i >= 0; ) {
                OperatorIterable values = inputValues[i];
                Union union = new Union();
                for (Operator operator : values) {
                    union.addParent(operator);
                }

                if (union.getParents().size() == 1)
                    inputOperators[i] = union.getParent(0);
                else
                    inputOperators[i] = union;

                joins[i] = new BitSet();
                opMap.add(inputOperators[i]);

            }

            // Find LCAs and store them in a map operator ID -> inputs
            for (int i = 0; i < ids.length - 1; i++) {
                for (int j = i + 1; j < ids.length; j++) {
                    // TODO: handle without join the case where the same operator corresponds
                    // to two or more inputs (but is not an LCA of higher order)
                    ArrayList<Operator> lca = opMap.findLCAs(inputOperators[i], inputOperators[j]);
                    for (Operator operator : lca) {
                        int key = opMap.get(operator);
                        joins[i].set(key);
                        joins[j].set(key);
                    }
                }
            }


            // Build the trie structure for product/joins
            TrieNode trie = new TrieNode();
            for (int i = 0; i < joins.length; i++) {
                trie.add(joins[i], inputOperators[i]);
            }

            TrieNode.MergeResult merge = trie.merge(opMap);
            self.addParent(merge.operator);

            // Associate streams with names
            Map<DotName, Integer> mappings = new TreeMap<>();
            for (int i = 0; i < ids.length; i++) {
                mappings.put(ids[i], merge.map.get(inputOperators[i]));
            }
            self.setMappings(mappings);


            // --- Handle group by

            outputs.add(self);
        }

        // End of loop over inputs

        if (outputs.size() == 1) {
            map.put(plan, outputs.get(0));
            return outputs.get(0);
        }

        Union union = new Union();
        map.put(plan, union);
        for (Operator output : outputs)
            union.addParent(output);
        return union;

    }

    public void add(PlanInputs inputs) {
        inputsList.add(inputs.map);
    }


    @Override
    public List<Operator> getParents() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Iterator<ReturnValue> _iterator(RunOptions runOptions) {
        throw new UnsupportedOperationException();
    }


    /**
     * Iterates over the diffent inputs
     */
    static private class OperatorIterable implements Iterable<Operator> {
        Collection<Operator> collection;
        Map<Plan, Operator> map;
        OperatorMap opMap;

        public OperatorIterable(Collection<Operator> collection, Map<Plan, Operator> map, OperatorMap opMap) {
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
