package sf.net.experimaestro.manager.java;

/*
 * This file is part of experimaestro.
 * Copyright (c) 2014 B. Piwowarski <benjamin@bpiwowar.net>
 *
 * experimaestro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * experimaestro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.apache.log4j.Level;
import sf.net.experimaestro.exceptions.ExperimaestroCannotOverwrite;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.manager.Manager;
import sf.net.experimaestro.manager.Task;
import sf.net.experimaestro.manager.TaskContext;
import sf.net.experimaestro.manager.Value;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.manager.json.JsonObject;
import sf.net.experimaestro.manager.json.JsonPath;
import sf.net.experimaestro.manager.json.JsonResource;
import sf.net.experimaestro.scheduler.*;
import sf.net.experimaestro.utils.io.LoggerPrintWriter;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Map;

/**
 * A task which is backed up main a Java class
 */
public class JavaTask extends Task {
//    final static private Logger LOGGER = Logger.getLogger();

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
        Path uniqueDir;
        Path path;
        try {
            final Path file = taskContext.workingDirectory;
            if (file == null)
                throw new XPMRuntimeException("Working directory is not set");

            String dirPrefix = factory.getId().getLocalPart();
            final String prefix = javaFactory.prefixes.get(factory.getId().getNamespaceURI());
            if (prefix != null) {
                dirPrefix = prefix + "." + dirPrefix;
            }

            uniqueDir = Manager.uniqueDirectory(file, dirPrefix, factory.getId(), json);
            path = uniqueDir.resolve(factory.getId().getLocalPart());
        } catch (Throwable e) {
            throw new XPMRuntimeException(e).addContext("while computing the unique directory");
        }

        // --- Check if this wasn't already done
        try (Transaction transaction = Transaction.create()) {
            final Resource old = Resource.getByLocator(transaction.em(), path);
            if (old != null && !old.canBeReplaced()) {
                taskContext.getLogger().warn("Cannot override resource [%s]", old);
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
                Job job = new Job(javaFactory.connector, uniqueDir);

                // --- Build the command
                Commands commands = javaFactory.commands(json, taskContext.simulate());

                CommandLineTask task = new CommandLineTask(commands);

                if (taskContext.simulate()) {
                    PrintWriter pw = new LoggerPrintWriter(taskContext.getLogger(), Level.INFO);
                    pw.format("[SIMULATE] Starting job: %s%n", task.toString());
                    pw.format("Command: %s%n", task.getCommands().toString());
                    pw.format("Path: %s", path);
                    pw.flush();
                } else {
                    if (old != null) {
                        // TODO: if equal, do not try to replace the task
                        try {
                            old.replaceBy(job);
                            job.setJobRunner(task);
                            taskContext.getLogger().info(String.format("Overwriting resource [%s]", task));
                        } catch (ExperimaestroCannotOverwrite e) {
                            taskContext.getLogger().warn("Cannot override resource [%s]", old);
                        }
                    } else {
                        transaction.em().persist(task);
                    }
                    transaction.commit();
                }
            }
        }

        taskContext.startedJob(task);

        // --- Fill some fields in returned json

        json.put(Manager.XP_TYPE.toString(), javaFactory.getOutput().toString());
        json.put(Manager.XP_RESOURCE.toString(), path.toString());

        for (PathArgument _path : javaFactory.pathArguments) {
            Path relativePath = uniqueDir.resolve(_path.relativePath);
            json.put(_path.jsonName, new JsonPath(relativePath));
        }

        return json;
    }
}
