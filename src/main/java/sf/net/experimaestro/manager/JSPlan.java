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

package sf.net.experimaestro.manager;

import org.apache.commons.lang.NotImplementedException;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Node;
import sf.net.experimaestro.manager.js.JSBaseObject;
import sf.net.experimaestro.utils.JSUtils;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * A JS wrapper around {@linkplain Plan}
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 7/2/13
 */
public class JSPlan extends JSBaseObject {
    Plan plan;

    public JSPlan(TaskFactory factory, NativeObject object) {
        plan = new Plan(factory);

        Mapping.Product mapping = new Mapping.Product();

        for (Object _id : object.getIds()) {
            final String name = JSUtils.toString(_id);
            DotName id = DotName.parse(name);

            final Object value = JSUtils.unwrap(object.get(name, object));
            if (value instanceof Integer)
                mapping.add(new Mapping.Simple(id, Integer.toString((Integer) value)));
            else
                throw new NotImplementedException(String.format("Plan value of type %s", value.getClass()));

        }

        plan.setMappings(mapping);
    }

    public Object run(Scriptable scope) {
        final Iterator<Node> iterator = plan.run();
        ArrayList<Object> values = new ArrayList<>();

        while (iterator.hasNext()) {
            final Object e4x = JSUtils.domToE4X(iterator.next(), Context.getCurrentContext(), scope);
            values.add(e4x);
        }

        return Context.getCurrentContext().newArray(scope, values.toArray());
    }
}
