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
import com.google.common.collect.*;
import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Level;
import sf.net.experimaestro.manager.DotName;
import sf.net.experimaestro.manager.Task;
import sf.net.experimaestro.manager.TaskFactory;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.manager.json.JsonNull;
import sf.net.experimaestro.utils.io.LoggerPrintStream;
import sf.net.experimaestro.utils.log.Logger;

import javax.xml.xpath.XPathExpressionException;
import java.util.*;

/**
 * A fake operator corresponding to a task factory. This is replaced by
 * {@linkplain TaskOperator} when constructing the final plan.
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class Plan extends Operator {
    final static private Logger LOGGER = Logger.getLogger();

    /**
     * The task factory associated with this plan
     */
    TaskFactory factory;

    /**
     * Mappings to either list of operators or operators
     */
    List<Multimap<DotName, Operator>> inputsList = new ArrayList<>();

    /**
     * Creates a new plan
     *
     * @param factory
     */
    public Plan(TaskFactory factory) {
        this.factory = factory;
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
        return prepare(simplify, initialize);
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


    @Override
    public Plan doCopy(boolean deep, Map<Object, Object> map) {
        Plan copy = new Plan(factory);
        for (Multimap<DotName, Operator> multimap : inputsList) {
            HashMultimap<DotName, Operator> mapCopy = HashMultimap.create();
            copy.inputsList.add(mapCopy);
            for (Map.Entry<DotName, Collection<Operator>> entry : multimap.asMap().entrySet()) {
                mapCopy.putAll(entry.getKey(), deep ? Operator.copy(entry.getValue(), deep, map) : entry.getValue());
            }
        }

        return copy;
    }


    /**
     * Run this plan
     *
     * @param planContext
     * @return An iterator over the generated XML values
     */
    public Iterator<Json> run(PlanContext planContext) throws XPathExpressionException {
        Operator operator = prepare(true, true);

        // Now run
        final Iterator<Value> iterator = operator.iterator(planContext);

        return Iterators.transform(iterator, new Function<Value, Json>() {
            @Override
            public Json apply(Value from) {
                assert from.getNodes().length == 1;
                return from.getNodes()[0];
            }
        });

    }

    private Operator prepare(boolean simplify, boolean initialize) throws XPathExpressionException {
        // Creates the TaskOperator
        Operator operator = prepare(new HashMap<>(), new OperatorMap());
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
     * @param map The current plan path (containg joins in input, and operators in output)
     * @return The node that is the root (sink) of the DAG
     */
    public synchronized Operator prepare(Map<Operator, Operator> map, OperatorMap opMap) {
        // Check if a plan was not already generated
        Operator old = map.get(this);
        if (old != null)
            return old;


        // Outputs will contain the list of operators that have
        // to be merged (because we have a series of different inputs)
        ArrayList<Operator> outputs = new ArrayList<>();

        for (Multimap<DotName, Operator> inputs : inputsList) {
            TaskOperator self = new TaskOperator(this);
            self.scope = this.scope;

            if (inputs.isEmpty()) {
                self.addParent(new Constant(JsonNull.getSingleton()));
                self.setMappings(ImmutableMap.of());
                outputs.add(self);
            } else {
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
                Operator inputOperators[] = new Operator[inputValues.length];

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

                    opMap.add(inputOperators[i]);

                }


                // Find LCAs and store them in a map operator ID -> inputs
                // joins contains the list of pairwise LCAs in the operator
                // graph above
                BitSet[] joins = new BitSet[inputOperators.length];
                for (int i = 0; i < joins.length; i++) {
                    joins[i] = new BitSet();
                }

                for (int i = 0; i < ids.length - 1; i++) {
                    for (int j = i + 1; j < ids.length; j++) {
                        ArrayList<Operator> lca = opMap.findLCAs(inputOperators[i], inputOperators[j]);
                        for (Operator operator : lca) {
                            int key = opMap.get(operator);
                            joins[i].set(key);
                            joins[j].set(key);
                        }
                    }
                }

                Lattice lattice = new Lattice(opMap);
                for (int i = 0; i < joins.length; i++) {
                    lattice.add(joins[i], inputOperators[i]);
                }
                LatticeNode.MergeResult merge = lattice.merge();

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
        }

        // End of loop over inputs

        Operator planOperator;
        if (outputs.size() == 1) {
            map.put(this, outputs.get(0));
            planOperator = outputs.get(0);
        } else {
            Union union = new Union();
            map.put(this, union);
            for (Operator output : outputs)
                union.addParent(output);
            planOperator = union;
        }

        return planOperator;

    }

    public void add(PlanInputs inputs) {
        inputsList.add(inputs.map);
    }


    @Override
    public void getAncestors(HashSet<Operator> ancestors) {
        if (ancestors.contains(this))
            return;

        ancestors.add(this);
        for (int i = 0; i < inputsList.size(); i++) {
            for (Operator parent : inputsList.get(i).values()) {
                parent.getAncestors(ancestors);
            }

        }
    }

    @Override
    public List<Operator> getParents() {
        // TODO: implement getParents
        throw new NotImplementedException();
    }

    @Override
    protected Iterator<ReturnValue> _iterator(PlanContext planContext) {
        throw new UnsupportedOperationException();
    }


    /**
     * Iterates over the different inputs
     */
    static class OperatorIterable implements Iterable<Operator> {
        Collection<Operator> collection;
        Map<Operator, Operator> map;
        OperatorMap opMap;

        public OperatorIterable(Collection<Operator> collection, Map<Operator, Operator> map, OperatorMap opMap) {
            this.collection = collection;
            this.map = map;
            this.opMap = opMap;
        }

        @Override
        public Iterator<Operator> iterator() {
            final Iterator<Operator> iterator = collection.iterator();

            return new AbstractIterator<Operator>() {
                @Override
                protected Operator computeNext() {
                    if (iterator.hasNext()) {

                        // Get the next item and process
                        Operator source = iterator.next();

                        // Transform the operator (in case it is a plan reference)
                        source = source.prepare(map, opMap);

                        return source;
                    }
                    return endOfData();
                }
            };
        }


    }

}
