package sf.net.experimaestro.manager.scripting;

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

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.mozilla.javascript.*;
import sf.net.experimaestro.exceptions.ValueMismatchException;
import sf.net.experimaestro.exceptions.XPMRhinoException;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.manager.TaskFactory;
import sf.net.experimaestro.manager.js.JavaScriptContext;
import sf.net.experimaestro.manager.js.JavaScriptTaskFactory;
import sf.net.experimaestro.manager.plans.Copy;
import sf.net.experimaestro.manager.plans.FunctionOperator;
import sf.net.experimaestro.manager.plans.Operator;
import sf.net.experimaestro.manager.plans.ProductReference;
import sf.net.experimaestro.manager.plans.functions.MergeFunction;
import sf.net.experimaestro.utils.JSUtils;

import java.util.Map;

/**
 * Access to the tasks
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Exposed
public class Tasks {
    @Expose
    public Tasks() {
    }

    @Expose(context = true)
    static public Operator merge(LanguageContext cx, String outputType, Object... objects) {
        Int2ObjectOpenHashMap<String> map = new Int2ObjectOpenHashMap<>();
        ProductReference pr = new ProductReference();
        for (Object object : objects) {
            if (object instanceof Map) {
                for (Map.Entry<?, ?> entry : ((Map<?, ?>)object).entrySet()) {
                    Object o = entry.getValue();
                    if (!(o instanceof Operator))
                        throw new XPMRhinoException("Cannot merge object of type " + o.getClass());
                    map.put(pr.getParents().size(), entry.getKey().toString());
                    pr.addParent(((Operator) o));
                }
            } else if (object instanceof Operator) {
                pr.addParent(((Operator) object));
            } else {
                throw new XPMRhinoException("Cannot merge object of type " + object.getClass());
            }
        }

        if (pr.getParents().size() == 0)
            throw new XPMRhinoException("Merge should at least have one argument");

        if (pr.getParents().size() == 1 && map.isEmpty()) {
            return pr.getParents().get(0);
        }


        QName qname = QName.parse(outputType, cx.getNamespaceContext());
        FunctionOperator operator = new FunctionOperator(new MergeFunction(qname, map));
        operator.addParent(pr);
        return operator;
    }

    @Expose(value = "set", context = true)
    public void set(LanguageContext cx, String qname, NativeObject definition) {
        QName id = QName.parse(qname, cx.getNamespaceContext());
        final TaskRef taskRef = new TaskRef(id);
        taskRef.set(cx, definition);
    }

    @Expose(context = true, mode = ExposeMode.CALL)
    public Object get(LanguageContext cx, String qname) {
        QName id = cx.qname(qname);
        return new TaskRef(id).get(cx);
    }

//
//    public Object call(LanguageContext cx, Scriptable thisObj, Object[] args) {
//        if (args.length != 1)
//            throw new IllegalArgumentException("Expected only one argument");
//
//        QName id = QName.parse(JSUtils.toString(args[0]), cx.getNamespaceContext());
//
//        final Object o = new TaskRef(id).get(cx);
//        if (o == null)
//            throw new XPMRhinoException("Cannot find task %s", id);
//        return o;
//    }

//    @Override
//    public refCall(LanguageContext cx, Object[] args) {
//        if (args.length != 1)
//            throw new IllegalArgumentException("Expected only one argument");
//
//        QName id = QName.parse(JSUtils.toString(args[0]), cx.getNamespaceContext());
//
//        return new TaskRef(id);
//    }

    @Expose(mode = ExposeMode.CALL, context = true)
    public TaskFactory call(LanguageContext cx, String qname) {
        QName id = cx.qname(qname);
        return ScriptContext.get().getRepository().getFactory(id);
    }

    @Expose(context = true)
    public void add(LanguageContext cx, String qname, @NoJavaization NativeObject taskDescription) {
        QName id = QName.parse(JSUtils.toString(qname), cx.getNamespaceContext());
        new TaskRef(id).set(cx, taskDescription);
    }

    @Expose(context = true)
    @Help(value = "Creates an anonymous task that will copy its input as output")
    public Copy copy(LanguageContext cx, String qname, Map plan){
        return new Copy(cx, qname, plan);
    }

    class TaskRef implements ScriptingReference<Object> {
        private static final long serialVersionUID = 1L;

        private final QName id;

        public TaskRef(QName id) {
            this.id = id;
        }

        @Override
        public TaskFactory get(LanguageContext cx) {
            final ScriptContext scriptContext = ScriptContext.get();
            return scriptContext.getRepository().getFactory(id);
        }

        @Override
        public void set(LanguageContext cx, Object value) {
            final TaskFactory factory;
            final ScriptContext scriptContext = ScriptContext.get();
            try {
                factory = new JavaScriptTaskFactory(id, ((JavaScriptContext)cx).scope(), (NativeObject) value, scriptContext.getRepository());
            } catch (RhinoException e) {
                throw e;
            } catch (ValueMismatchException | RuntimeException e) {
                throw new XPMRhinoException(e);
            }
            scriptContext.getRepository().addFactory(factory);
        }
    }
}
