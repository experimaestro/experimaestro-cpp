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

import org.mozilla.javascript.*;
import sf.net.experimaestro.manager.scripting.ScriptingPath;
import sf.net.experimaestro.manager.scripting.ScriptContext;
import sf.net.experimaestro.manager.scripting.Help;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.manager.*;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.manager.json.JsonObject;
import sf.net.experimaestro.manager.json.JsonString;
import sf.net.experimaestro.manager.scripting.Expose;
import sf.net.experimaestro.scheduler.Resource;
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
    public Json jsrun(ScriptContext taskContext) {
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
                resultObject.put(copyTo == null ? key : copyTo, value.get());
            }
        }


        if (runFunction != null) {
            // We have a run function
            JsonObject jsoninput = new JsonObject();
            for (Entry<String, Value> entry : values.entrySet()) {
                Json input = entry.getValue().get();
                jsoninput.put(entry.getKey(), input);
            }

            xpm.setTaskContext(taskContext);
            final Object returned = runFunction.call(cx, jsScope, jsObject,
                    new Object[]{new JSJson(jsoninput).setXPM(xpm), resultObject});
            xpm.setTaskContext(null);
            LOGGER.debug("Returned %s", returned);
            if (returned == Undefined.instance || returned == null)
                throw new XPMRuntimeException(
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

    public class JSTask extends JSBaseObject {
        /**
         * The object?
         */
        private NativeObject jsFactory;

        @Expose
        public JSTask(NativeObject jsFactory) {
            this.jsFactory = jsFactory;
        }

        @Expose(scope = true, optionalsAtStart = true, optional = 2)
        public ScriptingPath unique_directory(Context cx, Scriptable scope, java.nio.file.Path basedir, String prefix, Object json) throws IOException, NoSuchAlgorithmException {
            QName taskId = JSDirectTask.this.getFactory().getId();
            if (prefix == null) {
                prefix = taskId.getLocalPart();
            }
            return JSDirectTask.this.xpm.uniqueDirectory(scope, basedir, prefix, taskId, json);
        }

        @Expose(scope = true)
        public ScriptingPath unique_directory(Context cx, Scriptable scope, Resource resource, String prefix, Object json) throws IOException, NoSuchAlgorithmException {
            QName taskId = JSDirectTask.this.getFactory().getId();
            if (prefix == null) {
                prefix = taskId.getLocalPart();
            }
            java.nio.file.Path basedir = resource.getPath().getParent();
            return JSDirectTask.this.xpm.uniqueDirectory(scope, basedir, prefix, taskId, json);
        }


        @Expose()
        @Help("Returns a Json object corresponding to inputs of a given group (shallow copy)")
        public JSJson group(String groupId, JsonObject p) {
            JsonObject json = new JsonObject();
            for (Entry<String, Input> x : getFactory().getInputs().entrySet()) {
                if (x.getValue().inGroup(groupId)) {
                    json.put(x.getKey(), p.get(x.getKey()));
                }
            }

            return (JSJson) new JSJson(json).setXPM(xpm);
        }
    }
}
