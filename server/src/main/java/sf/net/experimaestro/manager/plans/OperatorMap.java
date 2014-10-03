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

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import static java.lang.Math.min;

/**
 * Keeps track of who is a descendent of who
 */
public class OperatorMap {
    Object2IntOpenHashMap<Operator> operators = new Object2IntOpenHashMap<>();

    {
        operators.defaultReturnValue(-1);
    }

    ArrayList<Operator> list = new ArrayList<>();
    BitSet ancestors = new BitSet();

    /**
     * Add a new operator (and its ancestors)
     * <b>Waring: operators can only be inserted in topological order (ancestors first)</b>
     *
     * @param operator
     * @return
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
            setChild(parentIds[parentId], id);

        return id;
    }


    boolean isDescendant(int node, int ancestor) {
        if (node <= ancestor)
            return false;
        return ancestors.get(getIndex(ancestor, node));
    }

    private int getIndex(int ancestor, int node) {
        assert ancestor <= node;
        return (node * (node - 1)) / 2 + ancestor;
    }

    void setChild(int parentIndex, int childIndex) {
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

    BitSet getAncestors(int id, int maxLength) {
        int index = getIndex(0, id);
        return ancestors.get(index, index + min(maxLength, id));
    }


    ArrayList<Operator> findLCAs(Operator op1, Operator op2) {
        // Get the indices of the two operators, and ensures from1 < id2
        int id1 = operators.getInt(op1);
        int id2 = operators.getInt(op2);
        if (id2 < id1) {
            int from = id1;
            id1 = id2;
            id2 = from;
        }

        assert id1 >= 0;
        assert id2 >= 0;

        // Get the list of common ancestors
        BitSet candidates = getAncestors(id1);
        candidates.set(id1);
        candidates.and(getAncestors(id2, id1+1));

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
}
