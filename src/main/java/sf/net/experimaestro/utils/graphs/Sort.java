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

package sf.net.experimaestro.utils.graphs;

import org.apache.commons.lang.mutable.MutableInt;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.utils.log.Logger;

import java.util.*;

/**
 * Graph sorting functions
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 8/2/13
 */
public class Sort {
    static final private Logger LOGGER = Logger.getLogger();


    /**
     * Topological sort of a set of nodes
     *
     * @param nodes The nodes to be sorted
     * @return An ordered list of nodes
     */
    static public <T extends Node> ArrayList<T> topologicalSort(List<T> nodes) {
        ArrayList<T> ordered = new ArrayList<>();
        HashSet<NodeRef<T>> dandling = new HashSet<>();

        Map<NodeRef<T>, MutableInt> remaining = new HashMap<>();
        for (Node node : nodes) {
            final List<? extends Node> children = node.getChildren();
            if (children.isEmpty())
                dandling.add(new NodeRef(node));
            else
                remaining.put(new NodeRef(node), new MutableInt(children.size()));
        }

        while (!remaining.isEmpty() || !dandling.isEmpty()) {
            if (dandling.isEmpty())
                throw new ExperimaestroRuntimeException("Graph contains cycle");

            HashSet<NodeRef<T>> newDandling = new HashSet<>();
            for (NodeRef<T> ref : dandling) {
                ordered.add(ref.node);
                remaining.remove(ref);
            }

            for (NodeRef<T> ref : dandling) {
                for (Node parent : ref.node.getParents()) {
                    final MutableInt children = remaining.get(new NodeRef<>(parent));
                    children.decrement();
                    assert children.intValue() >= 0;
                    if (children.intValue() == 0)
                        newDandling.add(new NodeRef(parent));
                }

            }
            dandling = newDandling;
        }

        return ordered;

    }

    /**
     * Useful to use a hash
     */
    static private class NodeRef<T extends Node> {
        T node;

        private NodeRef(T node) {
            this.node = node;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(node);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Node)
                return node == obj;
            return ((NodeRef) obj).node == node;
        }
    }
}
