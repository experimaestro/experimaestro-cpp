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
import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

import javax.xml.xpath.XPathExpressionException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    OperatorIterator currentIterator;

    Map<Operator, Integer> streams;

    /**
     * Get the mapping between joined operators and context index
     */
    public Map<Operator, Integer> getStreams() {
        return streams == null ? Collections.<Operator, Integer>emptyMap() : Collections.unmodifiableMap(streams);
    }

    public void addParent(Operator parent) {
        parent.addChild(this);
    }


    /** Recursive initialization of operator */
    public Operator init(PlanMap map) {
        List<Operator> parents = getParents();
        for(int i = 0; i < parents.size(); i++) {
            parents.set(i, parents.get(i).init(map));
        }
        return this;
    }

    final public Operator getParent(int i) {
        return getParents().get(i);
    }

    public void addSubPlans(Set<Plan> set) {
        for(Operator parent: getParents())
            parent.addSubPlans(set);
    }


    static public abstract class OperatorIterator extends AbstractIterator<Value> {
        boolean started = false;
        private Value current = null;

        @Override
        final protected Value computeNext() {
            started = true;
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
        return _iterator();
//        OperatorIterator iterator = _iterator();
//        if (currentIterator != null && !currentIterator.started)
//            iterator.current = currentIterator.current;
//        return currentIterator = iterator;
    }


    /**
     * Initialize the node and its parents
     *
     * @param streams
     * @throws javax.xml.xpath.XPathExpressionException
     *
     */
    protected Operator doInit(Multimap<Operator, Operator> streams) throws XPathExpressionException {
        return this;
    }

    /**
     * Prepare the streams of an operator (last operation before running)
     *
     * @param request (in/out) The request in terms of streams
     * @return
     * @throws XPathExpressionException
     */
    protected Operator doPrepare(StreamRequest request) throws XPathExpressionException {
        return this;
    }


    final private Operator init(HashSet<Operator> processed, Multimap<Operator, Operator> streams) throws XPathExpressionException {
        if (processed.contains(this))
            return null;


        for (Operator parent : getParents()) {
            Multimap<Operator, Operator> parentStreams = HashMultimap.create();
            parent.init(processed, parentStreams);
//            streams.putAll(parentStreams);
        }

        final Operator operator = doInit(streams);

        // Update the streams
        processed.add(operator);

        return operator;
    }

    /**
     * Init ourselves andthen parents
     */
    public Operator init() throws XPathExpressionException {
        return init(new HashSet<Operator>(), null);
    }


    final private Operator prepare(HashSet<Operator> processed) throws XPathExpressionException {
        if (processed.contains(this))
            return null;

        for (Operator parent : getParents()) {
            Multiset<Operator> parentCounts = HashMultiset.create();
        }

        final Operator operator = doPrepare(null);

        return operator;
    }

    public Operator prepare() throws XPathExpressionException {
        return prepare(new HashSet<Operator>());
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
        out.flush();
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
        printDOTNode(out);
        for (Operator parent : getParents()) {
            parent.printDOT(out, planNodes);
            out.format("p%s -> p%s;%n", System.identityHashCode(parent), System.identityHashCode(this));
        }
    }

    protected void printDOTNode(PrintStream out) {
        out.format("p%s [label=\"%s\"];%n", System.identityHashCode(this), this.getClass().getName());
    }


    // --- Simplify ---

    static public Operator simplify(Operator operator) {
        return simplify(operator, new HashMap<Operator, Operator>());
    }

    static public Operator simplify(Operator operator, Map<Operator, Operator> simplified) {
        Operator cache = simplified.get(simplified);
        if (cache != null)
            return cache;

        // --- First, simplify all the parents
        List<Operator> parents = operator.getParents();
        for (int i = 0; i < parents.size(); i++) {
            Operator newParent = simplify(parents.get(i));
            if (newParent != parents.get(i)) {
                parents.set(i, newParent);
                newParent.addChild(operator);
            }
        }

        // --- operator == Union
        if (operator instanceof Union) {
            if (parents.size() == 1) {
                return parents.get(0);
            }
        }

        // --- operator == Product
        if (operator instanceof Product) {
            if (parents.size() == 1) {
                return parents.get(0);
            }
        }
        return operator;
    }

    final private void addChild(Operator parent) {
        children.add(parent);
    }

    /**
     * Replace {@code this} operator by another one
     *
     * @param to The new operator
     * @return Returns {@code to}
     */
    Operator replaceBy(Operator to) {
        for (Operator child : children) {
            final List<Operator> childParents = child.getParents();
            for (int i = 0; i < childParents.size(); i++) {
                if (childParents.get(i) == this) {
                    childParents.set(i, to);
                }
            }
        }

        // Copy our children
        to.children = children;

        return to;
    }
}