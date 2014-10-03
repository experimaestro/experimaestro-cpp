package sf.net.experimaestro.manager.java;

import net.bpiwowar.experimaestro.tasks.Runner;
import org.apache.commons.vfs2.FileObject;
import org.apache.log4j.Level;
import sf.net.experimaestro.connectors.ComputationalRequirements;
import sf.net.experimaestro.connectors.SingleHostConnector;
import sf.net.experimaestro.exceptions.ExperimaestroCannotOverwrite;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.manager.Manager;
import sf.net.experimaestro.manager.Task;
import sf.net.experimaestro.manager.TaskContext;
import sf.net.experimaestro.manager.Value;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.manager.json.JsonObject;
import sf.net.experimaestro.scheduler.*;
import sf.net.experimaestro.utils.io.LoggerPrintWriter;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Map;

/**
 * Created by bpiwowar on 1/10/14.
 */
public class JavaTask extends Task {
    private final JavaTaskFactory javaFactory;

    public JavaTask(JavaTaskFactory factory) {
        super(factory);
        this.javaFactory = factory;
    }

    @Override
    public Json doRun(TaskContext taskContext) {
        // Copy the parameters
        JsonObject json = new JsonObject();
        for (Map.Entry<String, Value> entry : values.entrySet()) {
            json.put(entry.getKey(), entry.getValue().get());
        }

        // Computes the running directory
        ResourceLocator locator;
        try {
            final FileObject file = taskContext.workingDirectory;
            if (file == null)
                throw new XPMRuntimeException("Working directory is not set");
            final FileObject uniqueDir = Manager.uniqueDirectory(file, factory.getId().getLocalPart(), factory.getId(), json);
            final SingleHostConnector connector = javaFactory.connector.getConnector(new ComputationalRequirements() {
            });
            locator = new ResourceLocator(connector, connector.resolve(uniqueDir));
        } catch (Throwable e) {
            throw new XPMRuntimeException(e).addContext("while computing the unique directory");
        }

        final Command command = new Command();

        Command classpath = new Command();
        Arrays.asList(javaFactory.javaTasks.classpath).stream().forEach(f -> {
            classpath.add(new Command.Path(f));
            classpath.add(new Command.String(":"));
        });

        command.add("java", "-cp");
        command.add(classpath);
        command.add(Runner.class.getName());
        command.add(new Command.Path(taskContext.workingDirectory));
        final byte[] s = json.toString().getBytes(Manager.UTF8_CHARSET);
        final Command.ParameterFile jsonInput = new Command.ParameterFile("input", s);
        command.add(jsonInput);

        Commands commands = new Commands(command);

        final CommandLineTask commandLineTask = new CommandLineTask(taskContext.getScheduler(), locator, commands);

        commandLineTask.setState(ResourceState.WAITING);
        if (taskContext.simulate()) {
            PrintWriter pw = new LoggerPrintWriter(taskContext.getLogger(), Level.INFO);
            pw.format("[SIMULATE] Starting job: %s%n", commandLineTask.toString());
            pw.format("Command: %s%n", commandLineTask.getCommands().toString());
            pw.format("Locator: %s", locator.toString());
            pw.flush();
        } else {
            try {
                taskContext.getScheduler().store(commandLineTask, false);
            } catch (ExperimaestroCannotOverwrite e) {
                throw new XPMRuntimeException(e).addContext("while lauching command");
            }
        }


        // Fill
        json.put(Manager.XP_TYPE.toString(), javaFactory.getOutput().toString());

        return json;
    }
}
