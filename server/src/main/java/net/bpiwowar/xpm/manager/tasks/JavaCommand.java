package net.bpiwowar.xpm.manager.tasks;

import net.bpiwowar.xpm.commands.CommandPath;
import net.bpiwowar.xpm.commands.CommandString;
import net.bpiwowar.xpm.commands.JsonParameterFile;
import net.bpiwowar.xpm.commands.WorkingDirectory;
import net.bpiwowar.xpm.manager.Task;
import net.bpiwowar.xpm.manager.json.Json;
import net.bpiwowar.xpm.manager.json.JsonObject;
import net.bpiwowar.xpm.manager.json.JsonString;
import net.bpiwowar.xpm.commands.Command;
import net.bpiwowar.xpm.commands.Commands;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

/**
 *
 */
public class JavaCommand implements JavaCommandBuilder {
    private Path[] classpath;

    public JavaCommand(Path[] classpath) {
        this.classpath = classpath;
    }

    @Override
    public Commands build(String taskClassname, Task task) {
        final Commands commands = new Commands();

        final Command command = new Command();
        commands.add(command);

        Command classpath = new Command();

        Arrays.asList(this.classpath).stream().forEach(f -> {
            classpath.add(new CommandPath(f));
            classpath.add(new CommandString(":"));
        });

        command.add("java", "-cp");
        command.add(classpath);

        // Sets JVM options
        final Json jvm = task.getValue(JavaTaskFactory.JVM_OPTIONS).get();
        if (jvm != null && jvm instanceof JsonObject) {
            final Json memory = ((JsonObject) jvm).get("memory");
            if (memory instanceof JsonString) {
                final Object s = memory.get();
                command.add("-Xmx" + s);
            }
        }

        // TaskReference class name
        command.add(taskClassname);

        // Runner class name
        command.add(Runner.class.getName());

        // Working directory
        command.add(WorkingDirectory.INSTANCE);

        // Parameter file
        command.add(new JsonParameterFile("arg", task.getInputsAsJson()));

        return commands;
    }

    @Override
    public void setEnvironment(JsonObject json, Map<String, String> environment) {
        // do nothing
    }
}
