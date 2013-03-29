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

import com.google.common.collect.Iterables;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.WrappedException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.exceptions.XPMRhinoException;
import sf.net.experimaestro.manager.DotName;
import sf.net.experimaestro.manager.Manager;
import sf.net.experimaestro.manager.TaskFactory;
import sf.net.experimaestro.manager.ValueType;
import sf.net.experimaestro.manager.plans.Constant;
import sf.net.experimaestro.manager.plans.Operator;
import sf.net.experimaestro.manager.plans.Plan;
import sf.net.experimaestro.manager.plans.PlanInputs;
import sf.net.experimaestro.manager.plans.RunOptions;
import sf.net.experimaestro.utils.JSUtils;

import javax.xml.xpath.XPathExpressionException;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * A JS wrapper around {@linkplain sf.net.experimaestro.manager.plans.Plan}
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 7/2/13
 */
public class JSPlan extends JSAbstractOperator implements Callable {
    /**
     * The wrapped operators
     */
    Plan plan;

    /**
     * Builds a wrapper around a operators
     *
     * @param plan
     */
    public JSPlan(Plan plan) {
        this.plan = plan;
    }

    /**
     * Build a operators from a {@linkplain sf.net.experimaestro.manager.TaskFactory} and a JSON object
     *
     * @param factory
     * @param object
     */
    public JSPlan(Scriptable scope, TaskFactory factory, NativeObject object) throws XPathExpressionException {
        plan = new Plan(factory);
        plan.add(getMappings(object, scope));
    }

    public JSPlan(TaskFactory factory) {
        this.plan = new Plan(factory);
    }


    /**
     * Get the mappings out of a native object
     *
     * @param object
     * @param scope
     * @return
     * @throws XPathExpressionException
     */
    private PlanInputs getMappings(NativeObject object, Scriptable scope) throws XPathExpressionException {
        PlanInputs inputs = new PlanInputs();
        return getMappings(inputs, DotName.EMPTY, object, scope);
    }

    private PlanInputs getMappings(PlanInputs inputs, DotName prefix, NativeObject object, Scriptable scope) throws XPathExpressionException {
        for (Object _id : object.getIds()) {
            final String name = JSUtils.toString(_id);
            DotName id = new DotName(prefix, DotName.parse(name));

            final Object value = JSUtils.unwrap(object.get(name, object));

            try {
                if (value instanceof NativeArray) {
                    final NativeArray array = (NativeArray) value;
                    for (int i = 0; i < array.getLength(); i++) {
                        final Object e = array.get(i);
                        inputs.set(id, getSimple(e, scope));
                    }
                } else if (value instanceof NativeObject) {
                    getMappings(inputs, id, (NativeObject) value, scope);
                } else
                    inputs.set(id, getSimple(value, scope));

            } catch (XPMRhinoException | ExperimaestroRuntimeException e) {
                e.addContext("While setting %s", id);
                throw e;
            }
        }
        return inputs;
    }


    /**
     * Returns a mapping for the given value
     *
     * @param value The value
     * @param scope
     * @return The object (String or XML fragment) or <tt>null</tt>
     */
    Operator getSimple(Object value, Scriptable scope) throws XPathExpressionException {
        value = JSUtils.unwrap(value);

        // --- Constants

        if (value instanceof Integer) {
            return new Constant(ValueType.wrap(Manager.EXPERIMAESTRO_NS, "value", (Integer) value));
        }

        if (value instanceof Double) {
            // Because rhino returns doubles for any number
            if ((((Double) value).longValue()) == ((Double) value).doubleValue())
                return new Constant(ValueType.wrap(Manager.EXPERIMAESTRO_NS, "value", (Long) ((Double) value).longValue()));
            return new Constant(ValueType.wrap(Manager.EXPERIMAESTRO_NS, "value", (Double) value));
        }

        if (value instanceof Boolean) {
            return new Constant(ValueType.wrap(Manager.EXPERIMAESTRO_NS, "value", (Boolean) value));
        }

        if (value instanceof String) {
            return new Constant(ValueType.wrapString(Manager.EXPERIMAESTRO_NS, "value", (String) value, null));
        }

        if (JSUtils.isXML(value)) {
            return new Constant(JSUtils.toDocument(null, value));
        }

        if (value instanceof XMLSerializable)
            return new Constant(((XMLSerializable) value).serialize());

        if (value instanceof JSAbstractOperator) {
            return ((JSAbstractOperator) value).getOperator();
        }

        if (value instanceof JSNodeList) {
            return new Constant(Iterables.transform((JSNodeList) value, new com.google.common.base.Function<Node, Document>() {
                @Override
                public Document apply(Node input) {
                    return Manager.wrap(input);
                }
            }));

        }

        if (value instanceof Document)
            return new Constant((Document)value);

        // --- Plans & transformations

        // Case of operators or converter of operators
        if (value instanceof JSOperator)
            return ((JSOperator) value).getOperator();

        throw new XPMRhinoException("Cannot handle type " + value.getClass());

    }

    @JSFunction(value = "run", scope = true)
    public Object run(Context context, Scriptable scope) throws XPathExpressionException {
        return run(context, scope, false);
    }

    @JSFunction(scope = true)
    public Object simulate(Context context, Scriptable scope) throws XPathExpressionException {
        return run(context, scope, false);
    }

    private Object run(Context context, Scriptable scope, boolean simulate) throws XPathExpressionException {
        final Iterator<Node> iterator = plan.run(new RunOptions(simulate));
        ArrayList<Object> values = new ArrayList<>();

        while (iterator.hasNext()) {
            values.add(new JSNode(iterator.next()));
        }

        return context.newArray(scope, values.toArray());
    }


    @Override
    public String getClassName() {
        return "Plan";
    }

    @Override
    public Object getDefaultValue(Class<?> typeHint) {
        return String.format("Plan(%s)", plan.getFactory().getId());
    }

    @Override
    public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        try {
            return run(cx, scope);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        } catch (Throwable t) {
            throw new WrappedException(t);
        }
    }

    @JSFunction("to_dot")
    public String toDot(boolean simplify) throws XPathExpressionException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        Operator operator = plan.prepare();
        if (simplify)
            operator = Operator.simplify(operator);
        operator.printDOT(ps);
        return baos.toString();
    }

    private Plan[][] getPlanPaths(Object[] args) {
        Plan paths[][] = new Plan[args.length][];
        for (int i = 0; i < args.length; i++) {
            final Object object = JSUtils.unwrap(args[i]);
            if (object instanceof JSPlan) {
                paths[i] = new Plan[]{((JSPlan) object).plan};
            } else if (object instanceof NativeArray) {
                NativeArray path = (NativeArray) object;
                if (path.getLength() > Integer.MAX_VALUE)
                    throw new AssertionError("Array length above java capacity");
                paths[i] = new Plan[(int) path.getLength()];
                for (int j = 0; j < paths[i].length; j++) {
                    paths[i][j] = ((JSPlan) JSUtils.unwrap(path.get(j))).plan;
                }
            } else
                throw new XPMRhinoException("Cannot handle argument of type %s in join()", object.getClass());

        }
        return paths;
    }


    @JSFunction(value = "add", scope = true)
    public void add(Context cx, Scriptable scope, NativeObject object) throws XPathExpressionException {
        plan.add(getMappings(object, scope));
    }

    @Override
    Operator getOperator() {
        return plan;
    }


}
