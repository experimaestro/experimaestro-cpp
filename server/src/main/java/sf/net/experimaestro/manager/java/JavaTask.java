package sf.net.experimaestro.manager.java;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.log4j.Level;
import sf.net.experimaestro.connectors.ComputationalRequirements;
import sf.net.experimaestro.connectors.SingleHostConnector;
import sf.net.experimaestro.exceptions.ExperimaestroCannotOverwrite;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.manager.*;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.manager.json.JsonFileObject;
import sf.net.experimaestro.manager.json.JsonObject;
import sf.net.experimaestro.scheduler.*;
import sf.net.experimaestro.utils.io.LoggerPrintWriter;

import java.io.PrintWriter;
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
            if (entry.getValue().isSet())
                json.put(entry.getKey(), entry.getValue().get());
        }

        // Computes the running directory
        ResourceLocator locator;
        FileObject uniqueDir;
        try {
            final FileObject file = taskContext.workingDirectory;
            if (file == null)
                throw new XPMRuntimeException("Working directory is not set");
            uniqueDir = Manager.uniqueDirectory(file, factory.getId().getLocalPart(), factory.getId(), json);
            final SingleHostConnector connector = javaFactory.connector.getConnector(new ComputationalRequirements() {
            });
            locator = new ResourceLocator(connector, connector.resolve(uniqueDir.resolveFile("task")));
        } catch (Throwable e) {
            throw new XPMRuntimeException(e).addContext("while computing the unique directory");
        }

        // --- Build the command
        CommandPart commandPart = javaFactory.command(taskContext.getScheduler(), json);
        Commands commands = new Commands(commandPart.command());

        final CommandLineTask commandLineTask = new CommandLineTask(taskContext.getScheduler(), locator, commands);
        commandPart.dependencies().forEach(d -> commandLineTask.addDependency(d));

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

        // --- Fill some fields in returned json

        json.put(Manager.XP_TYPE.toString(), javaFactory.getOutput().toString());
        json.put(Manager.XP_RESOURCE.toString(), commandLineTask.getIdentifier());

        for(PathArgument path: javaFactory.pathArguments) {
            try {
                FileObject relativePath = uniqueDir.resolveFile(path.relativePath);
                json.put(path.jsonName, new JsonFileObject(relativePath));
            } catch (FileSystemException e) {
                throw new XPMRuntimeException(e, "Could not resolve file path [%s]", path.relativePath);
            }
        }

        return json;
    }
}
