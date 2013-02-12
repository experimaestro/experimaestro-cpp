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

import bpiwowar.argparser.utils.Output;
import org.apache.commons.lang.NotImplementedException;
import org.mozilla.javascript.*;
import org.mozilla.javascript.annotations.JSConstructor;
import org.mozilla.javascript.xml.XMLObject;
import org.w3c.dom.Node;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.manager.*;
import sf.net.experimaestro.utils.JSUtils;

import javax.xml.xpath.XPathExpressionException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * A JS wrapper around {@linkplain sf.net.experimaestro.manager.Plan}
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 7/2/13
 */
public class JSPlan extends XMLObject implements Callable {
    /**
     * The wrapped plan
     */
    private Plan plan;
    private String xpath;


    public JSPlan() {
    }

    public JSPlan(Plan plan, String xpath) {
        this.plan = plan;
        this.xpath = xpath;
    }

    @JSConstructor
    public void jsConstruct(JSPlan other) {
        this.plan = other.plan;
    }

    /**
     * Build a plan from a {@linkplain sf.net.experimaestro.manager.TaskFactory} and a JSON object
     *
     * @param factory
     * @param object
     */
    public JSPlan(TaskFactory factory, NativeObject object) throws XPathExpressionException {

        Mappings.Product mapping = new Mappings.Product();

        for (Object _id : object.getIds()) {
            final String name = JSUtils.toString(_id);
            DotName id = DotName.parse(name);

            final Object value = JSUtils.unwrap(object.get(name, object));

            Object o = getValue(value);
            if (o != null) {
                mapping.add(new Mappings.Simple(id, o));
            } else {
                if (value instanceof NativeArray) {
                    final NativeArray array = (NativeArray) value;
                    ArrayList<Object> objects = new ArrayList<>();

                    for (int i = 0; i < array.getLength(); i++) {
                        final Object e = array.get(i);
                        o = getValue(e);
                        if (o == null)
                            throw new NotImplementedException(String.format("Cannot handle plan value of type %s", e.getClass()));
                        objects.add(o);
                    }
                    mapping.add(new Mappings.Simple(id, objects.toArray()));
                } else
                    throw new NotImplementedException(String.format("Cannot handle plan value of type %s", value.getClass()));
            }

        }

        plan = new Plan(factory, mapping);
    }


    /**
     * Returns a mapping for the given value
     *
     * @param value The value
     * @return The object (String or XML fragment) or <tt>null</tt>
     */
    Object getValue(Object value) throws XPathExpressionException {
        value = JSUtils.unwrap(value);

        if (value instanceof Integer)
            return Integer.toString((Integer) value);

        if (value instanceof Double) {
            // Because rhino returns doubles for any number
            if ((((Double) value).longValue()) == ((Double) value).doubleValue())
                return Long.toString(((Double) value).longValue());
            return Double.toString((Double) value);
        }

        if (value instanceof JSPlan) {
            JSPlan jsplan = (JSPlan) value;
            final Mappings.Reference reference = new Mappings.Reference(jsplan.plan, ".");
            final Mappings.Connection connection = new Mappings.Connection(Mappings.IdentityFunction.INSTANCE, reference);
            return connection;
        }

        if (value instanceof JSPlanRef) {
            JSPlanRef jsplanRef = (JSPlanRef) value;
            final Mappings.Reference reference = new Mappings.Reference(jsplanRef.getPlan(), jsplanRef.xpath);
            final Mappings.Connection connection = new Mappings.Connection(Mappings.IdentityFunction.INSTANCE, reference);
            return connection;
        }

        if (value instanceof JSTransform) {
            final JSTransform jsTransform = (JSTransform) value;
            Mappings.Reference references[] = new Mappings.Reference[jsTransform.plans.length];
            for(int i = 0; i < references.length; i++)
                references[i] = new Mappings.Reference(jsTransform.plans[i].getPlan(), jsTransform.plans[i].xpath);
            final Mappings.Connection connection = new Mappings.Connection(jsTransform, references);

            return connection;
        }

        return null;

    }

    final private String toPath(List<String> path) {
        return Output.toString("/", path);
    }

    public Object run(Context context, Scriptable scope) throws XPathExpressionException {
        final Iterator<Node> iterator = plan.run();
        ArrayList<Object> values = new ArrayList<>();

        while (iterator.hasNext()) {
            final Object e4x = JSUtils.domToE4X(iterator.next(), Context.getCurrentContext(), scope);
            values.add(e4x);
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
        if (xpath != null)
            throw new IllegalAccessError("Cannot call a path reference of a plan");
        try {
            return run(cx, scope);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        } catch (Throwable t) {
            throw new WrappedException(t);
        }
    }

    @Override
    public boolean has(Context cx, Object id) {
        throw new NotImplementedException();
    }


    @Override
    public Object get(Context cx, Object id) {
        throw new NotImplementedException();
    }

    @Override
    public void put(Context cx, Object id, Object value) {
        throw new NotImplementedException();
    }

    @Override
    public boolean delete(Context cx, Object id) {
        throw new NotImplementedException();
    }

    @Override
    public Object getFunctionProperty(Context cx, String name) {
        throw new NotImplementedException();
    }

    @Override
    public Object getFunctionProperty(Context cx, int id) {
        throw new NotImplementedException();
    }

    @Override
    public Scriptable getExtraMethodSource(Context cx) {
        throw new NotImplementedException();
    }

    @Override
    public Object get(String name, Scriptable start) {
        return new JSPlanRef((xpath == null ? "" : xpath) + "/" + name);
    }

    @Override
    public Ref memberRef(Context cx, Object elem, int memberTypeFlags) {
        return new JSPlanRef((xpath == null ? "" : xpath) + "/" + JSUtils.toString(elem));
    }

    @Override
    public Ref memberRef(Context cx, Object namespace, Object elem, int memberTypeFlags) {
        return new JSPlanRef((xpath == null ? "" : xpath) + "/" + new QName(JSUtils.toString(namespace), JSUtils.toString(elem)).toString());
    }

    @Override
    public NativeWith enterWith(Scriptable scope) {
        throw new NotImplementedException();
    }

    @Override
    public NativeWith enterDotQuery(Scriptable scope) {
        throw new NotImplementedException();
    }


    // --- Path from a reference

    public class JSPlanRef extends Ref implements Callable {
        String xpath;

        public JSPlanRef(String path) {
            this.xpath = path;
        }

        @Override
        public Object get(Context cx) {
            return JSPlan.this;
        }

        @Override
        public Object set(Context cx, Object value) {
            throw new ExperimaestroRuntimeException("Cannot be set");
        }


        @Override
        public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
            assert JSPlan.this == thisObj;
            final String fname = xpath.substring(1);

            switch (fname) {
                case "join":
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
                    plan.addJoin(Arrays.asList(paths));
                    return UniqueTag.NULL_VALUE;

                case "copy":
                    return new JSPlan(plan.copy(), null);

                default:
                    throw ScriptRuntime.notFunctionError(NOT_FOUND, fname);
            }
        }

        public Plan getPlan() {
            return JSPlan.this.plan;
        }
    }


    /**
     * JS function to transform inputs in a plan
     */
    static public class JSTransform extends JSBaseObject implements Mappings.Function {
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

        @Override
        public Node f(Node[] parameters) {
            Object [] e4x_args = new Object[parameters.length];
            for(int i = 0; i < parameters.length; i++)
                e4x_args[i] = JSUtils.domToE4X(parameters[i], cx, scope);
            return JSUtils.toDOM(scope, f.call(cx, scope, null, e4x_args));
        }
    }
}
