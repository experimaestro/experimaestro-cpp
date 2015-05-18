package sf.net.experimaestro.manager.js;

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

import jdk.nashorn.internal.objects.NativeObject;
import org.mozilla.javascript.NativeArray;
import sf.net.experimaestro.manager.plans.Constant;
import sf.net.experimaestro.manager.plans.Operator;
import sf.net.experimaestro.manager.scripting.Expose;
import sf.net.experimaestro.manager.scripting.Help;
import sf.net.experimaestro.utils.JSUtils;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 10/3/13
 * @deprecated Use constant
 */
@Deprecated
public class JSPlanInput extends JSAbstractOperator {
    Operator operator;

    @Expose
    public JSPlanInput(NativeArray array) {
        this(null, array);
    }

    @Expose
    public JSPlanInput(String name, NativeArray array) {
        Constant constant = new Constant(name);
        this.operator = constant;
        for (Object o : array) {
            constant.add(JSUtils.toJSON(null, o));
        }
    }

    @Expose
    @Help("Adds a JSON value to the plan input")
    public void add(NativeObject o) {
        ((Constant)operator).add(JSUtils.toJSON(null, o));
    }

    @Override
    Operator getOperator() {
        return operator;
    }
}
