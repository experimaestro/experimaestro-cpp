package sf.net.experimaestro.manager.plans;

/*
 * This file is part of experimaestro.
 * Copyright (c) 2014 B. Piwowarski <benjamin@bpiwowar.net>
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

import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.testng.Assert;
import org.testng.annotations.Test;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.manager.json.JsonString;
import sf.net.experimaestro.manager.scripting.ScriptContext;
import sf.net.experimaestro.manager.scripting.StaticContext;
import sf.net.experimaestro.utils.IdentityHashSet;
import sf.net.experimaestro.utils.Output;
import sf.net.experimaestro.utils.log.Logger;

import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
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
    final static public Logger LOGGER = Logger.getLogger();

    static BitSet set(int... ints) {
        final BitSet bitSet = new BitSet();
        for (int i : ints)
            bitSet.set(i);
        return bitSet;
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
    public void SimpleOperatorTest(){
        Builder builder = new Builder();

        final Constant c0 = new Constant(json("A"), json("B"));

        builder.createOperator(c0);
        builder.createOperator(c0);

        final MergeResult result = builder.merge();
        final HashSet<String> set = builder.run(result);
        assert set.remove("AA");
        assert set.remove("BB");
        assert set.isEmpty();

    }

    @Test(description = "Test that the output of a plan is OK")
    public void FlatOperatorSimpleTest(){
        Builder builder = new Builder();
        OperatorMap opMap = builder.opMap;
        Lattice lattice = builder.lattice;

        // inputs
        final Constant c0 = new Constant(json("A"), json("B"));
        final Constant c1 = new Constant(json("C"), json("D"));

        // c0, c1 -> op0
        final FakeOperator op0 = builder.createOperator(c0, c1);

        // c0 -> op1
        final FakeOperator op1 = builder.createOperator(c0);

        // c1 -> op2
        final FakeOperator op2 = builder.createOperator(c1);

        // Checks the lattice structure
        final Triplets triplets = unroll(lattice);

        triplets.checkAndRemove(opMap.setOf(), opMap.setOf(c0, c1), op0);
        triplets.checkAndRemove(opMap.setOf(c0, c1), opMap.setOf(c0), op1);
        triplets.checkAndRemove(opMap.setOf(c0, c1), opMap.setOf(c1), op2);

        triplets.checkEmpty();

        // op0 ... op5 -> op6
        final MergeResult result = lattice.merge();
        final HashSet<String> set = builder.run(result);

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

    @Test(description = "Test that joins are simplified")
    public void simplifiedJoinTest() throws XPathExpressionException, IOException, InterruptedException {
        Builder builder = new Builder();
        final Lattice lattice = builder.lattice;
        final OperatorMap opMap = builder.opMap;

        final Constant c0 = new Constant(json("A"), json("B"));
        final FakeOperator op0 = builder.createOperator(true, opMap.setOf(), c0);
        final FakeOperator op1 = builder.createOperator(true, opMap.setOf(op0), op0);

        final Order order = new Order();
        Join op2join = new Join();
        order.add(op0);
        op2join.addJoin(op0);

        OrderBy orderBy0 = new OrderBy(order, null);
        orderBy0.addParent(op0);

        OrderBy orderBy1 = new OrderBy(order, null);
        orderBy1.addParent(op1);

        op2join.addParents(orderBy0, orderBy1);
        final FakeOperator op2 = builder.createOperator(false, opMap.setOf(op0, op1), op2join);

        // Check lattice
        final Triplets triplets = unroll(lattice);

        triplets.checkAndRemove(opMap.setOf(), opMap.setOf(op0, op1), op1, op2);
        triplets.checkAndRemove(opMap.setOf(op0, op1), opMap.setOf(op0), op0);
        triplets.checkEmpty();

        // Check results
        final MergeResult result = lattice.merge();
        HashSet<String> set = builder.run(result);

        check(set, result.map, new String[]{"A", "A", "AA"}, op0, op1, op2);
        check(set, result.map, new String[]{"B", "B", "BB"}, op0, op1, op2);
        Assert.assertTrue(set.isEmpty(), format("[%s]", Output.toString(", ", set)));

        // Check that we have those joins:
        // r = op1 join(op1) op2 [simplified from join(op0, op1)]
        // r join(op0) op0
        final Join[] joins = search(result.operator, Join.class).toArray(new Join[0]);

        Assert.assertEquals(joins.length, 3);
        int count0 = 0;
        int count1 = 0;
        for(Join join: joins) {
            Assert.assertEquals(join.joins.size(), 1);
            final Operator reference = join.joins.get(0).operator;
            if (reference == op0) { ++count0; }
            else if (reference == op1) { ++count1; }
        }
        Assert.assertEquals(count0, 2);
        Assert.assertEquals(count1, 1);

    }

    static <T extends Operator> IdentityHashSet<T> search(Operator operator, Class<T> aClass) {
        IdentityHashSet<T> set = new IdentityHashSet<>();
        IdentityHashSet<Operator> visited = new IdentityHashSet<>();
        search(set, visited, operator, aClass);
        return set;
    }

    private static <T extends Operator> void search(IdentityHashSet<T> set, IdentityHashSet<Operator> visited, Operator operator, Class<T> aClass) {
        if (visited.contains(operator)) {
            return;
        }

        if (operator.getClass() == aClass) {
            set.add((T) operator);
        }

        for(Operator parent: operator.getParents()) {
            search(set, visited, parent, aClass);
        }
    }

    @Test(description = "Test that the output of a plan is OK")
    public void flatOperatorTest() throws XPathExpressionException, IOException, InterruptedException {
        Builder builder = new Builder();
        final Lattice lattice = builder.lattice;
        final OperatorMap opMap = builder.opMap;

        // inputs
        final Constant c0 = new Constant(json("A"), json("B"));
        final Constant c1 = new Constant(json("C"), json("D"));
        final Constant c2 = new Constant(json("E"), json("F"));
        final Constant c3 = new Constant(json("G"), json("H"));
        final Constant c4 = new Constant(json("I"), json("J"));
        final Constant c5 = new Constant(json("K"), json("L"));
        final Constant c6 = new Constant(json("M"), json("N"));

        // c0, c1 -> op0
        final FakeOperator op0 = builder.createOperator(c0, c1);

        // c0 -> op1
        final FakeOperator op1 = builder.createOperator(c0);

        // c2, c3 -> op2
        final FakeOperator op2 = builder.createOperator(c2, c3);

        // c1, c4 -> op3
        final FakeOperator op3 = builder.createOperator(c1, c4);

        // c1, c5 -> op4
        final FakeOperator op4 = builder.createOperator(c1, c5);

        // c2, c3, c4, c5 -> op5
        final FakeOperator op5 = builder.createOperator(c2, c3, c4, c5);

        // c1, c4, c5 -> op6
        final FakeOperator op6 = builder.createOperator(c1, c4, c5);

        // c6 -> op7
        builder.add(c6, false, null);

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
        HashSet<String> set = builder.run(result);

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

    static void saveGraphAsPDF(Operator op, String pathname) throws IOException, InterruptedException {
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

    static class Builder {
        OperatorMap opMap = new OperatorMap();
        Lattice lattice = new Lattice(opMap);

        public FakeOperator createOperator(Operator... parents) {
            return createOperator(false, opMap.setOf(true, parents), parents);
        }

        public FakeOperator createOperator(boolean selfInLCA, BitSet lca, Operator... parents) {
            final FakeOperator op = new FakeOperator("");

            add(op, selfInLCA, lca, parents);
            op.id = format("fake/%d", opMap.add(op));
            return op;
        }

        void add(Operator operator, boolean selfInLCA, BitSet lca, Operator... parents) {
            if (lca == null) {
                lca = lattice.opMap.setOf(true, parents);
            }
            operator.addParents(parents);
            if (selfInLCA) {
                lca.set(opMap.add(operator));
            }
            lattice.add(lca, operator);
        }


        public HashSet<String> run(MergeResult result){
            HashSet<String> set = new HashSet<>();
            final FakeOperator op = new FakeOperator("result");
            op.addParent(result.operator);

            op.prepare();
            op.init();
            try(final ScriptContext pc = new StaticContext(null, LOGGER.getLoggerRepository()).scriptContext()) {

                final Iterator<Value> iterator = op.iterator(pc);
                while (iterator.hasNext()) {
                    final String e = iterator.next().getNodes()[0].toString();
                    set.add(e);
                }
                return set;
            }
        }

        public MergeResult merge() {
            return lattice.merge();
        }
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
        private String id;

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
        protected Iterator<ReturnValue> _iterator(ScriptContext scriptContext) {
            return new FakeIterator(scriptContext);
        }

        @Override
        public String toString() {
            return this.id;
        }

        private class FakeIterator extends ProductIterator {
            public FakeIterator(ScriptContext scriptContext) {
                super(scriptContext);
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
            return !(operators != null ? !operators.equals(triplet.operators) : triplet.operators != null);

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
