package net.bpiwowar.xpm.manager.scripting;

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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.bpiwowar.xpm.commands.AbstractCommand;
import net.bpiwowar.xpm.commands.Command;
import net.bpiwowar.xpm.commands.CommandContext;
import net.bpiwowar.xpm.commands.Commands;
import net.bpiwowar.xpm.commands.Redirect;
import net.bpiwowar.xpm.connectors.AbstractProcessBuilder;
import net.bpiwowar.xpm.connectors.Connector;
import net.bpiwowar.xpm.connectors.Launcher;
import net.bpiwowar.xpm.connectors.NetworkShare;
import net.bpiwowar.xpm.connectors.SingleHostConnector;
import net.bpiwowar.xpm.exceptions.ExperimaestroCannotOverwrite;
import net.bpiwowar.xpm.exceptions.XPMRuntimeException;
import net.bpiwowar.xpm.exceptions.XPMScriptRuntimeException;
import net.bpiwowar.xpm.manager.Constants;
import net.bpiwowar.xpm.manager.json.JsonObject;
import net.bpiwowar.xpm.manager.json.JsonResource;
import net.bpiwowar.xpm.scheduler.CommandLineTask;
import net.bpiwowar.xpm.scheduler.Dependency;
import net.bpiwowar.xpm.scheduler.DependencyParameters;
import net.bpiwowar.xpm.scheduler.LauncherParameters;
import net.bpiwowar.xpm.scheduler.Resource;
import net.bpiwowar.xpm.scheduler.ResourceState;
import net.bpiwowar.xpm.scheduler.Scheduler;
import net.bpiwowar.xpm.scheduler.TokenResource;
import net.bpiwowar.xpm.utils.Output;
import net.bpiwowar.xpm.utils.io.LoggerPrintWriter;
import net.bpiwowar.xpm.utils.log.Logger;
import org.apache.log4j.Level;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

/**
 * XPM Object in scripting languages
 */
@Exposed
public class XPM {
    public static final String COMMAND_LINE_JOB_HELP = "Schedule a command line job.<br>The options are <dl>" +
            "<dt>launcher</dt><dd></dd>" +
            "<dt>stdin</dt><dd></dd>" +
            "<dt>stdout</dt><dd></dd>" +
            "<dt>lock</dt><dd>An array of couples (resource, lock type). The lock depends on the resource" +
            "at hand, but are generally READ, WRITE, EXCLUSIVE.</dd>" +
            "";

    final static private Logger LOGGER = Logger.getLogger();

    static HashSet<String> COMMAND_LINE_OPTIONS = new HashSet<>(
            ImmutableSet.of("stdin", "stdout", "lock", "connector", "launcher"));

    @Expose()
    @Help("Retrieve (or creates) a token resource with a given xpath")
    static public TokenResource token_resource(
            @Argument(name = "path", help = "The path of the resource") String path
            ) throws ExperimaestroCannotOverwrite, SQLException, URISyntaxException {
        return token_resource(path, true);
    }

    @Expose()
    static public TokenResource token_resource(
            @Argument(name = "path", help = "The path of the resource") String path,
            @Argument(name = "post_process", help = "The path of the resource") boolean postProcess
    ) throws ExperimaestroCannotOverwrite, SQLException, URISyntaxException {
        final Resource resource = Resource.getByLocator(NetworkShare.uriToPath(path));
        final TokenResource tokenResource;
        if (resource == null) {
            tokenResource = new TokenResource(NetworkShare.uriToPath(path), 0);
            tokenResource.save();
        } else {
            if (!(resource instanceof TokenResource))
                throw new AssertionError(String.format("Resource %s exists and is not a token", path));
            tokenResource = (TokenResource) resource;
        }

        // Post-process (experiment handling)
        if (postProcess) {
            Context.get().postProcess(null, tokenResource);
        }

        return tokenResource;
    }

    /**
     * Get (but no create)
     */
    @Expose
    static public TokenResource token(@Argument(name = "path", help = "The path of the resource") String path
    ) throws ExperimaestroCannotOverwrite, SQLException, URISyntaxException {
        final Resource resource = Resource.getByLocator(NetworkShare.uriToPath(path));
        if (resource == null) {
            return null;
        }
        if (!(resource instanceof TokenResource))
            throw new AssertionError(String.format("Resource %s exists and is not a token", path));

        // Add to experiment
        return (TokenResource) resource;
    }

    protected static Context context() {

        return Context.get();
    }

    public static java.nio.file.Path getPath(Connector connector, Object stdout) throws IOException {

        if (stdout instanceof String)
            return connector.getMainConnector().resolveFile(stdout.toString());

        if (stdout instanceof java.nio.file.Path)
            return (java.nio.file.Path) stdout;

        throw new XPMRuntimeException("Unsupported stdout type [%s]", stdout.getClass());
    }

    @Expose("set_property")
    public void setProperty(@NotNull String name, Object object) {
        context().setProperty(name, object);
    }

    @Expose("set_default_lock")
    @Help("Adds a new resource to lock for all jobs to be started")
    public void setDefaultLock(
            @NotNull
            @Argument(name = "resource", help = "The resource to be locked")
                    Resource resource,
            @Argument(name = "parameters", help = "The parameters to be given at lock time")
                    DependencyParameters parameters) {

        context().addDefaultLock(resource, parameters);
    }

    @Expose("log_level")
    @Help(value = "Sets the logger debug level")
    public void setLogLevel(
            @Argument(name = "name") @NotNull String name,
            @Argument(name = "level") @NotNull String level
    ) {

        context().getLogger(name).setLevel(Level.toLevel(level));
    }

    @Expose(value = "command_line_job", optional = 1)
    @Help(value = COMMAND_LINE_JOB_HELP)
    public Resource commandlineJob(@Argument(name = "jobId") @NotNull Object path,
                                   @Argument(type = "Array", name = "command") @NotNull List<?> jsargs,
                                   @Argument(type = "Map", name = "options") Map<String, Object> options) throws Exception {
        return commandlineJob(path, Command.getCommand(jsargs), options);
    }

    @Expose(value = "command_line_job", optional = 1)
    @Help(value = COMMAND_LINE_JOB_HELP)
    public Resource commandlineJob(
            @Argument(name = "json") JsonObject json,
            @Argument(name = "jobId") Object jobId,
            @Argument(name = "commands") Object commands,
            @Argument(type = "Map", name = "options") Map<String, Object> jsOptions) throws Exception {

        Commands _commands;
        if (commands instanceof Commands) {
            _commands = (Commands) commands;
        } else if (commands instanceof AbstractCommand) {
            _commands = new Commands((AbstractCommand) commands);
        } else if (commands instanceof List) {
            _commands = new Commands(Command.getCommand((List) commands));
        } else {
            throw new XPMScriptRuntimeException("2nd argument of command_line_job must be a command");
        }

        Resource resource = commandlineJob(jobId, _commands, jsOptions);

        // Update the json
        json.put(Constants.XP_RESOURCE.toString(), new JsonResource(resource));
        return resource;
    }


    @Expose
    @Help("Set the simulate flag: When true, the jobs are not submitted but just output")
    public boolean simulate(boolean simulate) {
        final boolean old = Context.get().simulate();
        Context.get().simulate(simulate);
        return old;
    }

    @Expose
    public boolean simulate() {
        return Context.get().simulate();
    }

    @Expose
    public String evaluate(List<Object> command) throws Exception {
        return evaluate(command, ImmutableMap.of());
    }

    /**
     * Simple evaluation of shell commands (does not createSSHAgentIdentityRepository a job)
     *
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    @Expose
    public String evaluate(List<Object> jsargs, Map options) throws Exception {
        Context sc = context();
        Command command = Command.getCommand(jsargs);

        // Get the launcher
        final Launcher launcher;
        if (options.containsKey("launcher")) {
            launcher = (Launcher) options.get("launcher");
        } else {
            launcher = sc.getDefaultLauncher();
        }

        // Run the process and captures the output

        AbstractProcessBuilder builder = launcher.processBuilder(null);

        SingleHostConnector commandConnector = launcher.getMainConnector();
        try (CommandContext commandEnv = new CommandContext.Temporary(launcher)) {
            // Transform the list
            builder.command(Lists.newArrayList(Iterables.transform(command.components(), argument -> {
                try {
                    return argument.toString(commandEnv);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            })));

            if (options.containsKey("stdout")) {
                java.nio.file.Path stdout = getPath(commandConnector, options.get("stdout"));
                builder.redirectOutput(Redirect.to(stdout));
            } else {
                builder.redirectOutput(Redirect.PIPE);
            }

            return builder.execute(sc.getMainLogger());
        }
    }


    @Expose(value = "command_line_job", optional = 1)
    @Help(value = COMMAND_LINE_JOB_HELP)
    public Resource commandlineJob(@Argument(name = "jobId") Object path,
                                   AbstractCommand command,
                                   @Argument(type = "Map", name = "options") Map<String, Object> options) throws Exception {
        final Context context = context();
        final Logger rootLogger = context.getLogger("xpm");

        if (path == null) {
            throw new XPMScriptRuntimeException("Locator was null for command line job");
        }

        CommandLineTask job = null;
        // --- XPMProcess arguments: convert the javascript array into a Java array
        // of String
        LOGGER.debug("Adding command line job");

        // --- Create the task


        final Connector connector = (Connector) options.get("connector");

        // Resolve the path for the given connector
        if (!(path instanceof java.nio.file.Path)) {
            path = NetworkShare.uriToPath(path.toString());
        }

        job = new CommandLineTask((java.nio.file.Path) path);

        // Inherit output for main commands
        command.setOutputRedirect(Redirect.INHERIT);
        if (command instanceof Commands) {
            for (AbstractCommand subcommand : command) {
                subcommand.setOutputRedirect(Redirect.INHERIT);
            }
        }

        job.setCommand(command);
        if (context.getSubmittedJobs().containsKey(path)) {
            rootLogger.info("Not submitting %s [duplicate]", path);
            if (simulate()) {
                return job;
            }

            return Resource.getByLocator(connector.resolve((java.nio.file.Path) path));
        }


        // --- Environment
        ArrayList<Dependency> dependencies = new ArrayList<>();

        // --- Set defaults
        context.prepare(job);

        // --- Options
        if (options != null) {

            final ArrayList unmatched = new ArrayList(Sets.difference(options.keySet(), COMMAND_LINE_OPTIONS));
            if (!unmatched.isEmpty()) {
                throw new IllegalArgumentException(format("Some options are not allowed: %s",
                        Output.toString(", ", unmatched)));
            }


            // --- XPMProcess launcher
            if (options.containsKey("launcher")) {
                final Object launcher = options.get("launcher");
                if (launcher != null)
                    job.setLauncher((Launcher) launcher, (LauncherParameters) context.getParameter(launcher));

            }

            // --- Redirect standard output
            if (options.containsKey("stdin")) {
                final Object stdin = options.get("stdin");
                if (stdin instanceof String) {
                    job.setInput((String) stdin);
                } else if (stdin instanceof java.nio.file.Path) {
                    job.setInput((java.nio.file.Path) stdin);
                } else
                    throw new XPMRuntimeException("Unsupported stdin type [%s]", stdin.getClass());
            }

            // --- Redirect standard output
            if (options.containsKey("stdout")) {
                java.nio.file.Path fileObject = XPM.getPath(connector, options.get("stdout"));
                job.setOutput(fileObject);
            }

            // --- Redirect standard error
            if (options.containsKey("stderr")) {
                java.nio.file.Path fileObject = XPM.getPath(connector, options.get("stderr"));
                job.setError(fileObject);
            }


            // --- Resources to lock
            if (options.containsKey("lock")) {
                List locks = (List) options.get("lock");
                for (int i = locks.size(); --i >= 0; ) {
                    Object lock_i = locks.get(i);
                    Dependency dependency = null;

                    if (lock_i instanceof Dependency) {
                        dependency = (Dependency) lock_i;
                    } else if (lock_i instanceof List) {
                        List array = (List) lock_i;
                        if (array.size() != 2) {
                            throw new XPMScriptRuntimeException(new IllegalArgumentException("Wrong number of arguments for lock"));
                        }

                        final Object depObject = array.get(0);
                        Resource resource = null;
                        if (depObject instanceof Resource) {
                            resource = (Resource) depObject;
                        } else {
                            final String rsrcPath = depObject.toString();
                            resource = Resource.getByLocator(rsrcPath);
                            if (resource == null)
                                if (simulate()) {
                                    if (!context.getSubmittedJobs().containsKey(rsrcPath))
                                        LOGGER.error("The dependency [%s] cannot be found", rsrcPath);
                                } else {
                                    throw new XPMRuntimeException("Resource [%s] was not found", rsrcPath);
                                }
                        }

                        final DependencyParameters lockType = (DependencyParameters) array.get(1);
                        LOGGER.debug("Adding dependency on [%s] of type [%s]", resource, lockType);

                        if (!simulate()) {
                            dependency = resource.createDependency(lockType);
                        }
                    } else {
                        throw new XPMRuntimeException("Element %d for option 'lock' is not a dependency but %s",
                                i, lock_i.getClass());
                    }

                    if (!simulate()) {
                        dependencies.add(dependency);
                    }
                }

            }


        }


        job.setState(ResourceState.WAITING);
        if (simulate()) {
            PrintWriter pw = new LoggerPrintWriter(rootLogger, Level.INFO);
            pw.format("[SIMULATE] Starting job: %s%n", job.toString());
            pw.format("Command: %s%n", job.getCommand().toString());
            pw.format("Locator: %s", path.toString());
            pw.flush();
        } else {

            // Add dependencies
            dependencies.forEach(job::addDependency);

            final Resource old = Resource.getByLocator(job.getLocator());

            job.updateStatus();

            // Replace old if necessary
            if (old != null) {
                if (!old.canBeReplaced()) {
                    rootLogger.log(old.getState() == ResourceState.DONE ? Level.DEBUG : Level.INFO,
                            "Cannot overwrite task %s [%d]", old.getLocator(), old.getId());
                    context.postProcess(null, old);
                    return old;
                } else {
                    rootLogger.info("Replacing resource %s [%d]", old.getLocator(), old.getId());
                    old.replaceBy(job);
                }
            } else {
                // Store in scheduler
                job.save();
            }

        }

        context.postProcess(null, job);

        return job;
    }


    /**
     * Get experimaestro namespace
     */
    @Expose
    public String ns() {
        return Constants.EXPERIMAESTRO_NS;
    }


    public Scheduler getScheduler() {
        return context().getScheduler();
    }


}
