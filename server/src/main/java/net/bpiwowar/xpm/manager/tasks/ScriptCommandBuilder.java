package net.bpiwowar.xpm.manager.tasks;

import net.bpiwowar.xpm.manager.json.JsonObject;
import net.bpiwowar.xpm.scheduler.Command;
import net.bpiwowar.xpm.scheduler.Commands;

import java.nio.file.Path;

/**
 * Script
 */
public class ScriptCommandBuilder {
    Path interpreter;

    public ScriptCommandBuilder(Path interpreter) {
        this.interpreter = interpreter;
    }

    public Commands build(Path scriptPath, JsonObject json) {
        final Command command = new Command();

        final Commands commands = new Commands(command);

        // Add command
        command.add(interpreter);

        // TaskReference class name
        command.add(scriptPath);

        // Parameter file
        command.add(new Command.JsonParameterFile("json", json));

        return commands;
    }
}
