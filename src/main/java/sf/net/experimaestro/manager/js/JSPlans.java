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
import sf.net.experimaestro.manager.Plan;

import javax.xml.xpath.XPathExpressionException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * A set of plans
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 13/2/13
 */
public class JSPlans extends JSBaseObject {
    ArrayList<Plan> plans = new ArrayList<>();

    @JSFunction("add")
    public void add(JSPlan jsplan) {
        plans.add(jsplan.plan);
    }

    @JSFunction("run")
    public ArrayList<Node> run() throws XPathExpressionException {
        ArrayList<Node> result = new ArrayList<>();
        for(Plan plan: plans) {
            final Iterator<Node> nodes = plan.run();
            while (nodes.hasNext()) {
                result.add(nodes.next());
            }
        }
        return result;
    }
}
