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

import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;

import javax.xml.xpath.XPathExpressionException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Fake operator that only contains an union of plans
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 22/2/13
 */
public class PlanReference extends Operator {
    List<Plan> plans = new ArrayList<>();

    public PlanReference(Plan plans) {
        this.plans.add(plans);
    }

    public PlanReference(List<Plan> plans) {
        this.plans = plans;
    }

    @Override
    public List<Operator> getParents() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Iterator<ReturnValue> _iterator(boolean simulate) {
        throw new UnsupportedOperationException();
    }


    @Override
    public Operator init(PlanMap map, OperatorMap opMap) {
        try {
            Operator operator = getOperator(map, opMap);
            operator.children.addAll(children);
            return operator;

        } catch (XPathExpressionException e) {
            throw new ExperimaestroRuntimeException(e);
        }
    }

    private Operator getOperator(PlanMap map, OperatorMap opMap) throws XPathExpressionException {
        if (plans.size() == 1)
            return plans.get(0).planGraph(map.sub(plans.get(0), true), opMap);

        Union union = new Union();
        for(Plan plan: plans)
            union.addParent(plan.planGraph(map.sub(plan, true), opMap));
        return union;
    }


    @Override
    public void addSubPlans(Set<Plan> set) {
        for (Plan plan : this.plans)
            set.add(plan);
    }
}
