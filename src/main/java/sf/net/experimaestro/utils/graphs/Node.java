package sf.net.experimaestro.utils.graphs;

import java.util.List;

/**
 * A node in a graph
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 8/2/13
 */
public interface Node {
    List<? extends Node> getParents();
    List<? extends Node> getChildren();
}
