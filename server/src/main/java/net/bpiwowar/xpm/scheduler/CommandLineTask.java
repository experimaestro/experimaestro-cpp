package net.bpiwowar.xpm.scheduler;

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

import net.bpiwowar.xpm.commands.AbstractCommand;
import net.bpiwowar.xpm.commands.Redirect;
import net.bpiwowar.xpm.commands.XPMScriptProcessBuilder;
import net.bpiwowar.xpm.connectors.Launcher;
import net.bpiwowar.xpm.connectors.XPMProcess;
import net.bpiwowar.xpm.locks.Lock;
import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.utils.UUIDObject;
import net.bpiwowar.xpm.utils.log.Logger;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static net.bpiwowar.xpm.utils.PathUtils.SHELL_SPECIAL;
import static net.bpiwowar.xpm.utils.PathUtils.protect;

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
     * Launcher parameters
     */
    LauncherParameters parameters;

    /**
     * The command status execute
     */
    private AbstractCommand command;

    /**
     * The input source, if any (path from the main from)
     */
    private Path jobInputPath;

    /**
     * If the input source is a string (and jobInputPath is PIPE), store it in this variable
     */
    private String jobInputString;

    /**
     * Path for job output
     */
    private Path jobOutputPath;

    /**
     * Path for job error stream
     */
    private Path jobErrorPath;

    public CommandLineTask(long id, Path path) throws SQLException {
        super(id, path);
    }

    public CommandLineTask(Path path) throws IOException {
        super(path);
    }


    /**
     * Get a full command line from an array of arguments
     */
    public static String getCommandLine(List<String> args) {
        return bpiwowar.argparser.utils.Output.toString(" ", args, t -> protect(t, SHELL_SPECIAL));
    }


    @Override
    public XPMProcess start(ArrayList<Lock> locks, boolean fake) throws Exception {
        loadData();

        final Path runFile = Resource.RUN_EXTENSION.transform(getLocator());
        LOGGER.info("Starting command with run file [%s]", runFile);
        XPMScriptProcessBuilder builder = launcher.scriptProcessBuilder(runFile, parameters);

        // Sets the command
        builder.job(this);

        // The job will be run in detached mode
        builder.detach(true);


        // Write the input if needed
        Redirect jobInput = Redirect.INHERIT;

        Path jobInputPath = this.jobInputPath;
        if (jobInputString != null) {
            Path inputFile = Resource.INPUT_EXTENSION.transform(getLocator());
            final OutputStream outputStream = Files.newOutputStream(inputFile);
            outputStream.write(jobInputString.getBytes());
            outputStream.close();
            jobInputPath = inputFile;
        }

        if (jobInputPath != null) {
            jobInput = Redirect.from(jobInputPath);
        }

        if (jobOutputPath != null)
            builder.redirectOutput(Redirect.to(jobOutputPath));
        else
            builder.redirectOutput(Redirect.to(Resource.OUT_EXTENSION.transform(getLocator())));

        // Redirect output & error streams into corresponding files
        if (jobErrorPath != null)
            builder.redirectError(Redirect.to(jobErrorPath));
        else
            builder.redirectError(Redirect.to(Resource.ERR_EXTENSION.transform(getLocator())));

        builder.redirectInput(jobInput);

        builder.directory(getLocator().getParent());

        if (environment != null)
            builder.environment(environment);

        // Add command
        builder.command(command);

        builder.exitCodeFile(Resource.CODE_EXTENSION.transform(getLocator()));
        builder.doneFile(Resource.DONE_EXTENSION.transform(getLocator()));
        builder.removeLock(Resource.LOCK_EXTENSION.transform(getLocator()));

        // Start
        return builder.start(fake);
    }

    public JSONObject toJSON() throws IOException {
        loadData();
        JSONObject info = super.toJSON();
        info.put("command", command.toString());
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
    public void setInput(Path jobInputPath) {
        this.jobInputPath = jobInputPath;
        this.jobInputString = null;
    }

    public void setOutput(Path outputPath) {
        this.jobOutputPath = outputPath;
    }

    public void setError(Path errorPath) {
        this.jobErrorPath = errorPath;
    }

    public AbstractCommand getCommand() {
        loadData();
        return command;
    }


    @Override
    public Path outputFile() throws IOException {
        if (jobOutputPath != null) {
            return jobOutputPath;
        }
        return Resource.OUT_EXTENSION.transform(getLocator());
    }

    @Override
    public Stream<? extends Dependency> dependencies() {
        return command.dependencies();
    }

    @Override
    public boolean isActiveWaiting() {
        return launcher.getNotificationURL() == null;
    }

    public void setCommand(AbstractCommand command) {
        this.command = command;
    }

    @Override
    protected boolean loadData() {
        if (super.loadData()) {
            // Fill the UUID map
            final HashMap<String, Object> uuidMap = new HashMap<>();
            command.commands().forEach(c -> {
                if (c instanceof UUIDObject) {
                    uuidMap.put(c.getUUID(), c);
                }
            });

            // resolve
            command.allComponents().forEach(c -> c.init(uuidMap));

            return true;
        }

        return false;
    }
}
