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

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.BitSet;

import static sf.net.experimaestro.manager.plans.LatticeNode.MergeResult;

/**
 * Created by bpiwowar on 9/10/14.
 */
public class Lattice {
    /**
     * The operator map
     */
    final OperatorMap opMap;
    LatticeNode root = new LatticeNode();
    /**
     * Operators without any dependency
     */
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
            for (Operator op : operators) {
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
