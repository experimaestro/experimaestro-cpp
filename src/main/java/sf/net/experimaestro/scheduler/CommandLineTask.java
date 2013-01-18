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
import org.apache.commons.vfs2.FileObject;
import sf.net.experimaestro.connectors.*;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.utils.log.Logger;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static sf.net.experimaestro.connectors.UnixProcessBuilder.protect;

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
    private CommandArguments command;

    /**
     * The environment
     */
    public TreeMap<String, String> environment = null;

    /**
     * Working directory
     */
    public String workingDirectory;

    /**
     * The input source, if any (path from the main from)
     */
    private String jobInputPath;

    /**
     * If the input source is a string (and jobInputPath is PIPE), store it in this variable
     */
    private String jobInputString;

    /**
     * Path for job output
     */
    private String jobOutputPath;

    /**
     * Path for job error stream
     */
    private String jobErrorPath;

    protected CommandLineTask() {
    }

    /**
     * Constructs the command line
     *
     * @param scheduler  The scheduler for this command
     * @param identifier The from of the command (this will be used for the path of the files)
     * @param command    The command with arguments
     */
    public CommandLineTask(Scheduler scheduler, Connector connector, String identifier,
                           CommandArguments command, Map<String, String> environment, String workingDirectory) {

        super(scheduler, connector, identifier);

        launcher = new DefaultLauncher();

        LOGGER.info("Command is %s", command.toString());

        // Copy the environment
        if (environment != null)
            this.environment = new TreeMap<>(environment);
        this.workingDirectory = workingDirectory;

        // Construct command
        this.command = command;
    }

    /**
     * New command line task
     *
     * @param scheduler  The scheduler
     * @param connector  The connector
     * @param identifier The resource identifier
     * @param command    The command to run
     */
    public CommandLineTask(Scheduler scheduler, Connector connector, String identifier,
                           CommandArguments command) {
        this(scheduler, connector, identifier, command, null, null);
    }

    /**
     * Get a full command line from an array of arguments
     */
    public static String getCommandLine(List<String> args) {
        return bpiwowar.argparser.utils.Output.toString(" ", args, new Formatter<String>() {
            public String format(String t) {
                return protect(t, UnixProcessBuilder.SHELL_SPECIAL);
            }
        });
    }


    @Override
    protected XPMProcess startJob(ArrayList<Lock> locks) throws Exception {
        SingleHostConnector connector = getConnector().getConnector(null);

        final FileObject runFile = locator.resolve(connector, RUN_EXTENSION);
        LOGGER.info("Starting command with run file [%s]", runFile);
        XPMScriptProcessBuilder builder = launcher.scriptProcessBuilder(connector, runFile);

        // Sets the command
        builder.job(this);

        // The job will be run in detached mode
        builder.detach(true);


        // Write the input if needed
        XPMProcessBuilder.Redirect jobInput = XPMProcessBuilder.Redirect.INHERIT;

        if (jobInputString != null) {
            FileObject inputFile = locator.resolve(connector, INPUT_EXTENSION);
            final OutputStream outputStream = inputFile.getContent().getOutputStream();
            outputStream.write(jobInputString.getBytes());
            outputStream.close();
            jobInputPath = getMainConnector().resolve(inputFile);
        }

        if (jobInputPath != null)
            jobInput = XPMProcessBuilder.Redirect.from(getMainConnector().resolveFile(jobInputPath));

        if (jobOutputPath != null)
            builder.redirectOutput(XPMProcessBuilder.Redirect.to(getMainConnector().resolveFile(jobOutputPath)));
        else
            builder.redirectOutput(XPMProcessBuilder.Redirect.to(locator.resolve(connector, OUT_EXTENSION)));

        // Redirect output & error streams into corresponding files
        if (jobErrorPath != null)
            builder.redirectError(XPMProcessBuilder.Redirect.to(getMainConnector().resolveFile(jobErrorPath)));
        else
            builder.redirectError(XPMProcessBuilder.Redirect.to(locator.resolve(connector, ERR_EXTENSION)));

        builder.redirectInput(jobInput);

        // Add commands
        builder.command(command.toStrings(connector));
        builder.exitCodeFile(locator.resolve(connector, CODE_EXTENSION));
        builder.doneFile(locator.resolve(connector, DONE_EXTENSION));
        builder.removeLock(locator.resolve(connector, LOCK_EXTENSION));

        // Start
        return builder.start();
    }

    @Override
    public void printXML(PrintWriter out, PrintConfig config) {
        super.printXML(out, config);
        out.format("<div><b>Command</b>: %s</div>", command.toString());
        out.format("<div><b>Working directory</b> %s</div>", workingDirectory);
        out.format("<div><b>Environment</b>: %s</div>", environment);
    }

    public void setLauncher(Launcher launcher) {
        this.launcher = launcher;
    }

    public void setInput(String jobInput) {
        this.jobInputPath = null;
        this.jobInputString = jobInput;
    }

    public void setOutput(FileObject fileObject) {
        this.jobOutputPath = fileObject.toString();
    }

    public void setError(FileObject fileObject) {
        this.jobErrorPath = fileObject.toString();
    }
}
