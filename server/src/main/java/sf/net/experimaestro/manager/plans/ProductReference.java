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

import org.apache.commons.lang.NotImplementedException;

import java.util.*;

import static sf.net.experimaestro.manager.plans.LatticeNode.MergeResult;

/**
 * A fake operator that will be replaced by a succession of
 * joins & products
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 12/3/13
 */
public class ProductReference extends NAryOperator {

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

    @Override
    public Operator prepare(Map<Operator, Operator> map, OperatorMap opMap) {
        // --- Loop over the cartesian product of the inputs
        Plan.OperatorIterable inputValues[] = new Plan.OperatorIterable[parents.size()];
        {

            int index = 0;
            for (Operator input : parents) {
                inputValues[index] = new Plan.OperatorIterable(Arrays.asList(input), map, opMap);
                index++;
            }
        }

        // Create a new operator
        Operator inputOperators[] = new Operator[inputValues.length];
        BitSet[] joins = new BitSet[inputOperators.length];

        // Process union of operators
        for (int i = inputValues.length; --i >= 0; ) {
            Plan.OperatorIterable values = inputValues[i];
            Union union = new Union();
            for (Operator operator : values) {
                union.addParent(operator);
            }

            if (union.getParents().size() == 1)
                inputOperators[i] = union.getParent(0);
            else
                inputOperators[i] = union;

            joins[i] = new BitSet();
            opMap.add(inputOperators[i]);

        }

        // TODO: build a simplified graph of operators:
        // for each input, we only have LCAs (organized in a hierarchy)


        // Process this simplified graph step-wise, by merging

        // Find LCAs and store them in a map operator ID -> inputs
        for (int i = 0; i < inputOperators.length - 1; i++) {
            for (int j = i + 1; j < inputOperators.length; j++) {
                ArrayList<Operator> lca = opMap.findLCAs(inputOperators[i], inputOperators[j]);
                for (Operator operator : lca) {
                    int key = opMap.get(operator);
                    joins[i].set(key);
                    joins[j].set(key);
                }
            }
        }

        // Build the lattice of operators
        Lattice lattice = new Lattice(opMap);
        for (int i = 0; i < joins.length; i++) {
            lattice.add(joins[i], inputOperators[i]);
        }
        MergeResult merge = lattice.merge();

        // Build the trie structure for product/joins

        int[] mapping = new int[inputOperators.length];
        for (int i = 0; i < parents.size(); i++) {
            mapping[i] = merge.map.get(inputOperators[i]);
        }

        ReorderNodes reorder = new ReorderNodes(mapping);
        reorder.addParent(merge.operator);

        return reorder;
    }
}
