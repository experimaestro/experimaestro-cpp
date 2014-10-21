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


import bpiwowar.argparser.utils.Output;
import com.google.common.base.Function;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.mutable.MutableInt;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.scheduler.Resource;
import sf.net.experimaestro.utils.CachedIterable;
import sf.net.experimaestro.utils.WrappedResult;
import sf.net.experimaestro.utils.log.Logger;

import javax.xml.xpath.XPathExpressionException;
import java.io.PrintStream;
import java.util.*;

/**
 * An operator
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 20/2/13
 */
public abstract class Operator {
    final static private Logger LOGGER = Logger.getLogger();
    public PlanScope scope = new PlanScope();
    /**
     * Size of the output (1 per default)
     */
    int outputSize = 1;
    /**
     * List of mappings for context
     */
    Map<StreamReference, Integer> contextMappings = new HashMap<>();

    static Operator getSimplified(Map<Operator, Operator> simplified, Operator operator) {
        Operator tmp;
        while ((tmp = simplified.get(operator)) != null)
            operator = tmp;
        return operator;
    }

    public static void ensureConnections(Map<Operator, Operator> simplified, List<Operator> operators) {
        for (int i = 0; i < operators.size(); i++) {
            operators.set(i, getSimplified(simplified, operators.get(i)));
        }
    }

    public static void ensureConnections(final Map<Operator, Operator> simplified, Set<Operator> set) {
        ArrayList<Operator> list = new ArrayList<>(set);
        set.clear();
        for (Operator operator : Iterables.transform(list, new Function<Operator, Operator>() {
            @Override
            public Operator apply(Operator input) {
                return getSimplified(simplified, input);
            }
        })) {
            set.add(operator);
        }

    }

    /**
     * Copy a collection of operators
     */
    protected static Iterable<Operator> copy(Iterable<Operator> collection, final boolean deep, final Map<Object, Object> map) {
        return Iterables.transform(collection, new Function<Operator, Operator>() {
            @Override
            public Operator apply(Operator input) {
                return input.copy(deep, map);
            }
        });
    }

    static public Operator simplify(Operator operator) {
        HashMap<Operator, Operator> map = new HashMap<>();
        operator = simplify(operator, map);
        operator.ensureConnections(map, new HashSet<Operator>());
        return operator;
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

        // --- operator == Union
        if (operator instanceof OrderBy) {
            if (((OrderBy) operator).size() == 0) {
                return simplify(operator, parents.get(0), simplified);
            }
        }


        return operator;
    }

    private static Operator simplify(Operator operator, Operator optimised, Map<Operator, Operator> simplified) {
        simplified.put(operator, optimised);
        return optimised;
    }

    /**
     * Returns the parents of this node
     */
    public abstract List<Operator> getParents();

    public void addParent(Operator parent) {
        throw new UnsupportedOperationException();
    }

    final public Operator getParent(int i) {
        return getParents().get(i);
    }

    /**
     * Recursive initialization of operator
     */
    public Operator prepare(Map<Operator, Operator> map, OperatorMap opMap) {
        List<Operator> parents = getParents();
        for (int i = 0; i < parents.size(); i++) {
            parents.set(i, parents.get(i).prepare(map, opMap));
        }
        ensureConnections(map);
        return this;
    }

    /**
     * Prepare an operator
     *
     * @return
     * @throws XPathExpressionException
     */
    public Operator prepare() {
        return prepare(new HashMap<>(), new OperatorMap());
    }

    /**
     * Returns the size of the output
     */
    public int outputSize() {
        return outputSize;
    }

    /**
     * Copy the operator
     *
     * @param deep Deep copy
     */
    final public Operator copy(boolean deep) {
        return copy(deep, new IdentityHashMap<Object, Object>());
    }

    final protected Operator copy(boolean deep, Map<Object, Object> map) {
        Object o = map.get(this);
        if (o != null)
            return (Operator) o;
        Operator copy = doCopy(deep, map);
        map.put(this, copy);
        return copy;
    }

    protected abstract Operator doCopy(boolean deep, Map<Object, Object> map);

    public void getAncestors(HashSet<Operator> ancestors) {
        if (ancestors.contains(this))
            return;

        ancestors.add(this);
        for (Operator parent : getParents())
            parent.getAncestors(ancestors);
    }

    public Map<Resource, String> getDefaultLocks() {
        return scope.defaultLocks;
    }

    public void setDefaultLocks(Map<Resource, String> defaultLocks) {
        scope.defaultLocks = new HashMap<>(defaultLocks);
    }

    public void addParents(Operator... parents) {
        for (Operator parent : parents) {
            addParent(parent);
        }
    }

    /**
     * Creates a new iterator
     *
     * @param planContext Options
     * @return A new iterator over return values
     */
    protected abstract Iterator<ReturnValue> _iterator(PlanContext planContext);


//    CachedIterable<Value> cachedIterable;
//    PlanContext cachedOptions;

    // TODO: implement the cache
    public Iterator<Value> iterator(PlanContext planContext) {
        // Update the context with the operator environment
        planContext = planContext.add(scope);

        // No cache: just return the iterator
        if (!cacheIterator())
            return new OperatorIterator(planContext);

        // Retrieve the cached iterable or retrieve it
        CachedIterable<Value> cachedIterable = planContext.getCachedIterable(this);

        if (cachedIterable == null) {
            LOGGER.debug("Setting up a cached iterator");
            cachedIterable = new CachedIterable<>(new OperatorIterator(planContext));
            planContext.setCachedIterable(this, cachedIterable);
        } else {
            // Use the cached values
            LOGGER.debug("Using cached iterator");
        }

        return cachedIterable.iterator();
    }

    /**
     * Whether we should cache the result of the iterator to avoid recomputing the values
     */
    boolean cacheIterator() {
        return false;
    }

    /**
     * Initialize the node (called before the initialization of parents)
     *
     * @throws javax.xml.xpath.XPathExpressionException
     */
    protected void doPreInit() {
    }

    /**
     * Initialize the node  (called after the initialization of parents)
     * <p/>
     * Top-down calls (parents are initialized before)
     *
     * @param parentStreams A map from the operators from parent streams to the context index
     */
    protected void doPostInit(List<Map<Operator, Integer>> parentStreams) {
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
     * @param processed A map of already processed operators along with the returned value
     * @param needed    (input) The set of needed operator streams that should go out of this operator
     * @return A map operator -> integer
     * @throws XPathExpressionException
     */
    final private Map<Operator, Integer> init(HashMap<Operator, Map<Operator, Integer>> processed, Multimap<Operator, Operator> needed) throws XPathExpressionException {
        // Check if already visited, and if so, return cached value
        Map<Operator, Integer> cached = processed.get(this);
        if (cached != null) {
            return cached;
        }

        // Initialise the streams that we need
        doPreInit();

        // First, init the parents, and store the map
        List<Map<Operator, Integer>> list = new ArrayList<>();
        for (Operator parent : getParents()) {
            Map<Operator, Integer> parentMap = parent.init(processed, needed);
            list.add(parentMap);
        }


        // Map the previous streams
        HashMap<Operator, Integer> map = new HashMap<>();
        int count = 0;
        Collection<Operator> streams = needed.get(this);

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

        // Compute children
        Multimap<Operator, Operator> childrenMap = HashMultimap.create();
        HashSet<Operator> roots = new HashSet<>();
        computeChildren(roots, childrenMap);

        // Compute needed streams
        for (Operator root : roots)
            root.computeNeededStreams(childrenMap, needed);

        // Compute orders
        Map<Operator, Order<Operator>> orders = new HashMap<>();
        for (Operator root : roots)
            root.computeOrder(childrenMap, orders);

        init(new HashMap<Operator, Map<Operator, Integer>>(), needed);
    }

    private void computeChildren(HashSet<Operator> roots, Multimap<Operator, Operator> childrenMap) {
        if (getParents().size() == 0)
            roots.add(this);
        else for (Operator parent : getParents()) {
            // Early quit: if we already had this parent-child, this means
            // we already visited this operator
            if (!childrenMap.put(parent, this))
                return;

            parent.computeChildren(roots, childrenMap);
        }
    }

    /**
     * Top-down computation of the order: we ask our children
     * to compute their order and then compute ours
     *
     * @param childrenMap
     * @param orders
     * @return
     */
    private Order<Operator> computeOrder(Multimap<Operator, Operator> childrenMap, Map<Operator, Order<Operator>> orders) {
        if (orders.containsKey(this))
            return orders.get(this);

        Collection<Operator> children = childrenMap.get(this);

        // Get the orders needed by children
        Order<Operator> childrenOrders[] = new Order[children.size()];

        int i = 0;
        for (Operator child : children) {
            childrenOrders[i++] = child.computeOrder(childrenMap, orders);
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

    /**
     * @param childrenMap
     * @param needed
     * @return A collection of streams needed by this operator and its descendants
     */
    private Collection<Operator> computeNeededStreams(Multimap<Operator, Operator> childrenMap, Multimap<Operator, Operator> needed) {
        if (needed.containsKey(this))
            return needed.get(this);

        Collection<Operator> streams = needed.get(this);
        Collection<Operator> children = childrenMap.get(this);

        // Add the needed streams from all children
        LOGGER.debug("Adding needed streams for %s", this);
        for (Operator child : children) {
            Collection<Operator> c = child.computeNeededStreams(childrenMap, needed);
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

    /**
     * Print a graph starting from this node
     *
     * @param out The output stream
     */
    public void printDOT(PrintStream out) {
        printDOT(out, null);
    }

    /**
     * Print a graph starting from this node
     *
     * @param out    The output stream
     * @param counts Number of results output by each operator
     */
    public void printDOT(PrintStream out, Map<Operator, MutableInt> counts) {
        out.println("digraph G {");
        printDOT(out, new HashSet<Operator>(), counts);
        out.println("}");
        out.flush();
    }

    /**
     * Print a node
     *
     * @param out       The output stream
     * @param planNodes The values already processed (case of shared ancestors)
     */
    public boolean printDOT(PrintStream out, HashSet<Operator> planNodes, Map<Operator, MutableInt> counts) {
        if (planNodes.contains(this))
            return false;
        planNodes.add(this);
        printDOTNode(out, counts);
        int streamIndex = 0;
        for (Operator parent : getParents()) {
            parent.printDOT(out, planNodes, counts);
            ArrayList<Map.Entry<StreamReference, Integer>> list = new ArrayList<>();
            out.format("p%s -> p%s", System.identityHashCode(parent), System.identityHashCode(this));


            // Build the list of context mappings
            for (Map.Entry<StreamReference, Integer> x : contextMappings.entrySet()) {
                if (x.getKey().inputIndex == streamIndex) {
                    list.add(x);
                }
            }

            String labelValue = Integer.toString(streamIndex);
            if (!list.isEmpty()) {
                labelValue += ";" + Output.toString(", ", list, x -> String.format("%d/%d", x.getKey().contextIndex, x.getValue()));
            }

            Map<String, String> attributes = new HashMap<>();
            attributes.put("label", labelValue);

            out.print("[");
            Output.print(out, ", ", attributes.entrySet(), o -> String.format("%s=\"%s\"", o.getKey(), o.getValue()));
            out.println("];");
            streamIndex++;
        }


        return true;
    }

    @Override
    public String toString() {
        return String.format("%s [%s]", getName(), System.identityHashCode(this));
    }

    protected void printDOTNode(PrintStream out, Map<Operator, MutableInt> counts) {
        String attribute = "";
        StringBuilder label = new StringBuilder();
        label.append(getName());

        // If the stream is used in a join, make it dashed
        for (StreamReference x : contextMappings.keySet())
            if (x.inputIndex == -1) {
                attribute = ", style=\"dashed\"";
                break;

            }

        // Verify that each child has this in its parents
        if (counts != null) {
            MutableInt outCount = counts.get(this);
            if (outCount != null) {
                label.append("\\n# = " + outCount.intValue());
                if (outCount.intValue() > 0)
                    attribute += ", peripheries=2";

            }
        }

        label.append("\\n s=" + outputSize());
        label.append("\\n" + System.identityHashCode(this));

        out.format("p%s [label=\"%s\"%s];%n", System.identityHashCode(this), label.toString(),
                attribute);
    }

    protected String getName() {
        return this.getClass().getName();
    }

    private void ensureConnections(HashMap<Operator, Operator> simplified, HashSet<Operator> visited) {
        if (visited.contains(this))
            return;
        visited.add(this);
        for (Operator parent : getParents())
            parent.ensureConnections(simplified, visited);

        ensureConnections(simplified);
    }


    // --- Simplify ---

    /**
     * After changes, this is used ensure that references to changed operators
     * are kept
     *
     * @param map The map for changed operators
     */
    protected void ensureConnections(Map<Operator, Operator> map) {
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
        Json nodes[];
        Contexts contexts;

        public ReturnValue(Contexts contexts, Json... nodes) {
            this.nodes = nodes;
            this.contexts = contexts;
        }
    }

    public class OperatorIterator extends AbstractIterator<Value> {
        private final MutableInt counter;
        Iterator<ReturnValue> iterator;
        private long id = 0;

        OperatorIterator(PlanContext planContext) {
            iterator = _iterator(planContext);
            if (planContext.counts() != null)
                planContext.counts().put(Operator.this, this.counter = new MutableInt(0));
            else
                this.counter = null;
        }

        @Override
        final protected Value computeNext() {
            // End of stream
            if (!iterator.hasNext())
                return endOfData();

            // Increment the counter (if used)
            if (counter != null)
                counter.increment();

            // Get the next value and sets its id
            ReturnValue next = iterator.next();
            Value value = new Value(next.nodes);
            value.id = id++;

            // Copy context
            if (!contextMappings.isEmpty()) {
                value.context = new long[contextMappings.size()];
                for (Map.Entry<StreamReference, Integer> entry : contextMappings.entrySet()) {
                    StreamReference key = entry.getKey();
                    value.context[entry.getValue()] = key.inputIndex < 0 ?
                            value.id : next.contexts.get(key.inputIndex, key.contextIndex);
                }

            }

            return value;
        }

    }


}