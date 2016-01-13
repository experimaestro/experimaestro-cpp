package net.bpiwowar.xpm.manager.tasks;

import net.bpiwowar.xpm.manager.Repository;
import net.bpiwowar.xpm.manager.json.JsonObject;
import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.scheduler.Commands;

import java.nio.file.Path;
import java.util.Map;

/**
 * Commands launched with a script
 */
@Exposed
public class ScriptTaskFactory extends ExternalTaskFactory {
    /** Command builder */
    ScriptCommandBuilder builder;

    Path scriptPath;

    protected ScriptTaskFactory() {
    }

    public ScriptTaskFactory(Repository repository, TaskInformation information, ScriptCommandBuilder builder, Path scriptPath) {
        super(repository, information);
        this.builder = builder;
        this.scriptPath = scriptPath;
    }

    @Override
    protected Commands build(JsonObject json) {
        return builder.build(scriptPath, json);
    }

    @Override
    public void setEnvironment(JsonObject json, Map<String, String> environment) {

    }
}
