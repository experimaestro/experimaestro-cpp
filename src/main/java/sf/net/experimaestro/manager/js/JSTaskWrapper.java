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

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import sf.net.experimaestro.exceptions.NoSuchParameter;
import sf.net.experimaestro.exceptions.ValueMismatchException;
import sf.net.experimaestro.manager.DotName;
import sf.net.experimaestro.manager.Task;
import sf.net.experimaestro.utils.JSUtils;

import java.util.List;

/**
 * A JS wrapper around a task
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 6/3/13
 */
@JSObjectDescription()
public class JSTaskWrapper extends JSBaseObject {
    private final Task task;


    @Override
    public String getClassName() {
        return "Task";
    }

    public JSTaskWrapper(Task task) {
        this.task = task;
    }


    @Override
    public void put(String name, Scriptable start, Object value) {
        super.put(name, start, value);
    }

    @JSFunction(value = "set", scope = true)
    public void set(Context cx, Scriptable scope, String id, Object value) throws NoSuchParameter {
        DotName qid = DotName.parse(id);
        if (JSUtils.isXML(value)) {
            task.setParameter(qid, JSUtils.toDocument(scope, value));
        } else {
            task.setParameter(qid, JSUtils.toString(value));
        }
    }

    @JSFunction("run")
    public Object run(boolean simulate) throws ValueMismatchException, NoSuchParameter {
        return new JSNode(task.run(simulate));
    }

    @JSFunction("run")
    public Object run() throws ValueMismatchException, NoSuchParameter {
        return new JSNode(task.run(false));
    }

    @JSFunction(value = "run_plan", scope = true)
    public Object runPlan(Context cx, Scriptable scope, String planString) throws Exception {
        return wrap(task.runPlan(planString, false, new JSScriptRunner(scope), false));
    }


    static NativeArray wrap(List<Document> result) {
        NativeArray array = new NativeArray(result.size());
        for (int index = 0; index < result.size(); index++)
            array.put(index, array, new JSNode(result.get(index)));
        return array;
    }
}
