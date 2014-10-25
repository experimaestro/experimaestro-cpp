package sf.net.experimaestro.scheduler;

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

import org.json.simple.JSONObject;
import sf.net.experimaestro.connectors.*;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.utils.log.Logger;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static sf.net.experimaestro.connectors.UnixScriptProcessBuilder.protect;

/**
 * A command line task (executed with the default shell)
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@DiscriminatorValue(Resource.COMMAND_LINE_JOB_TYPE)
@Entity
public class CommandLineTask extends Job {
    final static private Logger LOGGER = Logger.getLogger();
    /**
     * The environment
     */
    public TreeMap<String, String> environment = null;
    /**
     * Working directory
     */
    public String workingDirectory;
    /**
     * Our command line launcher
     */
    Launcher launcher;

    /**
     * The command to execute
     */
    private Commands commands;

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

    /**
     * Constructs the commands line
     *
     * @param commands  The commands with arguments
     */
    public CommandLineTask(Connector connector, Path path,
                           Commands commands, Map<String, String> environment, String workingDirectory) {

        super(connector, path);

        launcher = new DefaultLauncher();

        LOGGER.debug("Command is %s", commands);

        // Copy the environment
        if (environment != null)
            this.environment = new TreeMap<>(environment);
        this.workingDirectory = workingDirectory;

        // Construct commands
        this.commands = commands;

        // Adds all dependencies
        this.commands.forEachDependency(d -> addDependency(d));
    }

    /**
     * New command line task
     *
     * @param commands  The commands to run
     */
    public CommandLineTask(Connector connector, Path path,
                           Commands commands) {
        this(connector, path, commands, null, null);
    }

    /**
     * Get a full command line from an array of arguments
     */
    public static String getCommandLine(List<String> args) {
        return bpiwowar.argparser.utils.Output.toString(" ", args, t -> protect(t, UnixScriptProcessBuilder.SHELL_SPECIAL));
    }


    @Override
    protected XPMProcess startJob(ArrayList<Lock> locks) throws Exception {
        SingleHostConnector connector = getConnector().getConnector(null);

        final Path runFile = RUN_EXTENSION.transform(path);
        LOGGER.info("Starting command with run file [%s]", runFile);
        XPMScriptProcessBuilder builder = launcher.scriptProcessBuilder(connector, runFile);

        // Sets the command
        builder.job(this);

        // The job will be run in detached mode
        builder.detach(true);


        // Write the input if needed
        AbstractProcessBuilder.Redirect jobInput = AbstractCommandBuilder.Redirect.INHERIT;

        if (jobInputString != null) {
            Path inputFile = INPUT_EXTENSION.transform(path);
            final OutputStream outputStream = Files.newOutputStream(inputFile);
            outputStream.write(jobInputString.getBytes());
            outputStream.close();
            jobInputPath = getMainConnector().resolve(inputFile);
        }

        if (jobInputPath != null)
            jobInput = AbstractCommandBuilder.Redirect.from(getMainConnector().resolveFile(jobInputPath));

        if (jobOutputPath != null)
            builder.redirectOutput(AbstractCommandBuilder.Redirect.to(getMainConnector().resolveFile(jobOutputPath)));
        else
            builder.redirectOutput(AbstractCommandBuilder.Redirect.to(OUT_EXTENSION.transform(path)));

        // Redirect output & error streams into corresponding files
        if (jobErrorPath != null)
            builder.redirectError(AbstractCommandBuilder.Redirect.to(getMainConnector().resolveFile(jobErrorPath)));
        else
            builder.redirectError(AbstractCommandBuilder.Redirect.to(ERR_EXTENSION.transform(path)));

        builder.redirectInput(jobInput);

        builder.directory(path.getParent());

        if (environment != null)
            builder.environment(environment);

        // Add commands
        builder.commands(commands);

        builder.exitCodeFile(CODE_EXTENSION.transform(path));
        builder.doneFile(DONE_EXTENSION.transform(path));
        builder.removeLock(LOCK_EXTENSION.transform(path));

        // Start
        return builder.start();
    }

    @Override
    public void printXML(PrintWriter out, PrintConfig config) {
        super.printXML(out, config);
        out.format("<div><b>Command</b>: %s</div>", commands.toString());
        out.format("<div><b>Working directory</b> %s</div>", workingDirectory);
        out.format("<div><b>Environment</b>: %s</div>", environment);
    }

    @Override
    public JSONObject toJSON() throws IOException {
        JSONObject info = super.toJSON();
        info.put("command", commands.toString());
        info.put("working-directory", workingDirectory);
        info.put("environment", environment);
        return info;
    }

    public void setLauncher(Launcher launcher) {
        this.launcher = launcher;
    }

    /**
     * Sets the input for the command line to be a string content
     */
    public void setInput(String jobInput) {
        this.jobInputPath = null;
        this.jobInputString = jobInput;
    }

    /**
     * Sets the input to be a file
     */
    public void setInput(Path fileObject) {
        this.jobInputPath = fileObject.toString();
        this.jobInputString = null;
    }

    public void setOutput(Path fileObject) {
        this.jobOutputPath = fileObject.toString();
    }

    public void setError(Path fileObject) {
        this.jobErrorPath = fileObject.toString();
    }

    public Commands getCommands() {
        return commands;
    }

    @Override
    public Path outputFile() throws FileSystemException {
        if (jobOutputPath != null) {
            return getMainConnector().resolveFile(jobOutputPath);
        }
        return OUT_EXTENSION.transform(path);
    }
}
