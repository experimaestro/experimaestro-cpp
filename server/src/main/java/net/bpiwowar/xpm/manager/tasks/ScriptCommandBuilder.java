package net.bpiwowar.xpm.manager.tasks;

import net.bpiwowar.xpm.manager.Task;
import net.bpiwowar.xpm.manager.json.JsonObject;
import net.bpiwowar.xpm.scheduler.Command;
import net.bpiwowar.xpm.scheduler.Commands;

import java.nio.file.Path;
import java.util.List;

/**
 * Script
 */
public class ScriptCommandBuilder {
    /** List of commands */
    private final List<CommandArgument> arguments;

    public ScriptCommandBuilder(ScriptsTaskInformation informations) {
        this.arguments = informations.command;
    }

    public Commands build(Commands commands, Path scriptPath, Task task) {
        final Command command = new Command();

        final Command.JsonParameterFile jsonParameter = new Command.JsonParameterFile("arg", task.getInputsAsJson());

        arguments.forEach(a -> a.process(this, command, scriptPath, jsonParameter));

        command.add(jsonParameter);

        commands.add(command);
        return commands;
    }
}
