package sf.net.experimaestro.manager.python;

import org.apache.commons.lang.NotImplementedException;
import sf.net.experimaestro.manager.Task;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.manager.scripting.ScriptContext;

/**
 * A python task
 */
public class PythonTask extends Task {
    public PythonTask(PythonTaskFactory factory) {
        super(factory);
    }

    @Override
    public Json doRun(ScriptContext taskContext) {
        throw new NotImplementedException();
    }
}
