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

    public Commands build(Path scriptPath, Task task) {
        final Command command = new Command();

        final Commands commands = new Commands(command);
        final Command.JsonParameterFile jsonParameter = new Command.JsonParameterFile("json", task.getInputsAsJson());

        arguments.forEach(a -> a.process(this, command, scriptPath, jsonParameter));
//        for (CommandArgument argument : arguments) {
//
//            if (argument instanceof CommandArgument.CommandString) {
//                command.add(((CommandArgument.CommandString) argument).string);
//            } else if (argument instanceof )
//        }
//
//        // Add command
//        command.add(interpreter);
//
//        // TaskReference class name
//        command.add(scriptPath);
//
//        // Parameter file
        command.add(jsonParameter);

        return commands;
    }
}
