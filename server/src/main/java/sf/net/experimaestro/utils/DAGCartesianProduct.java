/*
 * This file is part of experimaestro.
 * Copyright (c) 2012 B. Piwowarski <benjamin@bpiwowar.net>
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

/**
 *
 */
package sf.net.experimaestro.utils;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import sf.net.experimaestro.exceptions.ValueMismatchException;
import sf.net.experimaestro.utils.graphs.Node;
import sf.net.experimaestro.utils.graphs.Sort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

/**
 * This produces a cartesian product over all the possible combinations, taking care of dependencies
 * induced by a TaskOperator (i.e. an iterator depends on its parents).
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class DAGCartesianProduct {

    private final int[] marginalized, standard;
    private final ProductNode[] nodes;
    boolean first = true;


    /**
     * Construct
     *
     * @param nodes The nodes of the plan
     * @throw If the graph is not a DAG
     */
    public DAGCartesianProduct(SimpleIterable... nodes) {
        final ArrayList<SimpleIterable> list = Sort.topologicalSort(Arrays.asList(nodes));
        this.nodes = new ProductNode[list.size()];

        // Build the maps
        Object2IntOpenHashMap<SimpleIterable> map = new Object2IntOpenHashMap<>();
        Multimap<SimpleIterable, SimpleIterable> marginalizedFrom = HashMultimap.create();
        for (int i = 0; i < list.size(); i++) {
            map.put(list.get(i), i);
            for (SimpleIterable node : list.get(i).marginalizedParents())
                marginalizedFrom.put(node, list.get(i));
        }

        // Fill the list

        IntSet aggregateUpTo = new IntOpenHashSet();

        IntArrayList
                marginalized = new IntArrayList(),
                standard = new IntArrayList();

        for (int i = 0; i < list.size(); i++) {
            int[] marginalizeTo = null;
            final SimpleIterable node = list.get(i);
            final Collection<SimpleIterable> destinations = marginalizedFrom.get(node);
            if (!destinations.isEmpty()) {
                marginalizeTo = new int[destinations.size()];
                int k = 0;
                for (SimpleIterable destination : destinations)
                    marginalizeTo[k++] = map.get(destination);
            }

            int[] marginalizedParents = null;
            if (!node.marginalizedParents().isEmpty()) {
                marginalizedParents = new int[node.marginalizedParents().size()];
                int k = 0;
                for (SimpleIterable m : node.marginalizedParents()) {
                    final Integer id = map.get(m);
                    marginalizedParents[k++] = id;
                }

            }
            final int[] array = aggregateUpTo.toIntArray();
            Arrays.sort(array);
            this.nodes[i] = new ProductNode(node, marginalizeTo, marginalizedParents, array);
            if (this.nodes[i].isMarginalized())
                marginalized.push(i);
            else if (this.nodes[i].isAggregate()) {
                aggregateUpTo.add(i);
            } else standard.push(i);
        }


        this.marginalized = marginalized.toIntArray();
        this.standard = standard.toIntArray();
    }

    /**
     * Resets the iterator
     */
    public void reset() {
        first = true;
    }

    /**
     * Init all the iterators for a given set of indices
     *
     * @param indices The indices to initialize (in reverse order)
     * @return <tt>true</tt> if the initialization was successful, false otherwise
     */
    int init(int indices[]) throws ValueMismatchException {
        for (int i = indices.length; --i >= 0; ) {
            nodes[indices[i]].reset(true);
            if (!nodes[indices[i]].next())
                return -1;
        }
        return indices[indices.length - 1];
    }

    int next(int indices[], int bound) throws ValueMismatchException {
        int last = -1;
        main:
        for (int i = 0; i < indices.length && indices[i] < bound; i++) {
            last = i;
            if (nodes[i].next()) {
                // ok, now we search for the next values of the previous iterators
                for (int j = i; --j >= 0; ) {
                    nodes[j].reset(true); // FIXME: do better than that
                    if (!nodes[j].next()) {
                        // No next, we start back from here
                        i = j;
                        continue main;
                    }
                }
                break;
            }
        }
        return last;
    }

    /**
     * Iterates to the next value
     *
     * @return <tt>false</tt> if there is no other value
     */

    public boolean next() throws ValueMismatchException {
        // Fast exit
        if (nodes.length == 0)
            return false;

        int last;

        // Initialisation if first loop
        if (first) {
            first = false;
            last = init(standard);
        } else {
            last = next(standard, nodes.length);
        }

        if (last < 0)
            return false;

        // Marginalize out everything after
        // FIXME: not clear what to do when having empty sequences
        // for the moment, it just aggregate null values
        boolean _first = true;
        for (int i = 0; i <= last; i++) {

            if (nodes[i].isMarginalized())
                nodes[i].node.initValue();

            while (true) {
                // Find the next configuration for
                if (_first) {
                    if (init(marginalized) < 0)
                        break;
                    init(nodes[last].aggregateUpTo);
                    _first = false;
                } else {
                    int _last = next(marginalized, last);
                    if (_last < 0)
                        break;
                    final int[] aggregateUpTo = nodes[_last].aggregateUpTo;
                    next(aggregateUpTo, nodes.length);
                }
            }

        }

        return true;

    }

    /**
     * A node that can iterate
     */
    public interface SimpleIterable extends Node {
        /**
         * Resets the iterable
         *
         * @param full if <tt>true</tt>, one of the parents of this node has changed
         */
        void reset(boolean full);

        /**
         * Initialise the value (used only when aggregating)
         */
        void initValue();

        /**
         * Process the next value based on the current input(s)
         *
         * @param aggregate If <tt>true</tt>, the current value should be aggregated with previous ones
         * @return <tt>true</tt> if there was a next element
         */
        boolean next(boolean aggregate) throws ValueMismatchException;


        /**
         * Return an iteratable over the nodes over which we group by
         * or <tt>null</tt>
         */
        Set<? extends SimpleIterable> marginalizedParents();

        Iterable<? extends SimpleIterable> getParents();

        Iterable<? extends SimpleIterable> getChildren();

    }

    final public static class ProductNode {
        /**
         * Underlying node
         */
        final SimpleIterable node;

        /**
         * List of node indices that this variable will be marginalized over or <tt>null</tt> if none
         */
        final int marginalizeDestination[];

        /**
         * List of nodes that we need to marginalize to get the value
         */
        final int[] marginalized;

        /**
         * List of all marginalizable up to this index
         */
        private final int[] aggregateUpTo;


        public ProductNode(SimpleIterable node, int[] marginalizeDestination, int[] marginalized, int[] aggregateUpTo) {
            this.node = node;
            this.marginalizeDestination = marginalizeDestination;
            this.marginalized = marginalized;
            this.aggregateUpTo = aggregateUpTo;
        }

        boolean isMarginalized() {
            return marginalizeDestination != null;
        }

        public boolean next() throws ValueMismatchException {
            return node.next(marginalized != null);
        }

        public void reset(boolean full) {
            node.reset(full);
        }

        public boolean isAggregate() {
            return marginalized != null;
        }
    }

}