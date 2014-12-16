package sf.net.experimaestro.manager.java;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
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
import sf.net.experimaestro.manager.json.JsonFileObject;
import sf.net.experimaestro.manager.json.JsonObject;
import sf.net.experimaestro.manager.json.JsonResource;
import sf.net.experimaestro.scheduler.*;
import sf.net.experimaestro.utils.io.LoggerPrintWriter;
import sf.net.experimaestro.utils.log.Logger;

import java.io.PrintWriter;
import java.util.Map;

/**
 * Created by bpiwowar on 1/10/14.
 */
public class JavaTask extends Task {
    final static private Logger LOGGER = Logger.getLogger();

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

            String dirPrefix = factory.getId().getLocalPart();
            final String prefix = javaFactory.prefixes.get(factory.getId().getNamespaceURI());
            if (prefix != null) {
                dirPrefix = prefix + "." + dirPrefix;
            }

            uniqueDir = Manager.uniqueDirectory(file, dirPrefix, factory.getId(), json);
            final SingleHostConnector connector = javaFactory.connector.getConnector(new ComputationalRequirements() {
            });
            locator = new ResourceLocator(connector, connector.resolve(uniqueDir.resolveFile("task")));
        } catch (Throwable e) {
            throw new XPMRuntimeException(e).addContext("while computing the unique directory");
        }

        // --- Check if this wasn't already done
        final Resource old = taskContext.getScheduler().getResource(locator);
        CommandLineTask task;
        if (old == null || old.canBeReplaced()) {
            // --- Build the command
            Commands commands = javaFactory.commands(taskContext.getScheduler(), json, taskContext.simulate());

            task = new CommandLineTask(taskContext.getScheduler(), locator, commands);

            task.setState(ResourceState.WAITING);
            if (taskContext.simulate()) {
                PrintWriter pw = new LoggerPrintWriter(taskContext.getLogger("JavaTask"), Level.INFO);
                pw.format("[SIMULATE] Starting job: %s%n", task.toString());
                pw.format("Command: %s%n", task.getCommands().toString());
                pw.format("Locator: %s", locator.toString());
                pw.flush();
            } else {
                try {
                    if (old != null) {
                        // TODO: if equal, do not try to replace the task
                        if (task.replace(old)) {
                            taskContext.getLogger().info(String.format("Overwriting resource [%s]", task.getIdentifier()));
                        } else {
                            taskContext.getLogger().warn("Cannot override resource [%s]", task.getIdentifier());
                            old.init(taskContext.getScheduler());
                        }
                    } else {
                        taskContext.getScheduler().store(task, false);
                    }
                } catch (ExperimaestroCannotOverwrite e) {
                    throw new XPMRuntimeException(e).addContext("while lauching command");
                }
            }
        } else {
            task = (CommandLineTask) old;
        }

        taskContext.startedJob(task);

        // --- Fill some fields in returned json

        json.put(Manager.XP_TYPE.toString(), javaFactory.getOutput().toString());
        json.put(Manager.XP_RESOURCE.toString(), new JsonResource(task));

        for (PathArgument path : javaFactory.pathArguments) {
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
