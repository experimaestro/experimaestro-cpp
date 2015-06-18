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
import sf.net.experimaestro.manager.scripting.Exposed;
import sf.net.experimaestro.utils.log.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import static sf.net.experimaestro.connectors.UnixScriptProcessBuilder.protect;

/**
 * A command line task (executed with the default shell)
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Exposed
@TypeIdentifier("COMMANDLINE")
public class CommandLineTask extends Job {
    final static private Logger LOGGER = Logger.getLogger();
    /**
     * The environment
     */
    public Map<String, String> environment = null;

    /**
     * Working directory
     */
    public String workingDirectory;

    /**
     * Our command line launcher
     */
    Launcher launcher;

    /**
     * The command status execute
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

    public CommandLineTask(Long id, String locator) {
        super(id, locator);
    }

    public CommandLineTask(Connector connector, String path) {
        super(connector, path);

    }

    public CommandLineTask(Connector connector, Path path) throws IOException {
        super(connector, path);
    }


    /**
     * Get a full command line from an array of arguments
     */
    public static String getCommandLine(List<String> args) {
        return bpiwowar.argparser.utils.Output.toString(" ", args, t -> protect(t, UnixScriptProcessBuilder.SHELL_SPECIAL));
    }


    @Override
    public XPMProcess start(ArrayList<Lock> locks, boolean fake) throws Exception {
        SingleHostConnector singleHostConnector = getMainConnector();

        final Path runFile = Resource.RUN_EXTENSION.transform(getPath());
        LOGGER.info("Starting command with run file [%s]", runFile);
        XPMScriptProcessBuilder builder = launcher.scriptProcessBuilder(singleHostConnector, runFile);

        // Sets the command
        builder.job(this);

        // The job will be run in detached mode
        builder.detach(true);


        // Write the input if needed
        AbstractProcessBuilder.Redirect jobInput = AbstractCommandBuilder.Redirect.INHERIT;

        if (jobInputString != null) {
            Path inputFile = Resource.INPUT_EXTENSION.transform(getPath());
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
            builder.redirectOutput(AbstractCommandBuilder.Redirect.to(Resource.OUT_EXTENSION.transform(getPath())));

        // Redirect output & error streams into corresponding files
        if (jobErrorPath != null)
            builder.redirectError(AbstractCommandBuilder.Redirect.to(getMainConnector().resolveFile(jobErrorPath)));
        else
            builder.redirectError(AbstractCommandBuilder.Redirect.to(Resource.ERR_EXTENSION.transform(getPath())));

        builder.redirectInput(jobInput);

        builder.directory(getPath().getParent());

        if (environment != null)
            builder.environment(environment);

        // Add commands
        builder.commands(commands);

        builder.exitCodeFile(Resource.CODE_EXTENSION.transform(getPath()));
        builder.doneFile(Resource.DONE_EXTENSION.transform(getPath()));
        builder.removeLock(Resource.LOCK_EXTENSION.transform(getPath()));

        // Start
        return builder.start(fake);
    }

    public JSONObject toJSON() throws IOException {
        JSONObject info = new JSONObject();
        info.put("command", commands.toString());
        info.put("working-directory", workingDirectory);
        info.put("environment", environment);
        return info;
    }

    public void setLauncher(Launcher launcher) {
        this.launcher = launcher;
    }

    /**
     * Sets the input for the command line status be a string content
     */
    public void setInput(String jobInput) {
        this.jobInputPath = null;
        this.jobInputString = jobInput;
    }

    /**
     * Sets the input status be a file
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
    public Path outputFile() throws IOException {
        if (jobOutputPath != null) {
            return getMainConnector().resolveFile(jobOutputPath);
        }
        return Resource.OUT_EXTENSION.transform(getPath());
    }

    @Override
    public Stream<Dependency> dependencies() {
        return commands.dependencies();
    }

    @Override
    public boolean isActiveWaiting() {
        return launcher.getNotificationURL() == null;
    }

    public void setCommands(Commands commands) {
        this.commands = commands;
    }
}
