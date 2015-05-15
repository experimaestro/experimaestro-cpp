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

import org.mozilla.javascript.*;
import org.w3c.dom.Node;
import sf.net.experimaestro.exceptions.XPMRhinoException;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.manager.DotName;
import sf.net.experimaestro.manager.TaskFactory;
import sf.net.experimaestro.manager.ValueType;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.manager.json.JsonArray;
import sf.net.experimaestro.manager.json.JsonString;
import sf.net.experimaestro.manager.plans.*;
import sf.net.experimaestro.manager.scripting.Expose;
import sf.net.experimaestro.manager.scripting.ScriptContext;
import sf.net.experimaestro.utils.JSUtils;

import javax.xml.xpath.XPathExpressionException;
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

    protected JSPlan() {

    }

    /**
     * Builds a wrapper around a operators
     *
     * @param plan
     */
    @Expose
    public JSPlan(Plan plan) {
        this.plan = plan;
    }

    /**
     * Build a operators from a {@linkplain sf.net.experimaestro.manager.TaskFactory} and a JSON object
     *
     * @param factory
     * @param object
     */
    @Expose(scope = true)
    public JSPlan(Context cx, Scriptable scope, TaskFactory factory, NativeObject object) throws XPathExpressionException {
        plan = new Plan(factory);
        plan.add(getMappings(object, scope));
    }

    @Expose
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
    static public PlanInputs getMappings(NativeObject object, Scriptable scope) throws XPathExpressionException {
        PlanInputs inputs = new PlanInputs();
        return getMappings(inputs, DotName.EMPTY, object, scope);
    }

    static private PlanInputs getMappings(PlanInputs inputs, DotName prefix, NativeObject object, Scriptable scope) throws XPathExpressionException {
        for (Object _id : object.getIds()) {
            final String name = JSUtils.toString(_id);
            DotName id = new DotName(prefix, DotName.parse(name));

            final Object value = JSUtils.unwrap(object.get(name, object));

            try {
                if (value instanceof NativeArray) {
                    final NativeArray array = (NativeArray) value;
                    if (array.getLength() == 0) {
                        inputs.set(id, new Constant());
                    } else {
                        for (int i = 0; i < array.getLength(); i++) {
                            final Object e = array.get(i);
                            inputs.set(id, getSimple(e, scope));
                        }
                    }
                } else
                    inputs.set(id, getSimple(value, scope));

            } catch (XPMRhinoException | XPMRuntimeException e) {
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
    static Operator getSimple(Object value, Scriptable scope) throws XPathExpressionException {
        value = JSUtils.unwrap(value);

        // --- Already an operator
        if (value instanceof Operator) {
            return (Operator) value;
        }

        // --- Constants

        if (value instanceof Integer) {
            return new Constant(ValueType.wrap((Integer) value));
        }

        if (value instanceof Double) {
            // Because rhino returns doubles for any number
            if ((((Double) value).longValue()) == (Double) value)
                return new Constant(ValueType.wrap(((Double) value).longValue()));
            return new Constant(ValueType.wrap((Double) value));
        }

        if (value instanceof Boolean) {
            return new Constant(ValueType.wrap((Boolean) value));
        }

        if (value instanceof String) {
            return new Constant(new JsonString((String) value));
        }

        if (value instanceof ConsString) {
            return new Constant(new JsonString(value.toString()));
        }

        if (value instanceof Json)
            return new Constant((Json) value);

        if (value instanceof JSAbstractOperator) {
            return ((JSAbstractOperator) value).getOperator();
        }

        if (value instanceof NativeObject)
            return new Constant(JSUtils.toJSON(scope, value));

        if (JSUtils.isXML(value)) {
            return new Constant(new JSNode(JSUtils.toDocument(scope, value, null)));
        }

        if (value instanceof Node)
            return new Constant(new JSNode((Node) value));

        // --- Plans & transformations

        // Case of a native array: we wrap its values
        if (value instanceof NativeArray) {
            NativeArray narray = (NativeArray) value;
            JsonArray array = new JsonArray();
            for (int i = 0; i < narray.getLength(); i++) {
                final Object e = narray.get(i);
                array.add(ValueType.wrap(e));
            }

            return new Constant(new Json[]{array});
        }

        throw new XPMRhinoException("Cannot handle type " + value.getClass());

    }

    @Expose(value = "run", scope = true)
    public Object run(Context context, Scriptable scope) throws XPathExpressionException {
        return run(context, scope, false);
    }

    @Expose(scope = true)
    public Object simulate(Context context, Scriptable scope) throws XPathExpressionException {
        return run(context, scope, true);
    }

    private Object run(Context context, Scriptable scope, boolean simulate) throws XPathExpressionException {
        try(final ScriptContext scriptContext = xpm().getScriptContext().copy()) {
            final Iterator<Json> iterator = plan.run(scriptContext.simulate(simulate));
            ArrayList<Object> values = new ArrayList<>();

            while (iterator.hasNext()) {
                values.add(new JSJson(iterator.next()));
            }

            return context.newArray(scope, values.toArray());
        }
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


    @Expose(value = "add", scope = true)
    public void add(Context cx, Scriptable scope, NativeObject object) throws XPathExpressionException {
        plan.add(getMappings(object, scope));
    }

    @Override
    Operator getOperator() {
        return plan;
    }


}
