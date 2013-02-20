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


import com.google.common.collect.AbstractIterator;

import javax.xml.xpath.XPathExpressionException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * An operator
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 20/2/13
 */
public abstract class Operator {
    /**
     * Children nodes will take our output
     */
    ArrayList<Operator> children = new ArrayList<>();


    /**
     * Returns the parents of this node
     */
    public abstract List<Operator> getParents();

    /**
     * Current iterator
     */
    Iterator<Value> currentIterator;

    public abstract List<Plan> plans();


    static public abstract class OperatorIterator extends AbstractIterator<Value> {
        private Value current;

        @Override
        final protected Value computeNext() {
            if (current != null && current.next != null)
                return current = current.next;

            Value newValue = _computeNext();
            if (current != null && newValue != null) {
                newValue.id = current.id + 1;
                current.next = newValue;
            }
            return current = newValue;
        }

        protected abstract Value _computeNext();
    }

    protected abstract OperatorIterator _iterator();


    public Iterator<Value> iterator() {
        if (currentIterator != null)
            return currentIterator;
        return currentIterator = _iterator();
    }


    /**
     * Initialize the node and its parents
     *
     * @param processed Operators that were already processed
     * @throws javax.xml.xpath.XPathExpressionException
     *
     */
    protected Operator init(HashSet<Operator> processed) throws XPathExpressionException {
        // Already done
        if (processed.contains(this))
            return null;

        processed.add(this);
        for (Operator parent : getParents()) {
            parent.init(processed);
        }

        return this;

    }

    /**
     * Init ourselves andthen parents
     */
    public Operator init() throws XPathExpressionException {
        return init(new HashSet<Operator>());
    }

    /**
     * Print a graph starting from this node
     *
     * @param out The output stream
     */
    public void printDOT(PrintStream out) {
        out.println("digraph G {");
        printDOT(out, new HashSet<Operator>());
        out.println("}");
    }

    /**
     * Print a node
     *
     * @param out       The output stream
     * @param planNodes The nodes already processed (case of shared ancestors)
     */
    public void printDOT(PrintStream out, HashSet<Operator> planNodes) {
        if (planNodes.contains(this))
            return;
        planNodes.add(this);
        out.format("p%s;%n", System.identityHashCode(this));
        for (Operator parent : getParents()) {
            parent.printDOT(out, planNodes);
            out.format("p%s -> p%s;%n", System.identityHashCode(this), System.identityHashCode(parent));
        }
    }

    /**
     * Add all ancestors and self to the provided set
     *
     * @param set The set to be filled
     */
    public void fillWithNodes(HashSet<Operator> set) {
        set.add(this);
        for (Operator parent : getParents())
            parent.fillWithNodes(set);
    }

    abstract public void addParent(Operator parent);
}