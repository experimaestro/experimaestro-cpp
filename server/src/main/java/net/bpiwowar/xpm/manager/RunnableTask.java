package net.bpiwowar.xpm.manager;

import net.bpiwowar.xpm.manager.json.JsonObject;
import net.bpiwowar.xpm.manager.scripting.Expose;
import net.bpiwowar.xpm.manager.scripting.ExposeMode;
import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.manager.scripting.Property;
import net.bpiwowar.xpm.scheduler.Commands;

/**
 * Something that can be run similarly to factories
 */
@Exposed
public class RunnableTask {
    /** The task */
    private final JsonObject json;

    /** The commands */
    private final Commands commands;

    public RunnableTask(Task json, Commands commands) {
        this.json = json.getInputsAsJson();
        this.commands = commands;
    }

    @Expose(mode = ExposeMode.PROPERTY, value = "json")
    public JsonObject getJson() {
        return json;
    }

    @Expose(mode = ExposeMode.PROPERTY, value = "commands")
    public Commands getCommands() {
        return commands;
    }
}
