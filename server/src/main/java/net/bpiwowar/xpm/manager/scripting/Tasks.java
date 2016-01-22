package net.bpiwowar.xpm.manager.scripting;

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

import net.bpiwowar.xpm.exceptions.ValueMismatchException;
import net.bpiwowar.xpm.exceptions.XPMRhinoException;
import net.bpiwowar.xpm.manager.TypeName;
import net.bpiwowar.xpm.manager.TaskFactory;
import net.bpiwowar.xpm.manager.js.JavaScriptContext;
import net.bpiwowar.xpm.manager.js.JavaScriptTaskFactory;
import net.bpiwowar.xpm.utils.JSUtils;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.RhinoException;

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

    @Expose(value = "set", context = true)
    public void set(LanguageContext cx, String qname, NativeObject definition) {
        TypeName id = TypeName.parse(qname, cx.getNamespaceContext());
        final TaskRef taskRef = new TaskRef(id);
        taskRef.set(cx, definition);
    }

    @Expose(context = true, mode = ExposeMode.CALL)
    public Object get(LanguageContext cx, String qname) {
        TypeName id = cx.qname(qname);
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
        TypeName id = cx.qname(qname);
        return ScriptContext.get().getRepository().getFactory(id);
    }

    @Expose(context = true)
    public void add(LanguageContext cx, String qname, @NoJavaization NativeObject taskDescription) {
        TypeName id = TypeName.parse(JSUtils.toString(qname), cx.getNamespaceContext());
        new TaskRef(id).set(cx, taskDescription);
    }

    class TaskRef implements ScriptingReference<Object> {
        private final TypeName id;

        public TaskRef(TypeName id) {
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
