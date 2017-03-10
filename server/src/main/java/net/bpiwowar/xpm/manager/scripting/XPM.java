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
import net.bpiwowar.xpm.commands.Command;
import net.bpiwowar.xpm.commands.CommandContext;
import net.bpiwowar.xpm.commands.Redirect;
import net.bpiwowar.xpm.connectors.AbstractProcessBuilder;
import net.bpiwowar.xpm.connectors.Connector;
import net.bpiwowar.xpm.connectors.Launcher;
import net.bpiwowar.xpm.connectors.NetworkShare;
import net.bpiwowar.xpm.connectors.SingleHostConnector;
import net.bpiwowar.xpm.exceptions.ExperimaestroCannotOverwrite;
import net.bpiwowar.xpm.exceptions.XPMRuntimeException;
import net.bpiwowar.xpm.manager.Constants;
import net.bpiwowar.xpm.scheduler.DependencyParameters;
import net.bpiwowar.xpm.scheduler.Resource;
import net.bpiwowar.xpm.scheduler.Scheduler;
import net.bpiwowar.xpm.scheduler.TokenResource;
import net.bpiwowar.xpm.utils.log.Logger;
import org.apache.log4j.Level;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

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
            Context.get().postProcess(tokenResource);
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

    @Expose
    @Help("Set the simulate flag: When true, the jobs are not submitted but just output")
    public boolean simulate(boolean simulate) {
        final boolean old = Context.get().simulate();
        Context.get().simulate(simulate);
        return old;
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
