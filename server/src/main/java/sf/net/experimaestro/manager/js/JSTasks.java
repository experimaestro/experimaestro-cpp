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

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.apache.commons.lang.NotImplementedException;
import org.mozilla.javascript.*;
import sf.net.experimaestro.exceptions.ValueMismatchException;
import sf.net.experimaestro.exceptions.XPMRhinoException;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.manager.TaskFactory;
import sf.net.experimaestro.manager.java.JavaTaskFactory;
import sf.net.experimaestro.manager.plans.FunctionOperator;
import sf.net.experimaestro.manager.plans.MergeFunction;
import sf.net.experimaestro.manager.plans.ProductReference;
import sf.net.experimaestro.utils.JSNamespaceContext;
import sf.net.experimaestro.utils.JSUtils;

import javax.xml.xpath.XPathExpressionException;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 7/2/13
 */
public class JSTasks extends JSBaseObject implements RefCallable {
    XPMObject xpm;

    @JSFunction
    public JSTasks(XPMObject xpm) {
        this.xpm = xpm;
    }

    @JSFunction(value = "set", scope = true)
    public JSTaskFactory set(Context cx, Scriptable scope, String qname, NativeObject definition) {
        QName id = QName.parse(qname, JSUtils.getNamespaceContext(scope));
        return new TaskRef(id).set(cx, definition);
    }

    @JSFunction(value = "get", scope = true)
    public Object get(Context cx, Scriptable scope, String qname) {
        QName id = QName.parse(qname, JSUtils.getNamespaceContext(scope));
        return new TaskRef(id).get(cx);
    }
    

	@Override
    public String getClassName() {
        return "Tasks";
    }


    @Override
    public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (args.length != 1)
            throw new IllegalArgumentException("Expected only one argument");

        QName id = QName.parse(JSUtils.toString(args[0]), JSUtils.getNamespaceContext(scope));

        final Object o = new TaskRef(id).get(cx);
        if (o == null || o == NOT_FOUND)
            throw new XPMRhinoException("Cannot find task %s", id);
        return o;
    }

    @Override
    public Ref refCall(Context cx, Scriptable scope, Object[] args) {
        if (args.length != 1)
            throw new IllegalArgumentException("Expected only one argument");

        QName id = QName.parse(JSUtils.toString(args[0]), JSUtils.getNamespaceContext(scope));

        return new TaskRef(id);
    }
    
    @JSFunction(scope = true)
    public void add(Context cx, Scriptable scope, String qname, NativeObject taskDescription) {
        QName id = QName.parse(JSUtils.toString(qname), JSUtils.getNamespaceContext(scope));
        new TaskRef(id).set(cx, taskDescription);
    }

    @JSFunction(scope = true)
    @JSHelp(value = "Creates an anonymous task that will copy its input as output")
    public JSCopy copy(Context cx, Scriptable scope, String qname, NativeObject plan) throws XPathExpressionException {
        return new JSCopy(cx, scope, qname, plan);
    }

    class TaskRef extends Ref {
		private static final long serialVersionUID = 1L;
		
		private final QName id;

        public TaskRef(QName id) {
            this.id = id;
        }

        @Override
        public Object get(Context cx) {
            final TaskFactory factory = xpm.getRepository().getFactory(id);
            if (factory == null)
                return NOT_FOUND;
            final JSTaskFactory jsTaskFactory = new JSTaskFactory(factory);
            jsTaskFactory.setXPM(xpm);
            return jsTaskFactory;
        }

        @Override
        public JSTaskFactory set(Context cx, Object _value) {
            NativeObject value = (NativeObject) _value;
            final JSTaskFactory factory;
            try {
                factory = new JSTaskFactory(id, value.getParentScope(), value, xpm.getRepository());
            } catch (RhinoException e) {
                throw e;
            } catch (ValueMismatchException | RuntimeException e) {
                throw new XPMRhinoException(e);
            }
            xpm.getRepository().addFactory(factory.factory);
            return factory;
        }
    }

    @JSFunction(scope = true)
    static public JSAbstractOperator merge(Context cx, Scriptable scope, String outputType, Object... objects) {
        Int2ObjectOpenHashMap<String> map = new Int2ObjectOpenHashMap<>();
        ProductReference pr = new ProductReference();
        for (Object object : objects) {
            if (object instanceof NativeObject) {
                for (Object key : ((NativeObject) object).getIds()) {
                    Object o = ((NativeObject) object).get(key);
                    if (!(o instanceof JSAbstractOperator))
                        throw new XPMRhinoException("Cannot merge object of type " + o.getClass());
                    map.put(pr.getParents().size(), key.toString());
                    pr.addParent(((JSAbstractOperator) o).getOperator());
                }
            } else if (object instanceof JSAbstractOperator) {
                pr.addParent(((JSAbstractOperator) object).getOperator());
            } else {
                throw new XPMRhinoException("Cannot merge object of type " + object.getClass());
            }
        }

        if (pr.getParents().size() == 0)
            throw new XPMRhinoException("Merge should at least have one argument");

        if (pr.getParents().size() == 1 && map.isEmpty()) {
            return new JSOperator(pr.getParents().get(0));
        }


        QName qname = QName.parse(outputType, new JSNamespaceContext(scope));
        FunctionOperator operator = new FunctionOperator(new MergeFunction(qname, map));
        operator.addParent(pr);
        return new JSOperator(operator);
    }
}
