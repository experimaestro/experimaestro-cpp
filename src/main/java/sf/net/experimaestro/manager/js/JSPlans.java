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

package sf.net.experimaestro.manager.js;

import org.w3c.dom.Node;
import sf.net.experimaestro.manager.plans.Operator;
import sf.net.experimaestro.manager.plans.Plan;
import sf.net.experimaestro.manager.plans.RunOptions;
import sf.net.experimaestro.manager.plans.Union;
import sf.net.experimaestro.manager.plans.Value;

import javax.xml.xpath.XPathExpressionException;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * A set of plans
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 13/2/13
 */
public class JSPlans extends JSBaseObject {
    ArrayList<Plan> plans = new ArrayList<>();

    @JSFunction("add")
    public void add(JSPlan jsplan) {
        plans.add(jsplan.plan);
    }

    @JSFunction
    public Object run() throws XPathExpressionException {
        return doRun(false, false);
    }

    @JSFunction
    public Object simulate() throws XPathExpressionException {
        return doRun(true, false);
    }

    @JSFunction
    public Object simulate(boolean details) throws XPathExpressionException {
        return doRun(true, details);
    }

    private Object doRun(boolean simulate, boolean details) throws XPathExpressionException {
        RunOptions runOptions = new RunOptions(true);
        runOptions.counts(details);

        ArrayList<Node> result = new ArrayList<>();
        Operator operator = getOperator(true, true);

        final Iterator<Value> nodes = operator.iterator(runOptions);
        while (nodes.hasNext()) {
            result.add(nodes.next().getNodes()[0]);
        }

        if (!details)
            return result;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        operator.printDOT(ps, runOptions.counts());
        ps.flush();

        return new Object[] { result, baos.toString() };

    }

    @JSFunction("to_dot")
    public String toDOT(boolean simplify, boolean initialize) throws XPathExpressionException {
        Operator operator = getOperator(simplify, initialize);


        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);

        operator.printDOT(ps);
        ps.flush();
        return baos.toString();
    }

    private Operator getOperator(boolean simplify, boolean initialize) throws XPathExpressionException {
        Union union = new Union();
        Operator operator = union;
        for (Plan plan : plans)
            union.addParent(plan.getOperator(simplify, initialize));
        if (union.getParents().size() == 1)
            operator = union.getParent(0);
        return operator;
    }
}
