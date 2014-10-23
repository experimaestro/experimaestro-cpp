package sf.net.experimaestro.manager.js;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import sf.net.experimaestro.exceptions.XPMRhinoException;
import sf.net.experimaestro.manager.*;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.manager.json.JsonObject;
import sf.net.experimaestro.manager.plans.Plan;
import sf.net.experimaestro.manager.plans.PlanInputs;
import sf.net.experimaestro.utils.JSNamespaceContext;

import javax.xml.xpath.XPathExpressionException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 26/4/13
 */
public class JSCopy extends JSPlan {
    private final Type outputType;
    private final Map<String, Input> inputs = new HashMap<>();


    @JSFunction
    public JSCopy(Context cx, Scriptable scope, String outputType, NativeObject plan) throws XPathExpressionException {
        this.outputType = new Type(QName.parse(outputType, new JSNamespaceContext(scope)));
        this.plan = new Plan(new AnonymousTaskFactory());
        PlanInputs mappings = JSPlan.getMappings(plan, scope);

        Type anyType = new Type(Manager.XP_ANY);

        for (DotName name : mappings.getMap().keySet()) {
            if (name.size() != 1)
                throw new XPMRhinoException("Names for products should be simple (got %s)", name);
            inputs.put(name.toString(), new JsonInput(anyType));
        }

        this.plan.add(mappings);

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
        public Json doRun(TaskContext taskContext) {
            // We just copy the inputs as an output
            JsonObject json = new JsonObject();
            json.put(Manager.XP_TYPE.toString(), outputType.qname().toString());

            // Loop over non null inputs
            for (Map.Entry<String, Value> entry : values.entrySet()) {
                json.put(entry.getKey(), entry.getValue().get());
            }

            return json;
        }
    }
}
