/*
 * This file is part of experimaestro.
 * Copyright (c) 2012 B. Piwowarski <benjamin@bpiwowar.net>
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

import org.mozilla.javascript.*;
import org.mozilla.javascript.annotations.JSFunction;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.manager.Task;
import sf.net.experimaestro.manager.TaskFactory;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.log.Logger;

import java.util.List;

/**
 * Task factory as seen by JavaScript
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class TaskFactoryJSWrapper extends ScriptableObject {
    final static private Logger LOGGER = Logger.getLogger();

    private static final long serialVersionUID = 1L;

    public static final String CLASSNAME = "XPMTaskFactory";

    TaskFactory factory;

    public TaskFactoryJSWrapper() {
    }

    public void jsConstructor(Scriptable information) {
        if (information != null) {
            this.factory = ((JSTaskFactory) information).factory;
        }
    }

    @Override
    public String getClassName() {
        return "XPMTaskFactory";
    }

    // ---- JavaScript functions ----


    @JSFunction("run")
    static public List<Object> run(Context cx, Scriptable thisObj, Object[] args, Function funObj) throws Exception {
        if (args.length != 1) throw new ExperimaestroRuntimeException("Expected 1 argument for run");
        Task task = ((TaskFactoryJSWrapper)thisObj).factory.create();
        return TaskJSWrapper.wrap(task.runPlan(JSUtils.toString(args[0]), true, new JSScriptRunner(thisObj)), thisObj);
    }

    @JSFunction("create")
    public Scriptable create() {
        Task task = factory.create();
        return Context.getCurrentContext().newObject(getParentScope(), "XPMTask",
                new Object[] { task });
    }
}