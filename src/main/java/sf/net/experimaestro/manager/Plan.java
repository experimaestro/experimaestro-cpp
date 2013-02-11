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

package sf.net.experimaestro.manager;

import com.google.common.collect.AbstractIterator;
import org.w3c.dom.Node;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.utils.DAGCartesianProduct;

import javax.xml.xpath.XPathExpressionException;
import java.util.*;

/**
 * An experimental plan.
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 7/2/13
 */
public class Plan {
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

    /**
     * Add a join over those subplans
     *
     * @param plans
     */
    synchronized public void addJoin(Set<Plan[]> plans) {
        data = data.addJoin(plans);
    }

    @Override
    public boolean equals(Object other) {
        // Two plans are equal if holding the same data
        return data == ((Plan) other).data;
    }

    /**
     * Run this plan
     *
     * @return An iterator over the generated XML nodes
     */
    public Iterator<Node> run() throws XPathExpressionException {
        return data.run(this);
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


    /**
     * The data associated to a plan. It is a distinct object since a plan
     * can be either directly equal to another (same object) or can share
     * the same data.
     */
    static public class Data {
        /**
         * The task factory for this plan
         */
        TaskFactory factory;

        /**
         * Direct mappings
         */
        Mappings mappings;

        /**
         * Joins to perform
         */
        ArrayList<Set<Plan[]>> joins = new ArrayList<Set<Plan[]>>();

        /**
         * Direct sub-plans
         */
        ArrayList<Plan> subplans = new ArrayList<>();

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
        synchronized public Data addJoin(Set<Plan[]> paths) {
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
            // Creates the PlanNode
            final PlanNode planNode = planGraph(plan);

            HashSet<PlanNode> set = new HashSet<>();
            planNode.fillWithNodes(set);
            final PlanNode[] nodes = set.toArray(new PlanNode[set.size()]);
            final DAGCartesianProduct product = new DAGCartesianProduct(nodes);
            return new AbstractIterator<Node>() {
                @Override
                protected Node computeNext() {
                    if (product.next())
                        return planNode.value;
                    return endOfData();
                }
            };
        }


        /**
         * Returns the graph corresponding to this plan
         *
         * @param plan
         * @return The node that is the root (sink) of the DAG
         */
        synchronized private PlanNode planGraph(Plan plan) throws XPathExpressionException {
            // Create our node
            final PlanNode self = new PlanNode(plan);

            // --- Create the parent nodes
            for (Plan subplan : subplans) {
                final PlanNode parent = subplan.data.planGraph(subplan);
                self.parents.add(parent);
                parent.children.add(self);
            }

            // --- Handle joins
            PlanNode target = null;
            for (Set<Plan[]> set : joins) {
                // Find it (will be kth of current)
                int k = -1;
                PlanNode current = null;
                for (Plan[] path : set) {
                    current = current == null ? self : current.parents.get(k);
                    k = -1;
                    for (int i = 0; i < path.length; i++) {
                        for (int j = 0; j < current.parents.size(); j++)
                            if (current.parents.get(j).getPlan() == path[i]) {
                                k = j;
                                break;
                            }
                        if (k == -1)
                            throw new RuntimeException();
                    }
                }

                if (target == null)
                    // If we have no target yet
                    target = current.parents.get(k);
                else {
                    // Ensure equality
                    if (!current.parents.get(k).equals(target))
                        throw new ExperimaestroRuntimeException("Cannot join two distinct plans");
                    // Join
                    current.parents.set(k, target);
                }

            }

            self.init(mappings);
            return self;
        }

    }


}
