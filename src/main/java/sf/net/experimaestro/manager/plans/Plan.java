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
import com.google.common.collect.Iterators;
import com.sun.istack.internal.Nullable;
import org.apache.log4j.Level;
import org.w3c.dom.Node;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.manager.Task;
import sf.net.experimaestro.manager.TaskFactory;
import sf.net.experimaestro.utils.io.LoggerPrintStream;
import sf.net.experimaestro.utils.log.Logger;

import javax.xml.xpath.XPathExpressionException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

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
    public Plan(TaskFactory factory, Mappings mappings) {
        this.data = new Data(factory, mappings);
    }

    private Plan(Data data) {
        this.data = data;
    }

    /**
     * Add a join over those subplans
     *
     * @param plans
     */
    synchronized public void addJoin(List<Plan[]> plans) {
        data = data.addJoin(plans);
    }

    public void groupBy(List<Plan[]> plans) {
        data = data.groupBy(plans);
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
        final Operator planNode = data.planGraph(this);
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

    public Mappings init(TaskNode node) throws XPathExpressionException {
        return data.mappings.init(node);
    }

    public Plan copy() {
        return new Plan(data.copy());
    }

    public void add(Mappings mappings) {
        data = data.add(mappings);
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
         * Direct sub-plans
         */
        ArrayList<Plan> subplans = new ArrayList<>();

        /**
         * Direct mappings
         */
        Mappings mappings;

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

        public Data(TaskFactory factory, Mappings mappings) {
            this.factory = factory;
            this.mappings = mappings;
            HashSet<Plan> set = new HashSet<>();
            mappings.addPlans(set);
            this.subplans = new ArrayList<>(set);
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
                Data data = new Data(factory, mappings);
                data.joins.addAll(data.joins);
                data.subplans.addAll(data.subplans);
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
            final Operator mainNode = planGraph(plan).init();

            // Display the plan graph is trace is enabled
            if (LOGGER.isTraceEnabled()) {
                LoggerPrintStream out = new LoggerPrintStream(LOGGER, Level.TRACE);
                mainNode.printDOT(out);
                out.flush();
            }

            // Now run
            final Iterator<Value> iterator = mainNode.iterator();

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
         *
         * @param plan
         * @return The node that is the root (sink) of the DAG
         */
        synchronized private Operator planGraph(Plan plan) throws XPathExpressionException {
            // Create our node
            final TaskNode self = new TaskNode(plan);
            Operator operator = self;

            if (subplans.size() <= 1) {
                if (!subplans.isEmpty()) {
                    final Operator parent = subplans.get(0).data.planGraph(subplans.get(0));
                    self.addParent(parent);
                }
            } else {
                // Use a cartesian product
                Product product = new Product();
                for (Plan subplan : subplans) {
                    final Operator parent = subplan.data.planGraph(subplan);
                    product.addParent(parent);
                }
                self.addParent(product);
            }

            // --- Handle joins
            TaskNode target = null;
            for (List<Plan[]> list : joins) {
                // Find it (will be kth of current)
                for (Plan[] path : list) {
                    int k = -1;
                    TaskNode current = null;
                    List<Operator> parents = null;

                    for (int i = 0; i < path.length; i++) {
                        current = current == null ? self : (TaskNode) getParents(current).get(k);
                        parents = getParents(current);
                        k = -1;
                        for (int j = 0; j < parents.size(); j++)
                            if (((TaskNode) parents.get(j)).getPlan() == path[i]) {
                                k = j;
                                break;
                            }
                        if (k == -1)
                            throw new RuntimeException("Could not find a matching plan");
                    }

                    if (target == null)
                        // If we have no target yet
                        target = (TaskNode) parents.get(k);
                    else {
                        // Ensure equality
                        if (!parents.get(k).equals(target))
                            throw new ExperimaestroRuntimeException("Cannot join two distinct plans");
                        // Join
                        LOGGER.debug("Join: replacing %s by %s", System.identityHashCode(parents.get(k)), System.identityHashCode(target));
                        parents.set(k, target);
                        target.children.add(current);
                    }
                }

            }

            // --- Handle group by: mark parents

            if (groupBy != null) {
                GroupBy groupBy = new GroupBy();

                // Get all the ancestor to group by
                for (Plan[] path : this.groupBy) {
                    TaskNode current = self;
                    List<Operator> parents = getParents(current);
                    for (int i = 0; i < path.length; i++) {
                        int k = -1;
                        for (int j = 0; j < parents.size(); j++)
                            if (((TaskNode)parents.get(j)).getPlan() == path[i]) {
                                k = j;
                                break;
                            }
                        if (k == -1)
                            throw new RuntimeException("Could not find a matching plan for group-by");

                        current = (TaskNode) parents.get(k);
                        parents = getParents(current);
                    }

                    // Remove
                    groupBy.add(current);
                }

                operator = groupBy;
            }

            return operator;
        }

        /**
         * Get the taskoperators parents
         * @param current The current TaskOperator
         * @return
         */
        private List<Operator> getParents(Operator current) {
            if (current instanceof GroupBy)
                current = current.getParents().get(0);


            if (((TaskNode)current).input instanceof Product)
                return ((TaskNode)current).input.getParents();
            return current.getParents();
        }


        synchronized protected Data copy() {
            count++;
            return this;
        }

        /**
         * Add new mappings
         *
         * @param mappings
         * @return
         */
        synchronized public Data add(Mappings mappings) {
            final Data data = ensureOne();
            if (data != this)
                return data.add(mappings);

            if (this.mappings instanceof Mappings.Alternative) {
                ((Mappings.Alternative) this.mappings).add(mappings);
            } else {
                Mappings.Alternative alt = new Mappings.Alternative();
                alt.add(this.mappings);
                alt.add(mappings);
                this.mappings = alt;
            }

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
    }


}
