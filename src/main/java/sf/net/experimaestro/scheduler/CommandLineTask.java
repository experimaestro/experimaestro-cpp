/*
 * This file is part of experimaestro.
 * Copyright (c) 2012 B. Piwowarski <benjamin@bpiwowar.net>
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

package sf.net.experimaestro.scheduler;

import bpiwowar.argparser.utils.Formatter;
import com.sleepycat.persist.model.Persistent;
import sf.net.experimaestro.connectors.*;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.utils.arrays.ListAdaptator;
import sf.net.experimaestro.utils.log.Logger;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

/**
 * A command line task (executed with the default shell)
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Persistent
public class CommandLineTask extends Job {
    final static private Logger LOGGER = Logger.getLogger();

    /**
     * Our command line launcher
     */
    Launcher launcher;


    /**
     * The command to execute
     */
    private String[] command;

    /**
     * The environment
     */
    public String[] envp = null;

    public String workingDirectory;

    protected CommandLineTask() {
    }

    /**
     * Constructs the command line
     *
     * @param scheduler  The scheduler for this command
     * @param identifier The locator of the command (this will be used for the path of the files)
     * @param command    The command with arguments
     */
    public CommandLineTask(Scheduler scheduler, Connector connector, String identifier,
                           String[] command, Map<String, String> environment, String workingDirectory) {

        super(scheduler, connector, identifier);

        launcher = new DefaultLauncher();

        LOGGER.info("Command is %s", Arrays.toString(command));

        // Copy the environment
        if (environment != null) {
            envp = new String[environment.size()];
            int i = 0;
            for (Map.Entry<String, String> entry : environment.entrySet())
                envp[i++] = format("%s=%s", entry.getKey(), entry.getValue());
        }
        this.workingDirectory = workingDirectory;

        // Construct command
        this.command = command;
    }

    /**
     * New command line task
     *
     * @param scheduler
     * @param identifier
     * @param command
     */
    public CommandLineTask(Scheduler scheduler, Connector connector, String identifier,
                           String[] command) {
        this(scheduler, connector, identifier, command, null, null);
    }

    /**
     * Get a full command line from an array of arguments
     */
    public static String getCommandLine(List<String> args) {
        return bpiwowar.argparser.utils.Output.toString(" ", args, new Formatter<String>() {
            public String format(String t) {
                return protect(t, " \"'");
            }
        });
    }

    /**
     * Helper method to call {@linkplain #getCommandLine(java.util.List)}
     */
    public static String getCommandLine(String[] args) {
        return getCommandLine(ListAdaptator.create(args));
    }

    @Override
    protected XPMProcess startJob(ArrayList<Lock> locks) throws Exception {
        SingleHostConnector connector = getConnector().getConnector(null);
        XPMProcessBuilder builder = launcher.processBuilder(connector);

        builder.command(command);
        builder.detach(true);

        // TODO: redirect output & input

        return builder.start();
    }

    @Override
    public void printHTML(PrintWriter out, PrintConfig config) {
        super.printHTML(out, config);
        out.format("<div><b>Command</b>: %s</div>", command[2]);
        out.format("<div><b>Working directory</b> %s</div>", workingDirectory);
        out.format("<div><b>Environment</b>: %s</div>", Arrays.toString(envp));
    }

    /**
     * XPMProcess one argument, adding backslash if necessary to protect special
     * characters.
     *
     * @param string
     * @return
     */
    static public String protect(String string, String special) {
        if (string.equals(""))
            return "\"\"";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < string.length(); i++) {
            final char c = string.charAt(i);
            if (special.indexOf(c) != -1)
                sb.append("\\");
            sb.append(c);
        }
        return sb.toString();
    }

    public String[] getCommand() {
        return command;
    }


    public void setLauncher(Launcher launcher) {
        this.launcher = launcher;
    }
}
