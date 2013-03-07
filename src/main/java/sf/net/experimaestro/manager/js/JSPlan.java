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

import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.WrappedException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import sf.net.experimaestro.manager.DotName;
import sf.net.experimaestro.manager.Manager;
import sf.net.experimaestro.manager.TaskFactory;
import sf.net.experimaestro.manager.plans.Constant;
import sf.net.experimaestro.manager.plans.Function;
import sf.net.experimaestro.manager.plans.FunctionOperator;
import sf.net.experimaestro.manager.plans.Operator;
import sf.net.experimaestro.manager.plans.OperatorMap;
import sf.net.experimaestro.manager.plans.Plan;
import sf.net.experimaestro.manager.plans.PlanInputs;
import sf.net.experimaestro.manager.plans.PlanMap;
import sf.net.experimaestro.manager.plans.PlanReference;
import sf.net.experimaestro.manager.plans.XPathFunction;
import sf.net.experimaestro.utils.JSUtils;

import javax.xml.xpath.XPathExpressionException;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/**
 * A JS wrapper around {@linkplain sf.net.experimaestro.manager.plans.Plan}
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 7/2/13
 */
public class JSPlan extends JSBaseObject implements Callable {
    /**
     * The wrapped plan
     */
    Plan plan;

    /**
     * Builds a wrapper around a plan
     *
     * @param plan
     */
    public JSPlan(Plan plan) {
        this.plan = plan;
    }

    /**
     * Build a plan from a {@linkplain sf.net.experimaestro.manager.TaskFactory} and a JSON object
     *
     * @param factory
     * @param object
     */
    public JSPlan(Scriptable scope, TaskFactory factory, NativeObject object) throws XPathExpressionException {
        plan = new Plan(factory);
        plan.add(getMappings(object, scope));
    }



    private PlanInputs getMappings(NativeObject object, Scriptable scope) throws XPathExpressionException {
        PlanInputs inputs = new PlanInputs();
        for (Object _id : object.getIds()) {
            final String name = JSUtils.toString(_id);
            DotName id = DotName.parse(name);

            final Object value = JSUtils.unwrap(object.get(name, object));

            if (value instanceof NativeArray) {
                final NativeArray array = (NativeArray) value;
                for (int i = 0; i < array.getLength(); i++) {
                    final Object e = array.get(i);
                    inputs.set(id, getSimple(e, scope));
                }
            } else
                inputs.set(id, getSimple(value, scope));

        }
        return inputs;
    }


    /**
     * Returns a mapping for the given value
     *
     *
     *
     * @param value The value
     * @param scope
     * @return The object (String or XML fragment) or <tt>null</tt>
     */
    Operator getSimple(Object value, Scriptable scope) throws XPathExpressionException {
        value = JSUtils.unwrap(value);

        if (value instanceof Integer) {
            return wrapValue(Integer.toString((Integer) value));
        }

        if (value instanceof Double) {
            // Because rhino returns doubles for any number
            if ((((Double) value).longValue()) == ((Double) value).doubleValue())
                return wrapValue(Long.toString(((Double) value).longValue()));
            return wrapValue(Double.toString((Double) value));
        }

        // Case of plans or functions of plans
        if (value instanceof JSPlan)
            return new PlanReference(((JSPlan) value).plan);

        if (value instanceof JSPlanRef) {
            return getOperator((JSPlanRef) value, scope);
        }

        if (value instanceof JSTransform) {
            final JSTransform jsTransform = (JSTransform) value;
            final FunctionOperator operator = new FunctionOperator(jsTransform);
            for(JSPlanRef jsplanRef: jsTransform.plans)
                operator.addParent(getOperator(jsplanRef, scope));
            return operator;
        }

        if (JSUtils.isXML(value)) {
            return new Constant(JSUtils.toDocument(null, value));
        }

        return null;

    }

    /**
     * Get the operator of a Plan reference
     *
     * @param jsPlanRef
     * @param scope
     * @return
     * @throws XPathExpressionException
     */
    private Operator getOperator(JSPlanRef jsPlanRef, Scriptable scope) throws XPathExpressionException {
        if (jsPlanRef.getPath() == null || jsPlanRef.getPath().equals("."))
            return new PlanReference(jsPlanRef.plan);
        final XPathFunction function = new XPathFunction(jsPlanRef.getPath(), JSUtils.getNamespaceContext(scope));
        final FunctionOperator operator = new FunctionOperator(function);
        operator.addParent(new PlanReference(jsPlanRef.plan));
        return operator;
    }

    private Constant wrapValue(String value1) {
        return new Constant(Manager.wrap(Manager.EXPERIMAESTRO_NS, "value", value1));
    }

    @JSFunction(value = "run", scope = true)
    public Object run(Context context, Scriptable scope) throws XPathExpressionException {
        final Iterator<Node> iterator = plan.run();
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

    @JSFunction("join")
    public void join(Object... args) {
        Plan[][] paths = getPlanPaths(args);
        plan.addJoin(Arrays.asList(paths));
    }

    @JSFunction("to_dot")
    public String toDot(boolean simplify) throws XPathExpressionException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        Operator operator = plan.planGraph(new PlanMap(), new OperatorMap());
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
                ScriptRuntime.typeError0("Cannot handle argument of type " + object.getClass() + " in join()");

        }
        return paths;
    }


    @JSFunction("path")
    public JSPlanRef path(String path) {
        return new JSPlanRef(plan, path);
    }

    @JSFunction("copy")
    public JSPlan copy() {
        return new JSPlan(plan.copy());
    }

    @JSFunction("group_by")
    public JSPlan groupBy(Object... paths) {
        final Plan[][] plans = getPlanPaths(paths);
        plan.groupBy(Arrays.asList(plans));
        return this;
    }

    @JSFunction(value = "add", scope = true)
    public void add(Context cx, Scriptable scope, NativeObject object) throws XPathExpressionException {
        plan.add(getMappings(object, scope));
    }


    /**
     * JS function to transform inputs in a plan
     */
    static public class JSTransform extends JSBaseObject implements Function {
        protected final Context cx;
        protected final Scriptable scope;
        protected final Callable f;

        protected final JSPlanRef[] plans;

        public JSTransform(Context cx, Scriptable scope, Callable f, JSPlanRef[] plans) {
            this.cx = cx;
            this.scope = scope;
            this.f = f;
            this.plans = plans;
        }

        public Document f(Document[] parameters) {
            Object[] args = new Object[parameters.length];
            for (int i = 0; i < parameters.length; i++)
                args[i] = new JSNode(parameters[i]);
            return JSUtils.toDocument(scope, f.call(cx, scope, null, args));
        }
    }
}
