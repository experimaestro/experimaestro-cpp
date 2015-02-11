package sf.net.experimaestro.utils;

import sf.net.experimaestro.utils.log.Logger;

import java.util.*;

/**
 * Created by bpiwowar on 26/9/14.
 */
public class Graph {
    final static public Logger LOGGER = Logger.getLogger();

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
