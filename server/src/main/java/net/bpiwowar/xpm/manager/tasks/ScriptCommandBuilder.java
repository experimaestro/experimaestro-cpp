package net.bpiwowar.xpm.manager.tasks;

import net.bpiwowar.xpm.commands.JsonParameterFile;
import net.bpiwowar.xpm.manager.Task;
import net.bpiwowar.xpm.commands.Command;
import net.bpiwowar.xpm.commands.Commands;

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

    public Commands build(Path scriptPath, Task task) {
        final Commands commands = new Commands();
        final Command command = new Command();

        final JsonParameterFile jsonParameter = new JsonParameterFile("arg", task.getInputsAsJson());

        arguments.forEach(a -> a.process(this, command, scriptPath, jsonParameter));

        commands.add(command);
        return commands;
    }
}
