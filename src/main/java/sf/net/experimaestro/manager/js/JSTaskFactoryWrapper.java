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
import org.mozilla.javascript.Scriptable;
import sf.net.experimaestro.manager.Task;
import sf.net.experimaestro.manager.TaskFactory;
import sf.net.experimaestro.utils.log.Logger;

import java.util.List;

/**
 * Task factory as seen by JavaScript
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@JSObjectDescription(name = JSTaskFactoryWrapper.CLASSNAME)
public class JSTaskFactoryWrapper extends JSBaseObject {
    final static private Logger LOGGER = Logger.getLogger();


    public static final String CLASSNAME = "XPMTaskFactory";

    TaskFactory factory;

    public JSTaskFactoryWrapper(Scriptable information) {
        if (information != null) {
            this.factory = ((JSTaskFactory) information).factory;
        }
    }

    // ---- JavaScript functions ----


    @JSFunction("run")
    public List<Object> run(String plan) throws Exception {
        Task task = factory.create();
        return JSTaskWrapper.wrap(task.runPlan(plan, true, new JSScriptRunner(this), false));
    }

    @JSFunction("create")
    public Scriptable create() {
        Task task = factory.create();
        return Context.getCurrentContext().newObject(getParentScope(), "XPMTask",
                new Object[]{task});
    }
}