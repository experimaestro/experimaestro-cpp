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
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Fake operator that only contains a plan
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 22/2/13
 */
public class PlanReference extends Operator {
    Plan plan;

    public PlanReference(Plan plan) {
        this.plan = plan;
    }

    @Override
    public List<Operator> getParents() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Iterator<ReturnValue> _iterator() {
        throw new UnsupportedOperationException();
    }


    @Override
    public Operator init(PlanMap map, OperatorMap opMap) {
        try {
            return plan.planGraph(map.sub(plan, true), opMap);
        } catch (XPathExpressionException e) {
            throw new ExperimaestroRuntimeException(e);
        }
    }


    @Override
    public void addSubPlans(Set<Plan> set) {
        set.add(plan);
    }
}
