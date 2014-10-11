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

import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import static java.lang.Math.min;

/**
 * Keeps track of who is a descendent of who
 *
 * <ul>
 * <li>Each operator is associated to one ID (contiguous)</li>
 * <li>If x is an ancestor of y, id(x) < id(y)</li>
 * <li>The ancestor-descendant relationship is kept in a bitset: it exists
 * if </li>
 * </ul>
 */
public class OperatorMap {
    /** The node to ID */
    Object2IntOpenHashMap<Operator> operators = new Object2IntOpenHashMap<>();

    {
        operators.defaultReturnValue(-1);
    }

    ArrayList<Operator> list = new ArrayList<>();
    BitSet ancestors = new BitSet();

    /**
     * Add a new operator (and its ancestors)
     * <b>Warning: operators can only be inserted in topological order (ancestors first)</b>
     *
     * @param operator
     * @return The index of the node
     */
    public int add(Operator operator) {
        // Check if not registered
        int id = operators.getInt(operator);
        if (id >= 0)
            return id;

        // Add parents
        List<Operator> parents = operator.getParents();
        int parentIds[] = new int[parents.size()];
        for (int parentId = 0; parentId < parentIds.length; parentId++) {
            parentIds[parentId] = add(parents.get(parentId));
        }

        // Add the operator
        id = list.size();
        list.add(operator);
        operators.put(operator, id);

        // Set the ancestors
        for (int parentId = 0; parentId < parentIds.length; parentId++)
            markAncestors(parentIds[parentId], id);

        return id;
    }


    /**
     * Returns true if a node is a descendant of another one
     *
     * @param node The node to test
     * @param ancestor The ancestor to test
     * @return True if {@code ancestor} is an ancestor of {@code node}
     */
    boolean isDescendant(int node, int ancestor) {
        if (node <= ancestor)
            return false;
        return ancestors.get(getIndex(ancestor, node));
    }

    /**
     * Get the index of the bit for an ancestor-descendant relationship
     * @param ancestor The ancestor
     * @param descendant The descendant
     * @return The index of the bit
     */
    private int getIndex(int ancestor, int descendant) {
        assert ancestor <= descendant;
        return (descendant * (descendant - 1)) / 2 + ancestor;
    }

    /**
     * Set the bits to mark all the ancestors of a child.
     *
     * We suppose that the ancestors of the parent have already been marked,
     * which should be the case since we add nodes in topological order.
     *
     * @param parentIndex The index of the parent
     * @param childIndex The index of the child
     */
    void markAncestors(int parentIndex, int childIndex) {
        assert childIndex > parentIndex;
        ancestors.set(getIndex(parentIndex, childIndex));

        // Copy ancestors for this child
        int parentFrom = getIndex(0, parentIndex);
        int childFrom = getIndex(0, childIndex);
        for (int i = 0; i < parentIndex; i++)
            if (ancestors.get(parentFrom + i))
                ancestors.set(childFrom + i);
    }

    /**
     * Get the bitset corresponding to a given operator
     *
     * @param id The operator id
     */
    BitSet getAncestors(int id) {
        if (id == 0) return new BitSet();
        int index = getIndex(0, id);
        return ancestors.get(index, index + id);
    }

    /**
     * Get ancestors of a given operator
     *
     * @param id The ID of the operator
     * @param maxLength The maximum ID to consider (exclusive)
     * @return A bitset reprenting the ancestors
     */
    BitSet getAncestors(int id, int maxLength) {
        int index = getIndex(0, id);
        return ancestors.get(index, index + min(maxLength, id));
    }


    /**
     * Find the least common ancestors of a pair of operators
     * @param op1 The first operator
     * @param op2 The second operator
     * @return
     */
    ArrayList<Operator> findLCAs(Operator op1, Operator op2) {
        // Get the indices of the two operators, and ensures id1 < id2
        // id1 > id2 => id1 ancestors (and self) are the only candidates
        int id1 = operators.getInt(op1);
        int id2 = operators.getInt(op2);
        if (id2 < id1) {
            int from = id1;
            id1 = id2;
            id2 = from;
        }

        assert id1 >= 0;
        assert id2 >= 0;

        // --- Generate the list of common ancestors

        // Get (1) ancestors
        BitSet candidates = getAncestors(id1);

        // Add the id1 node
        candidates.set(id1);

        // Intersects with ancestors of id2 (restrict to those that can be ancestors or self of id1)
        candidates.and(getAncestors(id2, id1+1));


        // --- Remove the ancestors which are not the least ancestors

        // For each ancestor X, starting with the last, remove
        // all the ancestors of X
        ArrayList<Operator> selected = new ArrayList<>();
        for (int id = candidates.length(); (id = candidates.previousSetBit(id - 1)) >= 0; ) {
            BitSet ancestors = getAncestors(id);
            candidates.andNot(ancestors);
            selected.add(this.list.get(id));
        }

        return selected;
    }

    public Operator get(int opIndex) {
        return list.get(opIndex);
    }

    /**
     *
     * @param operator
     * @return The ID of the operator or <tt>-1</tt> if no such operator was found
     */
    public int get(Operator operator) {
        return operators.get(operator);
    }

    /**
     * Computes an LCA sub-graph of operators, i.e. a graph where
     * only the pairwise LCA between any two operator are preserved,
     * and where the LCA sub graph preserve the topological order
     *
     * @param operators The list of operators
     */
    public void computeLCAs(List<Operator> operators) {
        class OpId {
            Operator op;
            int id;
            IntSet lcas;

            OpId(Operator op, int id) {
                this.op = op;
                this.id = id;
            }
        }
        final OpId[] opIds = operators.stream()
                .map(op -> new OpId(op, get(op)))
                .sorted((a, b) -> Integer.compare(a.id, b.id))
                .toArray(n -> new OpId[n]);
    }

    /**
     * Returns a bitset corresponding to a set of operators
     * @param operators The operators
     * @return A bitset
     */
    public BitSet setOf(Operator... operators) {
        BitSet set = new BitSet();
        for (Operator operator : operators) {
            set.set(get(operator));
        }
        return set;
    }
}
