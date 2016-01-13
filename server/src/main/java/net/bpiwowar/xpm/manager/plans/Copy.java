package net.bpiwowar.xpm.manager.plans;

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

import net.bpiwowar.xpm.exceptions.XPMRhinoException;
import net.bpiwowar.xpm.manager.*;
import net.bpiwowar.xpm.manager.Value;
import net.bpiwowar.xpm.manager.json.Json;
import net.bpiwowar.xpm.manager.json.JsonObject;
import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.manager.scripting.LanguageContext;
import net.bpiwowar.xpm.manager.scripting.ScriptContext;
import net.bpiwowar.xpm.manager.scripting.Expose;

import java.util.HashMap;
import java.util.Map;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Exposed
public class Copy extends Plan {
    private final Type outputType;
    private final Map<String, Input> inputs = new HashMap<>();


    @Expose
    public Copy(LanguageContext cx, String outputType, Map plan){
        super(ScriptContext.get(), null);
        this.outputType = new Type(QName.parse(outputType, cx.getNamespaceContext()));
        this.setFactory(new AnonymousTaskFactory());
        PlanInputs mappings = Plan.getMappings(plan, cx);

        Type anyType = new Type(Constants.XP_ANY);

        for (DotName name : mappings.getMap().keySet()) {
            if (name.size() != 1)
                throw new XPMRhinoException("Names for products should be simple (got %s)", name);
            inputs.put(name.toString(), new JsonInput(anyType));
        }

        add(mappings);
    }


     private class AnonymousTaskFactory extends TaskFactory {
        public AnonymousTaskFactory() {
            super(new Repository(null), outputType.qname(), "1.0", "");
        }

        @Override
        public Map<String, Input> getInputs() {
            return inputs;
        }

        @Override
        public Type getOutput() {
            return outputType;
        }

        @Override
        public Task create() {
            AnonymousTask task = new AnonymousTask(this);
            task.init();
            return task;
        }

    }

    private class AnonymousTask extends Task {
        public AnonymousTask(AnonymousTaskFactory factory) {
            super(factory);
        }

        @Override
        public Json doRun(ScriptContext taskContext) {
            // We just copy the inputs as an output
            JsonObject json = new JsonObject();
            json.put(Constants.XP_TYPE.toString(), outputType.qname().toString());

            // Loop over non null inputs
            for (Map.Entry<String, Value> entry : values.entrySet()) {
                json.put(entry.getKey(), entry.getValue().get());
            }

            return json;
        }
    }
}
