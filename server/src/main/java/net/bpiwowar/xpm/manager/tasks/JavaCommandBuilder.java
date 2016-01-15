package net.bpiwowar.xpm.manager.tasks;

import net.bpiwowar.xpm.manager.Task;
import net.bpiwowar.xpm.manager.json.JsonObject;
import net.bpiwowar.xpm.commands.Commands;

import java.util.Map;

/**
 * Abstraction of the command
 */
public interface JavaCommandBuilder {
    Commands build(String taskClassname, Task task);

    void setEnvironment(JsonObject json, Map<String, String> environment);
}
