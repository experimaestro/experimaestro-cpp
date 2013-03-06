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


import bpiwowar.argparser.utils.Formatter;
import bpiwowar.argparser.utils.Output;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.w3c.dom.Document;
import sf.net.experimaestro.utils.WrappedResult;

import javax.xml.xpath.XPathExpressionException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
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

//    /**
//     * Current iterator
//     */
//    OperatorIterator currentIterator;

//    /**
//     * Ingoing streams
//     */
//    Map<Operator, Integer> streams;
//
//    /**
//     * Get the mapping between joined operators and context index
//     */
//    public Map<Operator, Integer> getStreams() {
//        return streams == null ? Collections.<Operator, Integer>emptyMap() : Collections.unmodifiableMap(streams);
//    }

    /**
     * List of mappings for context
     */
    Map<StreamReference, Integer> contextMappings = new HashMap<>();


    public void addParent(Operator parent) {
        parent.addChild(this);
    }


    /**
     * Recursive initialization of operator
     */
    public Operator init(PlanMap map, OperatorMap opMap) {
        List<Operator> parents = getParents();
        for (int i = 0; i < parents.size(); i++) {
            parents.set(i, parents.get(i).init(map, opMap));
        }
        return this;
    }

    final public Operator getParent(int i) {
        return getParents().get(i);
    }

    public void addSubPlans(Set<Plan> set) {
        for (Operator parent : getParents())
            parent.addSubPlans(set);
    }

    static Operator getSimplified(HashMap<Operator, Operator> simplified, Operator operator) {
        Operator tmp;
        while ((tmp = simplified.get(operator)) != null)
            operator = tmp;
        return operator;
    }

    public static void ensureConnections(HashMap<Operator, Operator> simplified, List<Operator> operators) {
        for(int i = 0; i < operators.size(); i++) {
            operators.set(i, getSimplified(simplified, operators.get(i)));
        }
    }

    public static void ensureConnections(HashMap<Operator, Operator> simplified, Set<Operator> set) {
        for(Operator operator: set) {
            Operator newOperator = getSimplified(simplified, operator);
            if (newOperator != operator) {
                set.remove(operator);
                set.add(newOperator);
            }
        }
    }

    public interface Contexts {
        long get(int stream, int index);
    }

    public static class DefaultContexts implements Contexts {
        long[][] contexts;

        public DefaultContexts(long[]... contexts) {
            this.contexts = contexts;
        }

        @Override
        public long get(int stream, int index) {
            return contexts[stream][index];
        }
    }

    static public class ReturnValue {
        Document[] nodes;
        Contexts contexts;

        public ReturnValue(Contexts contexts, Document... nodes) {
            this.nodes = nodes;
            this.contexts = contexts;
        }
    }

    public class OperatorIterator extends AbstractIterator<Value> {
        boolean started = false;
        private Value current = null;

        Iterator<ReturnValue> iterator = _iterator();

        @Override
        final protected Value computeNext() {
            started = true;
            if (current != null && current.next != null)
                return current = current.next;

            if (!iterator.hasNext())
                return endOfData();

            ReturnValue next = iterator.next();
            Value newValue = new Value(next.nodes);
            if (current != null) {
                newValue.id = current.id + 1;
                current.next = newValue;
            }

            // Copy context
            if (!contextMappings.isEmpty()) {
                newValue.context = new long[contextMappings.size()];
                for (Map.Entry<StreamReference, Integer> entry : contextMappings.entrySet()) {
                    StreamReference key = entry.getKey();
                    newValue.context[entry.getValue()] = key.streamIndex < 0 ?
                            newValue.id : next.contexts.get(key.streamIndex, key.contextIndex);
                }

            }

            return current = newValue;
        }

    }

    protected abstract Iterator<ReturnValue> _iterator();


    public Iterator<Value> iterator() {
        return new OperatorIterator();
//        OperatorIterator iterator = _iterator();
//        if (currentIterator != null && !currentIterator.started)
//            iterator.current = currentIterator.current;
//        return currentIterator = iterator;
    }


//    /**
//     * Prepare the streams of an operator (last operation before running)
//     *
//     * @param request (in/out) The request in terms of streams
//     * @return
//     * @throws XPathExpressionException
//     */
//    protected Operator doPrepare(StreamRequest request) throws XPathExpressionException {
//        return this;
//    }
//    final private Operator prepare(HashSet<Operator> processed) throws XPathExpressionException {
//        if (processed.contains(this))
//            return null;
//
//        for (Operator parent : getParents()) {
//            Multiset<Operator> parentCounts = HashMultiset.create();
//        }
//
//        final Operator operator = doPrepare(null);
//
//        return operator;
//    }
//
//    public Operator prepare() throws XPathExpressionException {
//        return prepare(new HashSet<Operator>());
//    }


    /**
     * Initialize the node (called before the initalization of parents)
     *
     * @throws javax.xml.xpath.XPathExpressionException
     *
     */
    protected void doPreInit() {
    }

    /**
     * Initialize the node  (called after the initialization of parents)
     *
     * @param parentStreams A map from the operators from parent streams to the context index
     * @throws javax.xml.xpath.XPathExpressionException
     *
     */
    protected void doPostInit(List<Map<Operator, Integer>> parentStreams) throws XPathExpressionException {
    }

    /**
     * Initialize the operator.
     * <p/>
     * <ol>
     * <li>Calls the {@linkplain #doPreInit()} method</li>
     * <li>Initialize the parents</li>
     * <li>Calls the {@linkplain #doPostInit(List)} method</li>
     * </ol>
     *
     * @param processed  The set of processed operators
     * @param streamsMap (input) The set of needed operator streams that should go out of this operator
     * @return
     * @throws XPathExpressionException
     */
    final private Map<Operator, Integer> init(HashMap<Operator, Map<Operator, Integer>> processed, Multimap<Operator, Operator> streamsMap) throws XPathExpressionException {
        Map<Operator, Integer> cached = processed.get(this);
        if (cached != null)
            return cached;


        // Initialise the streams that we need
        doPreInit();

        // First, init the parents
        List<Map<Operator, Integer>> list = new ArrayList<>();
        for (Operator parent : getParents()) {
            Map<Operator, Integer> parentMap = parent.init(processed, streamsMap);
            list.add(parentMap);
        }


        // Map the previous streams
        HashMap<Operator, Integer> map = new HashMap<>();
        int count = 0;
        Collection<Operator> streams = streamsMap.get(this);

        for (Operator operator : streams) {
            for (int streamIndex = 0; streamIndex < list.size(); streamIndex++) {
                Map<Operator, Integer> parentMap = list.get(streamIndex);
                Integer contextIndex = parentMap.get(operator);
                if (contextIndex != null) {
                    contextMappings.put(new StreamReference(streamIndex, contextIndex), count);
                    map.put(operator, count);
                    count++;
                    break;
                }
            }

        }

        // Check if we should add ourselves to the context
        if (streams.contains(this)) {
            contextMappings.put(new StreamReference(-1, -1), count);
            map.put(this, count);
            count++;
        }

        doPostInit(list);

        processed.put(this, map);

        return map;
    }

    /**
     * Init our ancestors and ourselves
     */
    public void init() throws XPathExpressionException {

        Multimap<Operator, Operator> needed = HashMultimap.create();
        HashSet<Operator> roots = getRoots();

        // Compute needed streams
        for (Operator root : roots)
            root.computeNeededStreams(needed);

        // Compute orders
        Map<Operator, Order<Operator>> orders = new HashMap<>();
        for (Operator root : roots)
            root.computeOrder(orders);

        init(new HashMap<Operator, Map<Operator, Integer>>(), needed);
    }


    /**
     * Top-down computation of the order: we ask our children
     * to compute their order and then compute ours
     *
     * @param orders
     * @return
     */
    private Order<Operator> computeOrder(Map<Operator, Order<Operator>> orders) {
        if (orders.containsKey(this))
            return orders.get(this);

        // Get the orders needed by children
        Order<Operator> childrenOrders[] = new Order[children.size()];

        for(int i = 0; i < childrenOrders.length; i++) {
            childrenOrders[i] = children.get(i).computeOrder(orders);
        }

        WrappedResult<Order<Operator>> result = Order.combine(childrenOrders);

        // and combine this with our own order
        Order<Operator> order = doComputeOrder(result.get());
        orders.put(this, order);
        return order;
    }

    protected Order<Operator> doComputeOrder(Order<Operator> childrenOrder) {
        childrenOrder.remove(this);
        return childrenOrder;
    }

    private Collection<Operator> computeNeededStreams(Multimap<Operator, Operator> needed) {
        if (needed.containsKey(this))
            return needed.get(this);

        Collection<Operator> streams = needed.get(this);

        for (Operator child : children) {
            Collection<Operator> c = child.computeNeededStreams(needed);
            for (Operator op : c)
                // TODO: consider using the ancestors map to check if we need to add
                if (c != child)
                    streams.add(op);
            child.addNeededStreams(streams);
        }

        return streams;
    }

    /**
     * Add the streams neeed by this operator
     */
    protected void addNeededStreams(Collection<Operator> streams) {
    }

    private void fillRoots(HashSet<Operator> roots) {
        if (getParents().isEmpty()) {
            roots.add(this);
        } else {
            for (Operator parent : getParents()) {
                parent.fillRoots(roots);
            }
        }
    }

    public HashSet<Operator> getRoots() {
        HashSet<Operator> roots = new HashSet<>();
        fillRoots(roots);
        return roots;
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
    public boolean printDOT(PrintStream out, HashSet<Operator> planNodes) {
        if (planNodes.contains(this))
            return false;
        planNodes.add(this);
        printDOTNode(out);
        int streamIndex = 0;
        for (Operator parent : getParents()) {
            parent.printDOT(out, planNodes);
            ArrayList<Map.Entry<StreamReference, Integer>> list = new ArrayList<>();
            out.format("p%s -> p%s", System.identityHashCode(parent), System.identityHashCode(this));
            Map<String, String> attributes = new HashMap<>();
            for (Map.Entry<StreamReference, Integer> x : contextMappings.entrySet()) {
                if (x.getKey().streamIndex == streamIndex)
                    list.add(x);
            }

            if (!list.isEmpty())
                attributes.put("label", Output.toString(", ", list, new Formatter<Map.Entry<StreamReference, Integer>>() {
                    @Override
                    public String format(Map.Entry<StreamReference, Integer> x) {
                        return String.format("%d/%d", x.getKey().contextIndex, x.getValue());
                    }
                }));

            out.print("[");
            Output.print(out, ", ", attributes.entrySet(), new Formatter<Map.Entry<String, String>>() {
                @Override
                public String format(Map.Entry<String, String> o) {
                    return String.format("%s=\"%s\"", o.getKey(), o.getValue());
                }
            });
            out.println("];");
            streamIndex++;
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format("%s [%s]", getName(), System.identityHashCode(this));
    }

    protected void printDOTNode(PrintStream out) {
        String color = "";

        for (StreamReference x : contextMappings.keySet())
            if (x.streamIndex == -1) {
                color = ", color=\"red\"";
                break;

            }
        out.format("p%s [label=\"%s\"%s];%n", System.identityHashCode(this), getName(), color);
    }

    protected String getName() {
        return this.getClass().getName();
    }


    // --- Simplify ---

    static public Operator simplify(Operator operator) {
        HashMap<Operator, Operator> map = new HashMap<>();
        operator = simplify(operator, map);
        operator.ensureConnections(map, new HashSet<Operator>());
        return operator;
    }

    private void ensureConnections(HashMap<Operator, Operator> simplified, HashSet<Operator> visited) {
        if (visited.contains(this))
            return;
        visited.add(this);
        for(Operator parent: getParents())
            parent.ensureConnections(simplified, visited);

        ensureConnections(simplified);
    }

    /**
     * After simplifications, this is used ensure that connections are kept
     * @param simplified
     */
    protected void ensureConnections(HashMap<Operator, Operator> simplified) {
    }

    static public Operator simplify(Operator operator, Map<Operator, Operator> simplified) {
        Operator cache = simplified.get(simplified);
        if (cache != null)
            return cache;

        // --- First, simplify all the parents
        List<Operator> parents = operator.getParents();
        for (int i = 0; i < parents.size(); i++) {
            Operator newParent = simplify(parents.get(i), simplified);
            if (newParent != parents.get(i)) {
                parents.set(i, newParent);
                newParent.addChild(operator);
            }
        }

        // --- operator == Union
        if (operator instanceof Union) {
            if (parents.size() == 1) {
                return simplify(operator, parents.get(0), simplified);
            }
        }

        // --- operator == Product
        if (operator instanceof Product) {
            if (parents.size() == 1) {
                return simplify(operator, parents.get(0), simplified);
            }
        }
        return operator;
    }

    private static Operator simplify(Operator operator, Operator optimised, Map<Operator, Operator> simplified) {
        simplified.put(operator, optimised);
        return optimised;
    }

    final private void addChild(Operator parent) {
        children.add(parent);
    }

}