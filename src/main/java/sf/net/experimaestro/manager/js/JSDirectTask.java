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

import org.apache.commons.vfs2.FileObject;
import org.mozilla.javascript.*;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.manager.*;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.manager.json.JsonObject;
import sf.net.experimaestro.manager.json.JsonString;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.log.Logger;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Map.Entry;

public class JSDirectTask extends JSAbstractTask {
    final static private Logger LOGGER = Logger.getLogger();

    /**
     * The run function
     */
    private Function runFunction;

    /**
     * The wrapper for the javascript object
     */
    private JSTask jsObject;

    public class JSTask extends JSBaseObject {
        /**
         * The object?
         */
        private NativeObject jsFactory;

        @JSFunction
        public JSTask(NativeObject jsFactory) {
            this.jsFactory = jsFactory;
        }

        @JSFunction(scope = true, optionalsAtStart = true, optional = 2)
        public JSFileObject unique_directory(Context cx, Scriptable scope, FileObject basedir, String prefix, Object json) throws IOException, NoSuchAlgorithmException {
            QName taskId = JSDirectTask.this.getFactory().getId();
            if (prefix == null) {
                prefix = taskId.getLocalPart();
            }
            return JSDirectTask.this.xpm.uniqueDirectory(scope, basedir, prefix, taskId, json);
        }
    }

    /**
     * The XPM object
     */
    private XPMObject xpm;

    public JSDirectTask() {
    }

    public JSDirectTask(XPMObject xpm, TaskFactory taskFactory, Scriptable jsScope,
                        NativeObject jsFactory, Function runFunction, Type outputType) {
        super(taskFactory, jsScope);
        this.xpm = xpm;
        this.jsObject = new JSTask(jsFactory);
        this.runFunction = runFunction;
    }

    @Override
    protected void init(Task _other) {
        JSDirectTask other = (JSDirectTask) _other;
        super.init(other);
        jsObject = other.jsObject;
        runFunction = other.runFunction;
        xpm = other.xpm;
    }

    @Override
    public Json jsrun(TaskContext taskContext) {
        LOGGER.debug("[Running] task: %s", factory.getId());

        final Context cx = Context.getCurrentContext();

        // Get the inputs
        JsonObject resultObject = new JsonObject();
        Type outputType = getFactory().getOutput();

        // Handles the type
        if (outputType != null) {
            // If the output is a generic object, modify the value
            resultObject.put(Manager.XP_TYPE.toString(), new JsonString(outputType.toString()));
        }

        // Copy the requested outputs
        for (Entry<String, Input> namedInput : getInputs().entrySet()) {
            final String copyTo = namedInput.getValue().getCopyTo();
            if (copyTo != null || runFunction == null) {
                String key = namedInput.getKey();
                Value value = values.get(key);
                resultObject.put(copyTo, value.get());
            }
        }


        if (runFunction != null) {
            // We have a run function
            Scriptable jsoninput = cx.newObject(jsScope, "Object", new Object[]{});
            for (Entry<String, Value> entry : values.entrySet()) {
                Json input = entry.getValue().get();
                // The JS object is set to the document element
                Object jsJson = input == null ? Scriptable.NOT_FOUND : new JSJson(input);
                jsoninput.put(entry.getKey(), jsoninput, jsJson);
            }

            xpm.setTaskContext(taskContext);
            final Object returned = runFunction.call(cx, jsScope, jsObject,
                    new Object[]{jsoninput, resultObject});
            xpm.setTaskContext(null);
            LOGGER.debug("Returned %s", returned);
            if (returned == Undefined.instance || returned == null)
                throw new ExperimaestroRuntimeException(
                        "Undefined returned by the function run of task [%s]",
                        factory.getId());
            LOGGER.debug("[/Running] task: %s", factory.getId());

            return JSUtils.toJSON(jsScope, returned);

        }


        // Simplify the output if needed
        if (outputType == null && values.size() == 1) {
            LOGGER.debug("[/Running] task: %s", factory.getId());
            return values.values().iterator().next().get();
        }

        LOGGER.debug("[/Running] task: %s", factory.getId());
        return resultObject;
    }
}
