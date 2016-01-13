package net.bpiwowar.xpm.manager.tasks;

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

import net.bpiwowar.xpm.exceptions.NoSuchParameter;
import net.bpiwowar.xpm.exceptions.ValueMismatchException;
import org.apache.log4j.Level;
import net.bpiwowar.xpm.exceptions.ExperimaestroCannotOverwrite;
import net.bpiwowar.xpm.exceptions.XPMRuntimeException;
import net.bpiwowar.xpm.manager.*;
import net.bpiwowar.xpm.manager.json.Json;
import net.bpiwowar.xpm.manager.json.JsonObject;
import net.bpiwowar.xpm.manager.json.JsonPath;
import net.bpiwowar.xpm.manager.scripting.RunningContext;
import net.bpiwowar.xpm.manager.scripting.ScriptContext;
import net.bpiwowar.xpm.scheduler.*;
import net.bpiwowar.xpm.utils.io.LoggerPrintWriter;
import net.bpiwowar.xpm.utils.log.Logger;
import org.mozilla.javascript.Script;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * A task which is backed up main a Java class
 */
public class ExternalTask extends Task {
//    final static private Logger LOGGER = Logger.getLogger();

    private final ExternalTaskFactory externalFactory;

    public ExternalTask(ExternalTaskFactory factory) {
        super(factory);
        this.externalFactory = factory;
    }


    @Override
    public Commands commands(IdentityHashMap<Object, Parameters> parameters) throws ValueMismatchException, NoSuchParameter {
        final ScriptContext sc = ScriptContext.get();
        processInputs(sc);
        JsonObject json = getInputsAsJson();
        return externalFactory.commands(json, false);
    }

    @Override
    public Json doRun(ScriptContext taskContext) {
        // Get the parameters
        JsonObject json = getInputsAsJson();

        // Computes the running directory
        Path uniqueDir;
        Path path;
        try {
            final Path file = taskContext.getWorkingDirectory();
            if (file == null) {
                throw new XPMRuntimeException("Working directory is not set");
            }

            String dirPrefix = factory.getId().getLocalPart();
            final String prefix = externalFactory.prefixes.get(factory.getId().getNamespaceURI());
            if (prefix != null) {
                dirPrefix = prefix + "." + dirPrefix;
            }

            uniqueDir = Manager.uniquePath(file, dirPrefix, factory.getId(), json, true);
            path = uniqueDir.resolve(factory.getId().getLocalPart());
        } catch (Throwable e) {
            throw new XPMRuntimeException(e).addContext("while computing the unique directory");
        }


        // --- Check if this wasn't already done
        final Logger taskLogger = taskContext.getLogger("JavaTask");

        try {
            final Resource old = Resource.getByLocator(path);
            if (old != null && !old.canBeReplaced()) {

                taskLogger.log(old.getState() == ResourceState.DONE ?
                        Level.DEBUG : Level.INFO, "Cannot overwrite task %s [%d]", old.getLocator(), old.getId());
            } else {
                // --- Build the command
                Commands commands = externalFactory.commands(json, RunningContext.get().simulate());

                // --- Build the command

                CommandLineTask job = new CommandLineTask(path);
                job.setCommands(commands);
                job.environment = new HashMap<>();
                externalFactory.setEnvironment(json, job.environment);

                commands.dependencies().forEach(job::addDependency);

                taskContext.prepare(job);
                if (RunningContext.get().simulate()) {
                    PrintWriter pw = new LoggerPrintWriter(taskLogger, Level.INFO);
                    pw.format("[SIMULATE] Starting job: %s%n", job.toString());
                    pw.format("Command: %s%n", job.getCommands().toString());
                    pw.format("Path: %s", path);
                    pw.flush();
                } else {
                    job.updateStatus();
                    if (old != null) {
                        // Lock and refresh the resource to be overwritten
                        try {
                            old.replaceBy(job);
                            taskLogger.info(String.format("Overwriting resource [%s]", job));
                        } catch (ExperimaestroCannotOverwrite e) {
                            taskLogger.warn("Cannot override resource [%s]", old);
                            throw new RuntimeException(e);
                        }
                    } else {
                        job.save();
                    }
                    taskLogger.info("Stored task %s [%s]", job.getLocator(), job.getId());
                }

                RunningContext.get().getSubmittedJobs().put(job.getLocator().toString(), job);
            }
        } catch (XPMRuntimeException e) {
            e.addContext("while storing task %s", path);
            throw e;
        } catch (RuntimeException e) {
            final XPMRuntimeException e2 = new XPMRuntimeException(e);
            e2.addContext("while storing task %s", path);
            throw e2;
        } catch (Throwable e) {
            final XPMRuntimeException e2 = new XPMRuntimeException(e);
            e2.addContext("while storing task %s", path);
            throw e2;
        }

        // --- Fill some fields in returned json

        json.put(Constants.XP_TYPE.toString(), externalFactory.getOutput().toString());
        json.put(Constants.XP_RESOURCE.toString(), path.toString());

        for (PathArgument __path : externalFactory.pathArguments) {
            Path relativePath = uniqueDir.resolve(__path.relativePath);
            json.put(__path.jsonName, new JsonPath(relativePath));
        }

        return json;
    }

}
