package sf.net.experimaestro.manager.plans;

import com.google.common.collect.AbstractIterator;
import com.google.common.primitives.Ints;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.apache.commons.lang.mutable.MutableInt;
import org.testng.Assert;
import org.testng.annotations.Test;
import sf.net.experimaestro.manager.TaskContext;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.manager.json.JsonString;
import sf.net.experimaestro.utils.Output;

import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.util.*;

import static java.lang.String.format;
import static sf.net.experimaestro.manager.plans.LatticeNode.Edge;
import static sf.net.experimaestro.manager.plans.LatticeNode.MergeResult;

/**
 * Tests for the Lattice
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class LatticeNodeTest {

    static BitSet set(int... ints) {
        final BitSet bitSet = new BitSet();
        for (int i : ints)
            bitSet.set(i);
        return bitSet;
    }

    static void add(Lattice lattice, Operator operator, Operator... parents) {
        BitSet bitset = new BitSet();
        for (Operator lca : parents) {
            bitset.set(lattice.opMap.add(lca));
        }
        operator.addParents(parents);
        lattice.add(bitset, operator);
//        System.err.format("=== %s%n", bitset);
//        print(lattice);
    }

    private static void print(Lattice lattice) {
        System.err.println("[Lattice]");
        for (Triplet triplet : unroll(lattice)) {
            System.err.println(triplet);
        }
    }

    static Triplets unroll(Lattice lattice) {
        Triplets set = new Triplets(lattice.opMap);
        unroll(lattice.opMap, lattice.root, set);
        return set;
    }

    static void unroll(OperatorMap opMap, LatticeNode node, HashSet<Triplet> list) {
        for (Edge parent : node.parents) {
            Triplet triplet = new Triplet(node.set, parent.node.set,
                    parent.node.operators.stream().map(op -> opMap.add(op)).iterator());
            list.add(triplet);
            unroll(opMap, parent.node, list);
        }
    }

    @Test
    public void simpleTest() {
        final OperatorMap opMap = new OperatorMap();
        Lattice lattice = new Lattice(opMap);

        final FakeOperator a = add(lattice, "A", set(0, 1, 2));
        final FakeOperator b = add(lattice, "B", set(0));
        final FakeOperator c = add(lattice, "C", set(1));
        final FakeOperator d = add(lattice, "D", set(2));

        final Triplets set = unroll(lattice);

        set.checkAndRemove(set(), set(0, 1, 2), a);

        set.checkAndRemove(set(0, 1, 2), set(0), b);
        set.checkAndRemove(set(0, 1, 2), set(1), c);
        set.checkAndRemove(set(0, 1, 2), set(2), d);

        set.checkEmpty();
    }

    private FakeOperator add(Lattice lattice, String id, BitSet set) {
        final FakeOperator a = new FakeOperator(id);
        lattice.add(set, a);
        return a;
    }

    @Test
    public void LatticeTest() {
        final OperatorMap opMap = new OperatorMap();
        Lattice lattice = new Lattice(opMap);

        final FakeOperator a = add(lattice, "A", set(0));
        final FakeOperator b = add(lattice, "B", set(0, 1, 2));
        final FakeOperator c = add(lattice, "C", set(1, 2));
        final FakeOperator d = add(lattice, "D", set(2));
        final FakeOperator e = add(lattice, "E", set(0, 2));
        final FakeOperator f = add(lattice, "F", set(1, 2));

        Triplets set = unroll(lattice);
//        for (Triplet triplet : set) {
//            System.err.println(triplet);
//        }

        set.checkAndRemove(set(), set(0, 1, 2), b);

        set.checkAndRemove(set(0, 1, 2), set(0, 2), e);
        set.checkAndRemove(set(0, 1, 2), set(1, 2), c, f);

        set.checkAndRemove(set(0, 2), set(0), a);

        set.checkAndRemove(set(0, 2), set(2), d);
        set.checkAndRemove(set(1, 2), set(2), d);

        assert set.isEmpty() : "Set is not empty";
    }

    @Test(description = "Simple merge c0 -> {op0, op1} -> op")
    public void SimpleOperatorTest() throws XPathExpressionException {

        PlanContext pc = new PlanContext(new TaskContext(null, null, null, null));
        OperatorMap opMap = new OperatorMap();
        LatticeNode root = new LatticeNode();

        final Constant c0 = new Constant(json("A"), json("B"));
        final int c0_nid = opMap.add(c0);

        final FakeOperator op0 = new FakeOperator(opMap);
        final FakeOperator op1 = new FakeOperator(opMap);

        op0.addParents(c0);
        root.add(set(c0_nid), op0);

        op1.addParents(c0);
        root.add(set(c0_nid), op1);

        final MergeResult result = root.merge(opMap);

        final FakeOperator op = new FakeOperator(opMap);
        op.addParents(result.operator);

        op.prepare();
        op.init();
        final Iterator<Value> iterator = op.iterator(pc);
        HashSet<String> set = new HashSet<>();
        while (iterator.hasNext()) {
            set.add(iterator.next().getNodes()[0].toString());
        }

        assert set.remove("AA");
        assert set.remove("BB");
        assert set.isEmpty();

    }

    @Test(description = "Test that the output of a plan is OK")
    public void FlatOperatorSimpleTest() throws XPathExpressionException {
        PlanContext pc = new PlanContext(new TaskContext(null, null, null, null));
        OperatorMap opMap = new OperatorMap();
        Lattice lattice = new Lattice(opMap);

        // inputs
        final Constant c0 = new Constant(json("A"), json("B"));
        final Constant c1 = new Constant(json("C"), json("D"));

        // c0, c1 -> op0
        final FakeOperator op0 = new FakeOperator(opMap);
        add(lattice, op0, c0, c1);

        // c0 -> op1
        final FakeOperator op1 = new FakeOperator(opMap);
        add(lattice, op1, c0);

        // c1 -> op2
        final FakeOperator op2 = new FakeOperator(opMap);
        add(lattice, op2, c1);

        // Checks the lattice structure
        final Triplets triplets = unroll(lattice);

        triplets.checkAndRemove(opMap.setOf(), opMap.setOf(c0, c1), op0);
        triplets.checkAndRemove(opMap.setOf(c0, c1), opMap.setOf(c0), op1);
        triplets.checkAndRemove(opMap.setOf(c0, c1), opMap.setOf(c1), op2);

        triplets.checkEmpty();

        // op0 ... op5 -> op6
        final MergeResult result = lattice.merge();
        final FakeOperator op = new FakeOperator(opMap);
        op.addParent(result.operator);

        // Checks the ancestors

        op.prepare();
        op.init();

        final Iterator<Value> iterator = op.iterator(pc);
        HashSet<String> set = new HashSet<>();
        while (iterator.hasNext()) {
            final String e = iterator.next().getNodes()[0].toString();
            set.add(e);
        }

        check(set, result.map, new String[]{"AC", "A", "C"}, op0, op1, op2);
        check(set, result.map, new String[]{"BC", "B", "C"}, op0, op1, op2);
        check(set, result.map, new String[]{"AD", "A", "D"}, op0, op1, op2);
        check(set, result.map, new String[]{"BD", "B", "D"}, op0, op1, op2);

        assert set.isEmpty();
    }

    private void check(HashSet<String> set, IdentityHashMap<Operator, Integer> map, String[] outputs, Operator... operators) {
        String[] strings = new String[operators.length];

        for (int i = 0; i < operators.length; i++) {
            int index = map.get(operators[i]);
            strings[index] = outputs[i];
        }

        final String s = Arrays.stream(strings).reduce((a, b) -> a + b).get();
        assert set.remove(s) : format("%s is not in returned set", s);
    }

    @Test(description = "Test that the output of a plan is OK")
    public void FlatOperatorTest() throws XPathExpressionException, IOException, InterruptedException {
        PlanContext pc = new PlanContext(new TaskContext(null, null, null, null));
        OperatorMap opMap = new OperatorMap();
        Lattice lattice = new Lattice(opMap);

        // inputs
        final Constant c0 = new Constant(json("A"), json("B"));
        final Constant c1 = new Constant(json("C"), json("D"));
        final Constant c2 = new Constant(json("E"), json("F"));
        final Constant c3 = new Constant(json("G"), json("H"));
        final Constant c4 = new Constant(json("I"), json("J"));
        final Constant c5 = new Constant(json("K"), json("L"));
        final Constant c6 = new Constant(json("M"), json("N"));

        // c0, c1 -> op0
        final FakeOperator op0 = new FakeOperator(opMap);
        add(lattice, op0, c0, c1);

        // c0 -> op1
        final FakeOperator op1 = new FakeOperator(opMap);
        add(lattice, op1, c0);

        // c2, c3 -> op2
        final FakeOperator op2 = new FakeOperator(opMap);
        add(lattice, op2, c2, c3);

        // c1, c4 -> op3
        final FakeOperator op3 = new FakeOperator(opMap);
        add(lattice, op3, c1, c4);

        // c1, c5 -> op4
        final FakeOperator op4 = new FakeOperator(opMap);
        add(lattice, op4, c1, c5);

        // c2, c3, c4, c5 -> op5
        final FakeOperator op5 = new FakeOperator(opMap);
        add(lattice, op5, c2, c3, c4, c5);

        // c1, c4, c5 -> op6
        final FakeOperator op6 = new FakeOperator(opMap);
        add(lattice, op6, c1, c4, c5);

        // c6 -> op7
        add(lattice, c6);

        // Checks the lattice structure
        final Triplets triplets = unroll(lattice);

        triplets.checkAndRemove(opMap.setOf(), opMap.setOf(c0, c1), op0);
        triplets.checkAndRemove(opMap.setOf(), opMap.setOf(c2, c3, c4, c5), op5);
        triplets.checkAndRemove(opMap.setOf(), opMap.setOf(c1, c4, c5), op6);

        triplets.checkAndRemove(opMap.setOf(c0, c1), opMap.setOf(c0), op1);

        triplets.checkAndRemove(opMap.setOf(c2, c3, c4, c5), opMap.setOf(c2, c3), op2);

        triplets.checkAndRemove(opMap.setOf(c1, c4, c5), opMap.setOf(c1, c4), op3);
        triplets.checkAndRemove(opMap.setOf(c1, c4, c5), opMap.setOf(c1, c5), op4);

        triplets.checkEmpty();

        // op0 ... op5 -> op6
        final MergeResult result = lattice.merge();
        final FakeOperator op = new FakeOperator(opMap);
        op.addParent(result.operator);

        // Checks the ancestors

        op.prepare();
        op.init();

        final Iterator<Value> iterator = op.iterator(pc);
        HashSet<String> set = new HashSet<>();
        while (iterator.hasNext()) {
            final String e = iterator.next().getNodes()[0].toString();
            set.add(e);
        }

        // Check all
        for (String s0 : new String[]{"A", "B"}) {
            for (String s1 : new String[]{"C", "D"}) {
                for (String s2 : new String[]{"E", "F"}) {
                    for (String s3 : new String[]{"G", "H"}) {
                        for (String s4 : new String[]{"I", "J"}) {
                            for (String s5 : new String[]{"K", "L"}) {
                                for (String s6 : new String[]{"M", "N"}) {
                                    check(set, result.map, new String[]{
                                                    s0 + s1,
                                                    s0,
                                                    s2 + s3,
                                                    s1 + s4,
                                                    s1 + s5,
                                                    s2 + s3 + s4 + s5,
                                                    s1 + s4 + s5,
                                                    s6
                                            },
                                            op0, op1, op2, op3, op4, op5, op6, c6);
                                }
                            }
                        }
                    }
                }
            }
        }

        Assert.assertTrue(set.isEmpty(), format("[%s]", Output.toString(", ", set)));
    }

    private void saveGraphAsPDF(FakeOperator op, String pathname) throws IOException, InterruptedException {
        final ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("dot", "-Tpdf");
        processBuilder.redirectOutput(new File(pathname));
        processBuilder.redirectInput(ProcessBuilder.Redirect.PIPE);
        final Process process = processBuilder.start();
        PrintStream out = new PrintStream(process.getOutputStream());
        op.printDOT(out);
        out.close();
        process.waitFor();
    }

    private JsonString json(String s) {
        return new JsonString(s);
    }

    static public class Triplets extends HashSet<Triplet> {

        private OperatorMap opMap;

        public Triplets(OperatorMap opMap) {
            this.opMap = opMap;
        }

        public void checkAndRemove(BitSet start, BitSet end, Operator... operators) {
            final Triplet triplet = new Triplet(start, end, opMap.setOf(operators));
            Assert.assertTrue(remove(triplet), format("Could not find edge %s in lattice", triplet));
        }

        public void checkEmpty() {
            Assert.assertTrue(isEmpty(), format("Set is not empty: [%s]", Output.toString(",", this)));
        }
    }

    static public class FakeOperator extends Product {
        private final String id;

        public FakeOperator(OperatorMap opMap) {
            this.id = format("op/%d", opMap.add(this));
        }

        public FakeOperator(String id) {
            this.id = id;
        }

        @Override
        protected String getName() {
            return "fake";
        }

        @Override
        protected Operator doCopy(boolean deep, Map<Object, Object> map) {
            return null;
        }

        @Override
        protected void doPostInit(List<Map<Operator, Integer>> parentStreams) {
            super.doPostInit(parentStreams);
            outputSize = 1;
        }

        @Override
        protected Iterator<ReturnValue> _iterator(PlanContext planContext) {
            return new FakeIterator(planContext);
        }

        @Override
        public String toString() {
            return this.id;
        }

        private class FakeIterator extends ProductIterator {
            public FakeIterator(PlanContext planContext) {
                super(planContext);
            }

            @Override
            ReturnValue getReturnValue(Value[] current) {
                String s = "";

                final long[][] contexts = new long[parents.size()][];
                for (int j = 0; j < contexts.length; j++) {
                    contexts[j] = current[j].context;
                    for (int k = 0, n = current[j].nodes.length; k < n; k++) {
                        s += current[j].nodes[k].toString();
                    }

                }

                Json[] nodes = new Json[]{new JsonString(s)};

                return new ReturnValue(new DefaultContexts(contexts), nodes);
            }

        }

    }

    static public class Triplet {
        BitSet before;
        BitSet parent;
        IntSet operators;

        public Triplet(BitSet node, BitSet parent, BitSet operators) {
            this(node, parent, operators.stream().iterator());
        }

        public Triplet(BitSet node, BitSet parent, Iterator<Integer> operators) {
            this.before = node;
            this.parent = parent;
            this.operators = new IntArraySet();
            while (operators.hasNext()) {
                this.operators.add(operators.next());
            }
        }


        @Override
        public String toString() {
            String ops = Output.toString(", ", operators, o -> o.toString());
            return format("%s -> %s [%s]", before, parent, ops);
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Triplet triplet = (Triplet) o;

            if (before != null ? !before.equals(triplet.before) : triplet.before != null) return false;
            if (parent != null ? !parent.equals(triplet.parent) : triplet.parent != null) return false;
            if (operators != null ? !operators.equals(triplet.operators) : triplet.operators != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = before != null ? before.hashCode() : 0;
            result = 31 * result + (parent != null ? parent.hashCode() : 0);
            result = 31 * result + (operators != null ? operators.hashCode() : 0);
            return result;
        }
    }


}
