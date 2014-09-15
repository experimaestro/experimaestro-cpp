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

import bpiwowar.argparser.utils.Formatter;
import bpiwowar.argparser.utils.Output;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.io.PrintStream;
import java.util.*;

/**
 * A trie node used when joining maning streams into a graph
 * of joins and products
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 2/3/13
 */
public class TrieNode {
    /**
     * The associated operators
     */
    HashSet<Operator> operators = new HashSet<>();

    public TrieNode() {
    }

    public TrieNode(Operator... operators) {
        this.operators = new HashSet<>(Arrays.asList(operators));
        this.weight = this.operators.size();
    }


    static public class Parent {
        IntSet set;
        TrieNode node;

        public Parent(IntSet set, TrieNode node) {
            this.set = set;
            this.node = node;
        }
    }

    /**
     * Pointer to the parents
     * Invariant: parent sets have pairwise null intersections
     */
    LinkedList<Parent> parents = new LinkedList<>();

    /**
     * Weight of this node (number of operators stored in this node or above)
     */
    int weight = 0;


    /**
     * Add to the trie
     *
     * @param set      Contains the remaining subset, i.e. the original inserted subset less the set
     *                 represented by this trie node
     * @param operator The operator to add
     */
    protected boolean add(BitSet set, Operator operator) {
        // Check if we are not the node corresponding to the bitset
        int size = set.cardinality();
        if (size == 0) {
            if (operators.add(operator)) {
                weight++;
                return true;
            }
            return false;
        }


        // try to find a parent with some intersection
        // Below, N is the current set and P is the parent
        for (int p = 0; p < parents.size(); p++) {
            Parent parent = parents.get(p);
            IntSet intersection = new IntArraySet(size);
            for (int i = set.nextSetBit(0); i >= 0; i = set.nextSetBit(i + 1)) {
                if (parent.set.contains(i))
                    intersection.add(i);
            }

            // If the parent intersection is not empty
            if (!intersection.isEmpty()) {
                // N is contained by P
                if (intersection.size() == size) {
                    // N == P [equality]
                    if (size == parent.set.size()) {
                        if (parent.node.operators.add(operator)) {
                            parent.node.weight++;
                            weight++;
                            return true;
                        }
                        return false;
                    } else {
                        // N < P [parent strictly contains set]
                        Parent newParent = new Parent(intersection, new TrieNode(operator));
                        newParent.node.parents.add(parent);
                        parent.set.removeAll(intersection);
                        newParent.node.weight += parent.node.weight;
                        parents.set(p, newParent);
                        weight++;
                        return true;
                    }
                } else {
                    // Case where N is not contained in P. Two possibilities:
                    // (1) P is contained by N: add N as a parent of P
                    // (2) P is not contained by N: create a new parent that will contain P and N

                    // If N is different from P, then we create a new parent
                    if (size != parent.set.size()) {
                        Parent newParent = new Parent(intersection, new TrieNode());
                        newParent.node.parents.add(parent);
                        newParent.node.weight += parent.node.weight;
                        parents.set(p, newParent);
                        parent = newParent;
                    }

                    // Add our selves
                    for (int id : parent.set)
                        set.clear(id);
                    boolean added = parent.node.add(set, operator);
                    for (int id : parent.set)
                        set.set(id);
                    if (added)
                        weight++;
                    return added;
                }
            }
        }

        // We did not add it to an existing parent, add a new parent node
        IntArraySet intSet = new IntArraySet();
        for (int i = set.nextSetBit(0); i >= 0; i = set.nextSetBit(i + 1))
            intSet.add(i);
        parents.add(new Parent(intSet, new TrieNode(operator)));
        weight++;
        return true;
    }

    static public class MergeResult {
        /**
         * The operator
         */
        Operator operator;

        /**
         * Position of each initial operator in the product / join
         */
        Object2IntOpenHashMap<Operator> map = new Object2IntOpenHashMap<>();
    }

    /**
     * DOT output (for debug)
     */
    private void outputDOT(PrintStream out, HashSet<TrieNode> visited) {
        if (visited.contains(this))
            return;
        visited.add(this);

        out.format("p%s [label=\"%d [%s]\"];%n", System.identityHashCode(this), this.weight,
                Output.toString(", ", operators, new Formatter<Operator>() {
                    @Override
                    public String format(Operator operator) {
                        return "(" + operator.getName() + "/" + Integer.toString(System.identityHashCode(operator)) + ")";
                    }
                }));

        for (Parent parent : parents) {
            parent.node.outputDOT(out, visited);
            out.format("p%s -> p%s [label=\"%s\"];%n",
                    System.identityHashCode(this), System.identityHashCode(parent.node),
                    parent.set);
        }
    }

    void outputDOT(PrintStream out) {
        out.println("digraph G {");
        outputDOT(out, new HashSet<TrieNode>());
        out.println("}");
    }

    /**
     * Merge all operators in this map
     *
     * @param opMap The operator bit-map
     * @return The result of the merge
     */
    public MergeResult merge(OperatorMap opMap) {
        Map<Operator, List<Operator>> map = new Object2ObjectOpenHashMap<>();


        while (weight > 1) {
            List<Operator> parents = new ArrayList<>();
            Operator operator = merge(new IntOpenHashSet(), opMap, parents);

            // Create the list of original operators associated to this newly
            // created operator
            LinkedList<Operator> list = new LinkedList<>();
            for (int i = 0; i < parents.size(); i++) {
                Operator parent = parents.get(i);
                if (map.containsKey(parent)) {
                    list.addAll(map.get(parent));
                    map.remove(parent);
                } else list.add(parent);
            }
            map.put(operator, list);
        }

        MergeResult result = new MergeResult();
        HashSet<Operator> operators = new HashSet<>();
        getAllOperators(operators);
        assert operators.size() <= 1;

        if (operators.size() == 1) {
            result.operator = operators.iterator().next();

            List<Operator> list = map.get(result.operator);
            if (list != null) {
                for (int i = 0; i < list.size(); i++)
                    result.map.put(list.get(i), i);
            } else {
                result.map.put(result.operator, 0);
            }
        }

        return result;
    }


    /**
     * Select the node in the trie with the heighest weight, and without any parent
     * with a weight > 1
     *
     * @param nodeIndices The indices of the merged operators up this trie node
     */
    private NAryOperator merge(IntSet nodeIndices, OperatorMap opMap, List<Operator> list) {
        // Nothing to merge
        if (weight == 0)
            return null;

        // Search for a better node above us
        Parent argmax = null;
        int maxWeight = 1;
        for (Parent entry : parents) {
            int parentWeight = entry.node.weight;
            if (parentWeight > maxWeight) {
                maxWeight = parentWeight;
                argmax = entry;
            }
        }

        // Case where we found one
        if (argmax != null) {
            nodeIndices.addAll(argmax.set);
            NAryOperator merge = argmax.node.merge(nodeIndices, opMap, list);
            weight -= merge.parents.size() - 1;
            return merge;
        }

        // --- OK, this is ours

        // Retrieve opertors below
        Map<Operator, IntSet> mergedOperators = new HashMap<>();
        IntOpenHashSet mergedIndices = new IntOpenHashSet();
        retrieveOperators(mergedOperators, mergedIndices);

        // Did not find any better parent: merge
        NAryOperator operator;
        if (nodeIndices.isEmpty()) {
            Product product = new Product();
            operator = product;
            for (Operator parent : mergedOperators.keySet()) {
                operator.addParent(parent);
                list.add(parent);
            }

        } else {
            // Wee need a join
            Join join = new Join();
            operator = join;
            Order<Operator> order = new Order();

            // Build the join and the order on the common indices
            for (int opIndex : nodeIndices) {
                Operator op = opMap.get(opIndex);
                join.addJoin(op);
                order.add(op, false);
            }


            for (Map.Entry<Operator, IntSet> parent : mergedOperators.entrySet()) {
                // Order the results first
                OrderBy orderBy = new OrderBy(order, null);
                orderBy.addParent(parent.getKey());

                list.add(parent.getKey());
                // Add the sorted stream to the join
                operator.addParent(orderBy);
            }
        }

        // Compute the set corresponding to this new operator
        // (relative to the node)
        BitSet set = new BitSet();
        for (int indice : mergedIndices) {
            set.set(indice);
        }

        add(set, operator);

        return operator;
    }


    private void getAllOperators(HashSet<Operator> allOperators) {
        if (!operators.isEmpty() && allOperators.contains(this.operators.iterator().next()))
            return;

        for (Operator operator : operators)
            allOperators.add(operator);

        for (Parent parent : parents)
            parent.node.getAllOperators(allOperators);
    }

    /**
     * Retrieve all the operators from this node and ancestors, removing
     * all associated operators and pruning empty branches
     *
     * @param allOperators The retrieved operators, each associated to the indices they are associated with
     * @param indices
     */
    private int retrieveOperators(Map<Operator, IntSet> allOperators, IntSet indices) {
        if (!operators.isEmpty() && allOperators.containsKey(operators.iterator().next()))
            return 0;

        for (Operator operator : operators)
            allOperators.put(operator, indices);

        int found = operators.size();
        weight -= found;
        operators.clear();

        Iterator<Parent> iterator = parents.iterator();
        while (iterator.hasNext()) {
            Parent parent = iterator.next();
            IntSet _indices = new IntOpenHashSet();
            int _found = parent.node.retrieveOperators(allOperators, _indices);
            if (_found > 0) {
                indices.addAll(parent.set);
                indices.addAll(_indices);
            }
            weight -= found;
            found += _found;
            if (parent.node.weight == 0) {
                iterator.remove();
            }

        }

        return found;
    }
}
