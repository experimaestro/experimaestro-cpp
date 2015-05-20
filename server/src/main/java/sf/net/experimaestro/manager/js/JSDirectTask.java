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
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.manager.*;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.manager.json.JsonObject;
import sf.net.experimaestro.manager.json.JsonString;
import sf.net.experimaestro.manager.scripting.Expose;
import sf.net.experimaestro.manager.scripting.Help;
import sf.net.experimaestro.manager.scripting.LanguageContext;
import sf.net.experimaestro.manager.scripting.ScriptContext;
import sf.net.experimaestro.scheduler.Resource;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.log.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Map.Entry;

public class JSDirectTask extends Task {
    final static private Logger LOGGER = Logger.getLogger();

    private final Scriptable jsScope;

    /**
     * The run function
     */
    private Function runFunction;

    /**
     * The wrapper for the javascript object
     */
    private JSTask jsObject;

    @Override
    public Json doRun(ScriptContext taskContext) {
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

            final Object returned = runFunction.call(cx, jsScope, jsObject,
                    new Object[]{jsoninput, resultObject});
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

    public JSDirectTask(TaskFactory taskFactory, Scriptable jsScope,
                        NativeObject jsFactory, Function runFunction, Type outputType) {
        super();
        this.jsScope = jsScope;
        this.jsObject = new JSTask(jsFactory);
        this.runFunction = runFunction;
    }

    @Override
    protected void init(Task _other) {
        JSDirectTask other = (JSDirectTask) _other;
        super.init(other);
        jsObject = other.jsObject;
        runFunction = other.runFunction;
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

        @Expose(context = true, optionalsAtStart = true, optional = 2)
        public Path unique_directory(LanguageContext cx, Path basedir, String prefix, Object json) throws IOException, NoSuchAlgorithmException {
            QName taskId = JSDirectTask.this.getFactory().getId();
            if (prefix == null) {
                prefix = taskId.getLocalPart();
            }
            return cx.uniqueDirectory(basedir, prefix, taskId, json, true);
        }

        @Expose(context = true)
        public Path unique_file(LanguageContext cx, Resource resource, String prefix, Object json) throws IOException, NoSuchAlgorithmException {
            QName taskId = JSDirectTask.this.getFactory().getId();
            if (prefix == null) {
                prefix = taskId.getLocalPart();
            }
            return cx.uniqueDirectory(basedir, prefix, taskId, json, false);
        }

        @Expose(context = true)
        public Path unique_directory(LanguageContext cx, Resource resource, String prefix, Object json) throws IOException, NoSuchAlgorithmException {
            QName taskId = JSDirectTask.this.getFactory().getId();
            if (prefix == null) {
                prefix = taskId.getLocalPart();
            }
            Path basedir = resource.getPath().getParent();
            return JSDirectTask.this.xpm.uniquePath(scope, basedir, prefix, taskId, json, true);
        }



        @Expose()
        @Help("Returns a Json object corresponding to inputs of a given group (shallow copy)")
        public Json group(String groupId, JsonObject p) {
            JsonObject json = new JsonObject();
            for (Entry<String, Input> x : getFactory().getInputs().entrySet()) {
                if (x.getValue().inGroup(groupId)) {
                    json.put(x.getKey(), p.get(x.getKey()));
                }
            }

            return json;
        }
    }
}
