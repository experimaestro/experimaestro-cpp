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

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import sf.net.experimaestro.exceptions.NoSuchParameter;
import sf.net.experimaestro.exceptions.ValueMismatchException;
import sf.net.experimaestro.manager.DotName;
import sf.net.experimaestro.manager.Task;
import sf.net.experimaestro.manager.ValueType;
import sf.net.experimaestro.manager.scripting.Expose;
import sf.net.experimaestro.utils.JSUtils;

/**
 * A JavaScript wrapper around a task
 *
 * @author B. Piwowarski
 */
@JSObjectDescription(name = "TaskWrapper")
public class JSTaskWrapper extends JSBaseObject {
    private final Task task;
    private final XPMObject xpm;

    @Expose
    public JSTaskWrapper(Task task, XPMObject xpm) {
        this.task = task;
        this.xpm = xpm;
    }

    @Override
    public String getClassName() {
        return "Task";
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        super.put(name, start, value);
    }

    @Expose(value = "set", scope = true)
    public void set(Context cx, Scriptable scope, String id, Object value) throws NoSuchParameter {
        DotName qid = DotName.parse(id);
        task.setParameter(qid, ValueType.wrap(JSUtils.unwrap(value)));
    }

    @Expose("run")
    public Object run(boolean simulate) throws ValueMismatchException, NoSuchParameter {
        return new JSJson(task.run(xpm.newScriptContext().simulate(simulate)));
    }

    @Expose("run")
    public Object run() throws ValueMismatchException, NoSuchParameter {
        return new JSJson(task.run(xpm.newScriptContext().simulate(false)));
    }
}
