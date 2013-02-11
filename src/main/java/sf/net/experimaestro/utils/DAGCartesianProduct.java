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

import sf.net.experimaestro.utils.graphs.Node;
import sf.net.experimaestro.utils.graphs.Sort;

import java.util.Arrays;

/**
 * This produces a cartesian product over all the possible combinations, taking care of dependencies
 * induced by a PlanNode (i.e. an iterator depends on its parents).
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class DAGCartesianProduct {
    private final SimpleIterable[] iterables;


    /**
     * A node that can iterate
     */
    public interface SimpleIterable extends Node {
        void reset();

        boolean next();
    }


    public <T extends SimpleIterable & Node> DAGCartesianProduct(T... iterables) {
        this.iterables = Sort.topologicalSort(Arrays.asList(iterables)).toArray(new SimpleIterable[iterables.length]);
    }


    boolean first = true;

    /**
     * Resets the iterator
     */
    public void reset() {
        first = true;
    }

    /**
     * Iterates to the next value
     *
     * @return <tt>false</tt> if there is no other value
     */
    public boolean next() {
        // Fast exit
        if (iterables.length == 0)
            return false;


        // Initialisation if first loop
        if (first) {
            first = false;
            // We initialise the ancestors first (last elements of the array)
            for (int i = iterables.length; --i >= 0; ) {
                iterables[i].reset();
                if (!iterables[i].next())
                    return false;
            }
            return true;
        }

        // We search for the iterator that has a next value

        for (int i = 0; i < iterables.length; i++) {
            if (iterables[i].next()) {
                // ok, no we search for the next values
                for (int j = i; --i >= 0; ) {
                    if (!iterables[i].next()) {
                        // No next, we start back from here
                        i = j;
                        continue;
                    }
                }
                return true;
            }
        }

        return false;

    }

}