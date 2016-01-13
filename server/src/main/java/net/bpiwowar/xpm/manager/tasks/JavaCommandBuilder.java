package net.bpiwowar.xpm.manager.tasks;

import net.bpiwowar.xpm.manager.json.JsonObject;
import net.bpiwowar.xpm.scheduler.Commands;

import java.util.Map;

/**
 * Abstraction of the command
 */
public interface JavaCommandBuilder {
    Commands build(String taskClassname, JsonObject json);

    void setEnvironment(JsonObject json, Map<String, String> environment);
}
