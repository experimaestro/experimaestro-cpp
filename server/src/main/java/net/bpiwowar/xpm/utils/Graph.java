package net.bpiwowar.xpm.utils;

/*
 * This file is part of experimaestro.
 * Copyright (c) 2014 B. Piwowarski <benjamin@bpiwowar.net>
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Created by bpiwowar on 26/9/14.
 */
public class Graph {
    final static public Logger LOGGER = LogManager.getFormatterLogger();

    /**
     * Topological sort of a set of nodes
     *
     * @param graph           A list of nodes that will be emptied if the sort is successful. Otherwise, nodes
     * @param forward_edges   The forward edges
     * @param backwards_edges The backward edges
     * @param <Node>          A node
     * @return A sorted list
     */
    static public <Node> ArrayList<Node> topologicalSort(
            Collection<Node> graph,
            Map<Node, ? extends Set<Node>> forward_edges,
            Map<Node, ? extends Set<Node>> backwards_edges) {
        ArrayList<Node> sorted_nodes = new ArrayList<>();
        boolean done = false;
        while (!done) {
            done = true;
            Iterator<Node> iterator = graph.iterator();
            while (iterator.hasNext()) {
                Node n1 = iterator.next();
                final Set<Node> inSet = backwards_edges.get(n1);
                LOGGER.debug("Node %s has %d incoming edges", n1,
                        inSet == null ? 0 : inSet.size());
                if (inSet == null || inSet.isEmpty()) {
                    LOGGER.debug("Removing node %s", n1);
                    done = false;
                    sorted_nodes.add(n1);
                    iterator.remove();

                    // Remove the edges from n1
                    final Set<Node> nodes = forward_edges.get(n1);
                    if (nodes != null)
                        for (Node n2 : nodes) {
                            LOGGER.debug("Removing edge from %s to %s", n1, n2);
                            backwards_edges.get(n2).remove(n1);
                        }
                }
            }
        }
        return sorted_nodes;
    }
}
