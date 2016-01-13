package net.bpiwowar.xpm.manager.js;

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
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import net.bpiwowar.xpm.manager.Task;
import net.bpiwowar.xpm.manager.TaskFactory;
import net.bpiwowar.xpm.manager.json.Json;
import net.bpiwowar.xpm.manager.scripting.ScriptContext;
import net.bpiwowar.xpm.utils.JSUtils;
import net.bpiwowar.xpm.utils.log.Logger;

import static java.lang.String.format;

/**
 * TaskReference as implemented by a javascript object
 *
 * FIXME: is this really used?
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class TaskJavascript extends Task {
    final static private Logger LOGGER = Logger.getLogger();

    private final Scriptable jsScope;

    /**
     * The TaskReference object
     */
    private NativeObject jsObject;

    /**
     * The run Function
     */
    private Function runFunction;

    /**
     * Initialise a new task from a JavaScript object
     *
     * @param taskFactory The task factory
     * @param jsContext   The context for evaluation JavaScript code
     * @param jsScope     The scope for evaluating JavaScript code
     * @param jsObject    The JavaScript object
     */
    public TaskJavascript(TaskFactory taskFactory, Context jsContext,
                          Scriptable jsScope, NativeObject jsObject) {
        super();

        this.jsObject = jsObject;
        this.jsScope = jsScope;

        // Get the run function
        runFunction = JSUtils.get(jsScope, "run", jsObject, null);
        if (runFunction == null) {
            throw new RuntimeException(
                    format("Could not find the function run() in the object"));
        }

        // Set inputs
        Scriptable jsInputs = Context.getCurrentContext().newObject(jsScope,
                "Object", new Object[]{});
        jsObject.put("inputs", jsObject, jsInputs);
    }

    @Override
    public Json doRun(ScriptContext taskContext) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Run a task
     *
     * @param taskContext
     * @return
     */
//    public Json jsrun(ScriptContext taskContext) {
//        LOGGER.debug("[Running] task: %s", factory.getId());
//        Scriptable result = (Scriptable) runFunction.call(
//                Context.getCurrentContext(), jsScope, jsObject,
//                new Object[]{(Scriptable) jsObject.get("inputs", jsObject)});
//        LOGGER.debug("[/Running] task: %s", factory.getId());
//
//        return getDocument(jsScope, result);
//    }

    @Override
    protected void init(Task _other) {
        TaskJavascript other = (TaskJavascript) _other;
        super.init(other);
        jsObject = other.jsObject;
        runFunction = other.runFunction;
    }
}
