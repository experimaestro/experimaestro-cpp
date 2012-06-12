/*
 *
 *  This file is part of experimaestro.
 *  Copyright (c) 2011 B. Piwowarski <benjamin@bpiwowar.net>
 *
 *  experimaestro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  experimaestro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package sf.net.experimaestro.scheduler;

import static java.lang.String.format;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import bpiwowar.argparser.utils.Formatter;
import bpiwowar.argparser.utils.Output;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.manager.js.JSLauncher;
import sf.net.experimaestro.utils.arrays.ListAdaptator;
import sf.net.experimaestro.utils.log.Logger;

import com.sleepycat.persist.model.Persistent;

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
    Launcher launcher = new ShLauncher();

    /**
     * The command to execute
     */
    private String[] command;

    private String[] envp = null;

    private String workingDirectory;

    protected CommandLineTask() {
    }

    /**
     * Constructs the command line
     *
     * @param scheduler  The scheduler for this command
     * @param identifier The identifier of the command (this will be used for the path of the files)
     * @param command    The command with arguments
     */
    public CommandLineTask(Scheduler scheduler, String identifier,
                           String[] command, Map<String, String> environment, String workingDirectory) {

        super(scheduler, identifier);

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
    public CommandLineTask(Scheduler scheduler, String identifier,
                           String[] command) {
        this(scheduler, identifier, command, null, null);
    }

    @Override
    protected int doRun(ArrayList<Lock> locks) throws Exception {


        // Write command
        final String runId = String.format("%s.run",
                identifier);
        PrintWriter writer = connector.printWriter(runId);


        writer.format("# Experimaestro generated task: %s%n", identifier);
        writer.println();
        if (envp != null) {
            for (String env : envp)
                writer.println(env);
            writer.println();
        }
        if (workingDirectory != null)
            writer.format("cd \"%s\"%n%n", protect(workingDirectory, "\""));

        writer.println(Output.toString(" ", ListAdaptator.create(command), new Formatter<String>() {
            public String format(String t) {
                return CommandLineTask.protect(t, " \"'");
            }
        }));
        writer.close();

        // --- Execute command and return error code
        return connector.exec(identifier, launcher.getCommand(identifier), locks);
    }

    @Override
    public void printHTML(PrintWriter out, PrintConfig config) {
        super.printHTML(out, config);
        out.format("<div><b>Command</b>: %s</div>", command[2]);
        out.format("<div><b>Working directory</b> %s</div>", workingDirectory);
        out.format("<div><b>Environment</b>: %s</div>", Arrays.toString(envp));
    }

    /**
     * Process one argument, adding backslash if necessary to protect special
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

    public void setLauncher(Launcher launcher) {
        this.launcher = launcher;
    }
}
