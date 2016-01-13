package net.bpiwowar.xpm.manager;

import net.bpiwowar.xpm.manager.json.JsonObject;
import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.manager.scripting.Property;
import net.bpiwowar.xpm.scheduler.Commands;

/**
 * Something that can be run similarly to factories
 */
@Exposed
public class RunnableTask {
    /** The task */
    @Property
    private final JsonObject json;

    /** The commands */
    @Property
    final Commands commands;

    public RunnableTask(Task json, Commands commands) {
        this.json = json.getInputsAsJson();
        this.commands = commands;
    }

}
