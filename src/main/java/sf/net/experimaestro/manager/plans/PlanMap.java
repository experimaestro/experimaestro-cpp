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

import java.util.HashMap;

/**
 * Simplified graph of the overall plan
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 21/2/13
 */
public class PlanMap {
    /**
     * Our map
     */
    HashMap<Plan, PlanMap> map = new HashMap<>();

    /**
     * All joined operators point to the same plan map: the {@linkplain #indirection}
     * should be resolved to get the plan map that contains the operator
     */
    PlanMap indirection;

    /**
     * Number of joins (when indirection is null)
     */
    int joins = 0;

    /**
     * The associated operator
     */
    Operator operator;

    PlanMap sub(Plan plan, boolean create) {
        PlanMap submap = map.get(plan);
        if (submap == null && create) {
            map.put(plan, submap = new PlanMap());
        }
        return submap;
    }

    public PlanMap sub(Plan[] path, boolean create) {
        PlanMap current = this;
        for (int i = 0; i < path.length && current != null; i++) {
            current = current.sub(path[i], create);
        }
        return current;
    }

    Operator get() {
        return resolve().operator;
    }

    /**
     * Set the operator for this node
     */
    public Operator set(Operator value) {
        PlanMap ref = resolve();
        Operator old = ref.operator;
        ref.operator = value;
        return old;
    }

    public void join(PlanMap other) {
        // Resolve the reference
        PlanMap ref = resolve();

        // Change all the chain to the given indirection
        while (other != null) {
            assert other != ref;
            PlanMap next = other.indirection;
            other.indirection = ref;
            other = next;
            ref.joins++;
        }
    }

    private PlanMap resolve() {
        PlanMap current = this;
        while (current.indirection != null)
            current = current.indirection;
        return current;
    }

//    /**
//     * Retrieve all joined operators
//     *
//     * @param operators
//     */
//    public void getJoins(Set<Operator> operators) {
//        for (PlanMap sub : map.values())
//            sub.getJoins(operators);
//        if (indirection != null)
//            operators.add(get());
//    }
//
//    public void getOperators(Set<Operator> operators) {
//        for (PlanMap sub : map.values())
//            sub.getOperators(operators);
//        operators.add(get());
//    }
}
