package net.bpiwowar.xpm.manager.tasks;

import net.bpiwowar.xpm.manager.Task;
import net.bpiwowar.xpm.manager.json.JsonObject;
import net.bpiwowar.xpm.scheduler.Commands;

import java.util.Map;

/**
 * Abstraction of the command
 */
public interface JavaCommandBuilder {
    Commands build(Commands commands, String taskClassname, Task task);

    void setEnvironment(JsonObject json, Map<String, String> environment);
}
