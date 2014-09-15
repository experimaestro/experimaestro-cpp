package sf.net.experimaestro.manager.plans;

import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.apache.commons.lang.NotImplementedException;
import org.testng.annotations.Test;
import sf.net.experimaestro.utils.Output;

import java.util.*;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 27/4/13
 */
public class TrieNodeTest {
    static class FakeOperator extends Operator {
        int id;

        FakeOperator(int id) {
            this.id = id;
        }

        @Override
        public List<Operator> getParents() {
            // TODO: implement getParents
            throw new NotImplementedException();
        }

        @Override
        protected Operator doCopy(boolean deep, Map<Object, Object> map) {
            // TODO: implement doCopy
            throw new NotImplementedException();
        }

        @Override
        protected Iterator<ReturnValue> _iterator(PlanContext planContext) {
            // TODO: implement _iterator
            throw new NotImplementedException();
        }
    }

    @Test
    public void simpleTest() {
        TrieNode root = new TrieNode();
        add(root, 3, 0, 1, 2);
        add(root, 0, 0);
        add(root, 1, 1);
        add(root, 2, 2);

        HashSet<Triplet> set = new HashSet<>();
        unroll(root, new IntOpenHashSet(), set);
        for(Triplet triplet: set)
            System.err.println(triplet);

        for (int i = 0; i < 3; i++) {
            // {} - {i} [i]
            check(set, new Triplet(set(), set(i), set(i)));
            // {i} - {0, 1, 2} [3]
            check(set, new Triplet(set(i), set(0,1,2), set(3)));
        }

        assert set.isEmpty() : "Set is not empty";

    }

    private void check(HashSet<Triplet> set, Triplet triplet) {
        if (!set.remove(triplet)) {
            throw new AssertionError("Could not find triplet " + triplet);
        }
    }


    static IntSet set(int... ints) {
        return new IntArraySet(ints);
    }


    void unroll(TrieNode node, IntSet set, HashSet<Triplet> list) {
        for (TrieNode.Parent parent : node.parents) {
            IntOpenHashSet parentSet = new IntOpenHashSet(set);
            parentSet.addAll(parent.set);
            Triplet triplet = new Triplet(set, parentSet, parent.node.operators);
            list.add(triplet);

            unroll(parent.node, parentSet, list);
        }
    }

    static void add(TrieNode root, int id, int... bits) {
        BitSet bitset = new BitSet();
        for (int bit : bits)
            bitset.set(bit);
        root.add(bitset, new FakeOperator(id));
    }


    static public class Triplet {
        IntSet before;
        IntSet edge;
        IntSet operators;

        public Triplet(IntSet before, IntSet edge, HashSet<Operator> operators) {
            this.before = before;
            this.edge = edge;
            this.operators = new IntArraySet(operators.size());
            for(Operator o: operators)
                this.operators.add(((FakeOperator)o).id);
        }

        public Triplet(IntSet before, IntSet edge, IntSet operators) {
            this.before = before;
            this.edge = edge;
            this.operators = operators;
        }

        @Override
        public String toString() {
            String ops = Output.toString(", ", operators, new Output.Formatter<Integer>() {
                @Override
                public String format(Integer o) {
                    return o.toString();
                }
            });
            return String.format("%s -> %s [%s]", before, edge, ops);
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Triplet triplet = (Triplet) o;

            if (before != null ? !before.equals(triplet.before) : triplet.before != null) return false;
            if (edge != null ? !edge.equals(triplet.edge) : triplet.edge != null) return false;
            if (operators != null ? !operators.equals(triplet.operators) : triplet.operators != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = before != null ? before.hashCode() : 0;
            result = 31 * result + (edge != null ? edge.hashCode() : 0);
            result = 31 * result + (operators != null ? operators.hashCode() : 0);
            return result;
        }
    }


}
