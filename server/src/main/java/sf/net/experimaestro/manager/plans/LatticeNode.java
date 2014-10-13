package sf.net.experimaestro.manager.plans;

import bpiwowar.argparser.utils.Output;
import com.google.common.collect.ImmutableList;
import sf.net.experimaestro.utils.Heap;
import sf.net.experimaestro.utils.HeapElement;
import sf.net.experimaestro.utils.IdentityHashSet;

import java.io.PrintStream;
import java.util.*;

/**
 * A lattice set (node)
 * <p>
 * We store it in inverse order (Starting by the union of all sets)
 */
final public class LatticeNode implements HeapElement<LatticeNode> {
    final BitSet set;
    final int cardinality;

    final ArrayList<Edge> parents = new ArrayList<>();
    final HashSet<Operator> operators = new HashSet<>();
    ArrayList<LatticeNode> children = new ArrayList<>();

    /**
     * The operator which is the result of a merge operation
     */
    Operator resultOperator;

    /**
     * List of original operators that were merged
     */
    LinkedList<Operator> mergedOperators = new LinkedList<>();

    /**
     * Number of operators
     */
    int nbOperators = 0;

    /**
     * The index for the heap
     */
    int heapIndex;

    public LatticeNode(BitSet set) {
        this.set = set;
        cardinality = set.cardinality();
    }

    public LatticeNode() {
        this(new BitSet());
    }

    /**
     * Merge a set of operators
     *
     * @param operators The set of operators to merge
     * @param set       The set of indices to merge upon
     * @return The merged result
     */
    static private Operator merge(OperatorMap opMap, List<Operator> operators, BitSet set) {
        // Simple case
        if (operators.size() == 1) {
            return operators.get(0);
        }

        // Simple case: no intersection, we have a product
        if (set.isEmpty()) {
            Product product = new Product();
            for (Operator parent : operators) {
                product.addParent(parent);
            }
            return product;
        }

        // Need a join
        Join join = new Join();
        // This will hold the common order between the OrderBy operators
        Order<Operator> order = new Order();

        // Build the join and the order on the common indices
        for (int opIndex = set.nextSetBit(0); opIndex != -1; opIndex = set.nextSetBit(opIndex + 1)) {
            Operator op = opMap.get(opIndex);
            join.addJoin(op);
            order.add(op);
        }

        for (Operator parent : operators) {
            // Order the results first
            OrderBy orderBy = new OrderBy(order, null);
            orderBy.addParent(parent);

            // Add the sorted stream to the join
            join.addParent(orderBy);
        }

        return join;
    }

    public void addOperator(Operator id) {
        operators.add(id);
    }

    /**
     * Add a node to the lattice
     *
     * @param opSet The set of ancestors indices
     * @param edge  The edge that was taken
     * @param edges The edges taking to opSet that need to be changed in case we find a node equal to opSet
     * @return The original lattice node to be inserted, the found lattice node or null (if not inserted)
     */
    private LatticeNode add(LatticeNode opSet, Edge edge, List<Edge> edges) {
        // If root node, always included
        final Inclusion status = isRoot() ? Inclusion.INCLUDES : Inclusion.inclusion(this, opSet);

        switch (status) {
            case DIFFERENT:
                // Connect to potential other nodes
                connect(opSet);

            case NULL:
                // Intersection is null: nothing to insert
                return null;

            case EQUALS:
                for (Edge _edge : edges) {
                    _edge.node = this;
                }

                operators.addAll(opSet.operators);
                return this;
            case INCLUDES:
                // set includes strictly opSet: we look at the parents
                boolean fullyInserted = false;

                for (Iterator<Edge> iterator = parents.iterator(); iterator.hasNext(); ) {
                    Edge parentEdge = iterator.next();
                    final LatticeNode node = parentEdge.node.add(opSet, parentEdge, edges);
                    if (node != null) {
                        if (fullyInserted) {
                            // Remove this parent since the node was already fully inserted
                            iterator.remove();
                        } else {
                            fullyInserted = true;

                            if (node != opSet) {
                                assert edges != null;
                                edges = null;
                                opSet = node;
                            }
                        }
                    }

                }

                // If the node was not fully inserted, add a new child
                if (!fullyInserted) {
                    parents.add(new Edge(opSet));
                }

                return opSet;
            case INCLUDED:
                // opSet includes (strictly) set: return this new node
                opSet.parents.add(new Edge(this));

                edge.node = opSet;

                return opSet;

        }

        assert false;
        return null;
    }

    /**
     * Connects a node to any node in this sublattice
     *
     * @param opSet The node to connect
     */
    private void connect(LatticeNode opSet) {
        switch (Inclusion.inclusion(this, opSet)) {
            case INCLUDES:
            case EQUALS:
                throw new AssertionError("Should not happen");

            case NULL:
                break;

            case INCLUDED:
                opSet.parents.add(new Edge(this));
                break;


            case DIFFERENT:
                for (Edge parent : parents) {
                    parent.node.connect(opSet);
                }
                break;
        }

    }

    /**
     * Adds a new operator to this lattice
     *
     * @param bitset   The bitset corresponding to the operator LCAs
     * @param operator The operator to add
     * @return The new root of the lattice
     */
    public void add(BitSet bitset, Operator operator) {
        final LatticeNode node = new LatticeNode(bitset);
        node.addOperator(operator);
        Edge edge = new Edge(this);
        add(node, edge, new ArrayList<>());
    }

    /**
     * DOT output (for debug)
     */
    private void outputDOT(PrintStream out, HashSet<LatticeNode> visited) {
        if (visited.contains(this))
            return;
        visited.add(this);

        out.format("p%s [label=\"%s\"];%n", System.identityHashCode(this),
                Output.toString(", ", operators, operator -> "(" + operator.getName() + "/" + Integer.toString(System.identityHashCode(operator)) + ")"));

        for (Edge parent : parents) {
            parent.node.outputDOT(out, visited);
            out.format("p%s -> p%s [label=\"%s\"];%n",
                    System.identityHashCode(this), System.identityHashCode(parent.node)
            );
        }
    }

    /**
     * O
     *
     * @param out
     */
    void outputDOT(PrintStream out) {
        out.println("digraph G {");
        outputDOT(out, new HashSet<LatticeNode>());
        out.println("}");
    }

    @Override
    public String toString() {
        return set.toString();
    }

    /**
     * Merge all the operators in the map
     *
     * @param opMap The operator map (useful to find the ID of an operator, and its ancestors)
     * @return A merged result, or null if no operator were added to the lattice
     */
    MergeResult merge(OperatorMap opMap) {
        // Special case: nothing to merge
        if (parents.isEmpty()) {
            return null;
        }

        // First, build up some information
        IdentityHashSet<LatticeNode> visited = new IdentityHashSet<>();

        // We need higher weights up in the heap tree

        Heap<LatticeNode> heap = new Heap<>(LatticeNodeComparator.INSTANCE);

        buildInformation(heap, visited);

        // Loop while we have only one operator left
        while (!heap.isEmpty()) {
            // Get the node to merge
            heap.peek().merge(opMap, heap);
        }

        assert resultOperator != null : "No result operator at root node";
        return new MergeResult(resultOperator, mergedOperators);
    }

    /**
     * Merge this node
     * <p>
     * Depending on the context, either merge the operators
     * at this node level, or the content of this node with the
     * children
     *
     * @param opMap The operator map (useful to find the ID of an operator, and its ancestors)
     * @param heap  The heap for updating priorities over nodes
     */
    private void merge(OperatorMap opMap, Heap<LatticeNode> heap) {
        boolean remove = false;

        if (isRoot()) {
            // Special case
            BitSet currentSet = new BitSet();

            for (int i = 0; i < parents.size(); i++) {
                LatticeNode parent = parents.get(i).node;
                assert parent.operators.size() == 0;
                assert parent.resultOperator != null;

                if (i == 0) {
                    resultOperator = parent.resultOperator;
                    mergedOperators = parent.mergedOperators;
                    currentSet = parent.set;
                } else {
                    BitSet intersection = (BitSet) currentSet.clone();
                    intersection.and(parent.set);
                    final ImmutableList<Operator> list = ImmutableList.of(resultOperator, parent.resultOperator);
                    resultOperator = merge(opMap, list, intersection);
                    mergedOperators.addAll(parent.mergedOperators);
                }

                currentSet.or(parent.set);

            }

            heap.remove(this);
        } else if (this.operators.size() > 1) {
            // Merge all operators at this node
            assert mergedOperators.isEmpty();
            mergedOperators.addAll(operators);
            nbOperators -= operators.size() + 1;
            operators.clear();

            resultOperator = merge(opMap, mergedOperators, set);

            if (parents.isEmpty() && isRootParent()) {
                heap.remove(this);
            } else {
                heap.update(this);
            }
        } else {
            // merge with one of the children (the first one)

            final LatticeNode child = this.children.get(0);

            assert this.parents.isEmpty() : "We should have no parent";
            assert this.operators.isEmpty() : "This node should be processed";
            assert child.operators.isEmpty() : "The child node should be processed";

            child.mergedOperators.addAll(this.mergedOperators);

            child.resultOperator = merge(opMap, ImmutableList.of(child.resultOperator, this.resultOperator), this.set);

            // Cleanup
            remove = true;
            child.operators.clear();
            nbOperators = 0;
            heap.remove(this);
        }

        // Update information in descendants
        IdentityHashSet<LatticeNode> visited = new IdentityHashSet<>();
        for (LatticeNode child : children) {
            child.updateInformation(heap, visited);

            if (remove) {
                // Remove the edges going to this node
                final Iterator<Edge> iterator = child.parents.iterator();
                while (true) {
                    final Edge edge = iterator.next();
                    if (edge.node == this) {
                        iterator.remove();
                        break;
                    }
                }
            }
        }

    }

    private boolean isRootParent() {
        return children.size() == 1 && children.get(0).isRoot();
    }

    private boolean isRoot() {
        return this.cardinality == 0;
    }

    /**
     * Update information after a merge (going down)
     *
     * @param heap    The heap for updating priorities over nodes
     * @param visited The list of visited nodes
     */
    private void updateInformation(Heap<LatticeNode> heap, IdentityHashSet<LatticeNode> visited) {
        if (visited.contains(this)) {
            return;
        }

        nbOperators = operators.size() + (resultOperator != null ? 1 : 0);
        for (Edge edge : parents) {
            nbOperators += edge.node.nbOperators;
        }

        if (nbOperators == 1) {
            // We have finished with this node
            if (getIndex() >= 0) {
                heap.remove(this);
            }
        } else {
            heap.update(this);
        }
    }


    /**
     * Build the information prior to a merge (going up)
     *
     * @param heap    The heap to be populated
     * @param visited The list of visited nodes
     */
    private void buildInformation(Heap<LatticeNode> heap, IdentityHashSet<LatticeNode> visited) {
        if (visited.contains(visited))
            return;

        if (operators.size() == 1 && resultOperator == null) {
            resultOperator = operators.iterator().next();
            mergedOperators.addAll(operators);
            operators.clear();
        }

        nbOperators = operators.size() + (resultOperator != null ? 1 : 0);
        for (Edge edge : parents) {
            edge.node.children.add(this);
            edge.node.buildInformation(heap, visited);
            nbOperators += edge.node.nbOperators;
        }

        // Don't add to the heap if one operator, no parent, and parent of root
        if (nbOperators != 1 || parents.size() != 0 || !isRootParent()) {
            heap.add(this);
        }
    }

    @Override
    public int getIndex() {
        return heapIndex;
    }

    @Override
    public void setIndex(int index) {
        heapIndex = index;
    }

    /**
     * Gives the inclusion relationship between two bitsets
     */
    static public enum Inclusion {
        /**
         * Null intersection
         */
        NULL,

        /**
         * Sets are different, but with a not empty intersection
         */
        DIFFERENT,

        INCLUDED,

        EQUALS,

        INCLUDES;

        /**
         * Return the relationship between this set and another one
         *
         * @param opSet1 The first set
         * @param opSet2 The second set
         * @return Returns the relationship (reads bs1-RELATIONSHIP-bs2)
         */
        static Inclusion inclusion(LatticeNode opSet1, LatticeNode opSet2) {
            final BitSet set_opSet = (BitSet) opSet2.set.clone();
            set_opSet.and(opSet1.set);
            final int card_set_opSet = set_opSet.cardinality();
            if (card_set_opSet == 0) {
                return Inclusion.NULL;
            }

            if (card_set_opSet == opSet2.cardinality) {
                if (card_set_opSet == opSet1.cardinality) {
                    return EQUALS;
                }
                return Inclusion.INCLUDES;
            }

            if (card_set_opSet == opSet1.cardinality) {
                return Inclusion.INCLUDED;
            }
            return Inclusion.DIFFERENT;
        }
    }

    /**
     * The result of the merge of an iterator
     */
    static public class MergeResult {
        /**
         * The operator
         */
        Operator operator;

        /**
         * Position of each initial operator in the product / join, so that
         * we can retrieve a value from one operator from the position
         */
        IdentityHashMap<Operator, Integer> map = new IdentityHashMap<>();

        public MergeResult(Operator operator, List<Operator> operators) {
            this.operator = operator;

            for (int i = 0; i < operators.size(); i++) {
                map.put(operators.get(i), i);
            }
        }

    }

    /**
     * An edge
     */
    final static public class Edge {
        LatticeNode node;

        public Edge(LatticeNode node) {
            this.node = node;
        }

        public BitSet set() {
            return node.set;
        }

        @Override
        public String toString() {
            return " -> " + node.toString();
        }
    }

    static private class InsertResult {
        LatticeNode node;
        boolean fullyInserted;

        private InsertResult(LatticeNode node, boolean fullyInserted) {
            this.node = node;
            this.fullyInserted = fullyInserted;
        }
    }

    private static class LatticeNodeComparator implements Comparator<LatticeNode> {
        public static final LatticeNodeComparator INSTANCE = new LatticeNodeComparator();

        private LatticeNodeComparator() {
        }

        @Override
        public int compare(LatticeNode a, LatticeNode b) {
            // Priority is for nodes that contain several nodes (reverse)
            int z = Integer.compare(b.operators.size(), a.operators.size());
            if (z != 0) {
                return z;
            }

            // Compare the number of parents (those with 0 parents should come first)
            z = Integer.compare(a.parents.size(), b.parents.size());
            if (z != 0) {
                return z;
            }

            // Then, this is based on number of operators (reverse)
            return Integer.compare(b.nbOperators, a.nbOperators);
        }
    }
}
