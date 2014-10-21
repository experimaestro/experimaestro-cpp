package sf.net.experimaestro.utils.graphs;

/**
 * A node in a graph
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 8/2/13
 */
public interface Node {
    Iterable<? extends Node> getParents();

    Iterable<? extends Node> getChildren();
}
