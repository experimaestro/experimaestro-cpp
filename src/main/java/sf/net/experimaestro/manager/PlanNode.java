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

package sf.net.experimaestro.manager;

import org.w3c.dom.Node;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.exceptions.NoSuchParameter;
import sf.net.experimaestro.utils.DAGCartesianProduct;

import javax.xml.xpath.XPathExpressionException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * A plan as a graph node
 */
public class PlanNode implements sf.net.experimaestro.utils.graphs.Node, DAGCartesianProduct.SimpleIterable {
    ArrayList<PlanNode> children = new ArrayList<>();
    ArrayList<PlanNode> parents = new ArrayList<>();

    /**
     * Our plan
     */
    private Plan plan;

    /**
     * Our iterator over values
     */
    Iterator<? extends Mapping> iterator;

    /**
     * The current value
     */
    public Node value;

    /**
     * Our mappings
     */
    Mappings mappings;

    public PlanNode(Plan plan) {
        this.plan = plan;
    }


    final Plan getPlan() {
        return plan;
    }

    @Override
    public List<PlanNode> getParents() {
        return parents;
    }

    @Override
    public List<PlanNode> getChildren() {
        return children;
    }

    @Override
    public void reset() {
        iterator = mappings.iterator();
    }

    @Override
    public boolean next() {
        if (!iterator.hasNext())
            return false;

        try {
            final Task task = plan.createTask();
            final Mapping mapping = iterator.next();
            mapping.set(task);
            value = task.run();
            return true;
        } catch (NoSuchParameter | XPathExpressionException e) {
            throw new ExperimaestroRuntimeException(e);
        }
    }

    public void fillWithNodes(HashSet<PlanNode> set) {
        set.add(this);
        for(PlanNode parent: parents)
            parent.fillWithNodes(set);
    }

    public void printDOT(PrintStream out, HashSet<PlanNode> planNodes) {
        if (planNodes.contains(this))
            return;
        planNodes.add(this);
        out.format("p%s;%n", System.identityHashCode(this));
        for(PlanNode parent: parents) {
            parent.printDOT(out, planNodes);
            out.format("p%s -> p%s;%n", System.identityHashCode(this), System.identityHashCode(parent));
        }
    }


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PlanNode) {
            PlanNode other = (PlanNode)obj;
            return other == this || getPlan().equals(other.getPlan());
        }

        return super.equals(obj);
    }

    /**
     * Init self and then parents
     */
    public void init() throws XPathExpressionException {
        init(new HashSet<PlanNode>());
    }

    /**
     * Initialize the node and its parents
     * @param nodes
     * @throws XPathExpressionException
     */
    private void init(HashSet<PlanNode> nodes) throws XPathExpressionException {
        if (nodes.contains(this))
            return;

        nodes.add(this);
        mappings = plan.init(this);
        for(PlanNode parent: parents) {
            parent.init(nodes);
        }
    }
}
