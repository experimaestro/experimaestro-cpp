package sf.net.experimaestro.manager.java;

import sf.net.experimaestro.manager.json.JsonObject;
import sf.net.experimaestro.scheduler.Commands;

import java.util.Map;

/**
 * Abstraction of the command
 */
public interface JavaCommandBuilder {
    Commands build(String taskClassname, JsonObject json);

    void setEnvironment(JsonObject json, Map<String, String> environment);
}
