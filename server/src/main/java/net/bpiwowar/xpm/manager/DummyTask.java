package net.bpiwowar.xpm.manager;


import com.google.common.collect.ImmutableMap;
import net.bpiwowar.xpm.manager.json.Json;
import net.bpiwowar.xpm.manager.scripting.ScriptContext;
import net.bpiwowar.xpm.manager.tasks.JsonType;

import java.util.Map;

/**
 *
 */
public class DummyTask extends Task {
    private static final TaskFactory FACTORY = new TaskFactory() {
        {
            this.id = Constants.EXPERIMAESTRO_NS_OBJECT.typename("dummy");
        }

        @Override
        public Map<String, Input> getInputs() {
            return ImmutableMap.of();
        }

        @Override
        public Type getOutput() {
            return new ValueType(Constants.XP_ANY);
        }

        @Override
        public Task create() {
            throw new UnsupportedOperationException("Cannot create a dummy task");
        }

        @Override
        public TypeName getId() {
            return new TypeName(Constants.EXPERIMAESTRO_NS, "task");
        }
    };

    final static public DummyTask INSTANCE = new DummyTask();

    public DummyTask() {
        super(FACTORY);
    }

    @Override
    public Json doRun(ScriptContext taskContext) {
        throw new UnsupportedOperationException("Cannot run a dummy task");
    }
}
