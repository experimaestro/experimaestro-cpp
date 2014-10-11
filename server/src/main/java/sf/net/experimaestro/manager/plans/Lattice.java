package sf.net.experimaestro.manager.plans;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.BitSet;

import static sf.net.experimaestro.manager.plans.LatticeNode.Edge;
import static sf.net.experimaestro.manager.plans.LatticeNode.MergeResult;

/**
 * Created by bpiwowar on 9/10/14.
 */
public class Lattice {
    LatticeNode root = new LatticeNode();

    /** The operator map */
    final OperatorMap opMap;

    /** Operators without any dependency */
    private ArrayList<Operator> operators = new ArrayList<>();

    public Lattice(OperatorMap opMap) {
        this.opMap = opMap;
    }


    public MergeResult merge() {
        MergeResult result = root.merge(opMap);

        // Operators without context = plain product
        if (!operators.isEmpty()) {
            if (result == null && operators.size() == 1) {
                return new MergeResult(operators.get(0), operators);
            }

            Product product = new Product();
            if (result == null) {
                result = new MergeResult(null, ImmutableList.of());
            } else {
                product.addParent(result.operator);
            }
            for(Operator op: operators) {
                product.addParent(op);
                result.map.put(op, result.map.size());
            }
            result.operator = product;
        }

        return result;
    }

    public void add(BitSet bitset, Operator operator) {
        if (bitset.isEmpty()) {
            operators.add(operator);
        } else {
            root.add(bitset, operator);
        }
    }
}
