package net.bpiwowar.xpm.commands;

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


import com.google.common.collect.Iterables;
import net.bpiwowar.xpm.connectors.AbstractProcessBuilder;
import net.bpiwowar.xpm.connectors.Launcher;
import net.bpiwowar.xpm.connectors.SingleHostConnector;
import net.bpiwowar.xpm.connectors.XPMProcess;
import net.bpiwowar.xpm.exceptions.LaunchException;
import net.bpiwowar.xpm.manager.Constants;
import net.bpiwowar.xpm.scheduler.LauncherParameters;
import net.bpiwowar.xpm.utils.Functional;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static net.bpiwowar.xpm.utils.PathUtils.QUOTED_SPECIAL;
import static net.bpiwowar.xpm.utils.PathUtils.SHELL_SPECIAL;
import static net.bpiwowar.xpm.utils.PathUtils.protect;

/**
 * Class that knows how to build UNIX scripts to run command
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class UnixScriptProcessBuilder extends XPMScriptProcessBuilder {

    /**
     * Lock files to delete
     */
    ArrayList<String> lockFiles = new ArrayList<>();

    /**
     * Commands to be executed to notify the end of the job
     */
    Commands endOfJobCommands;

    /**
     * Path to /bin/sh
     */
    private String shPath = "/bin/bash";

    /**
     * File where the exit code is written
     */
    private Path exitCodePath;

    /**
     * File where the exit code is written
     */
    private Path donePath;

    /**
     * If cleanup should be performed on script exit
     */
    private boolean doCleanup = true;

    /**
     * Pre-process command
     */
    private Commands preprocessCommands = null;

    /** PID file (or null) */
    private Path pidFile;

    /**
     * Path for preventing bad starts
     */
    private Path startlockPath;

    /**
     * Commands
     *
     * @param file
     * @param launcher
     * @param parameters
     * @throws IOException
     */

    public UnixScriptProcessBuilder(Path file, Launcher launcher, LauncherParameters parameters) throws IOException {
        super(launcher, parameters, file, null);
    }

    public UnixScriptProcessBuilder(Path scriptFile, Launcher launcher, LauncherParameters parameters, AbstractProcessBuilder processBuilder) throws IOException {
        super(launcher, parameters, scriptFile, processBuilder);
    }

    /**
     * Sets end of job command
     */
    public void endOfJobCommands(Commands commands) {
        this.endOfJobCommands = commands;
    }

    @Override
    final public XPMProcess start(boolean fake) throws LaunchException, IOException {
        final Path runFile = path;
        final SingleHostConnector mainConnector = launcher.getMainConnector();
        String pathString = mainConnector.resolve(path);
        final Path basepath = runFile.getParent();
        Files.createDirectories(basepath);
        final String baseName = runFile.getFileName().toString();

        try (CommandContext env = new CommandContext.FolderContext(launcher, basepath, baseName)) {
            // Prepare the command
            command().prepare(env);

            // First generate the run file
            PrintWriter writer = new PrintWriter(Files.newOutputStream(runFile));

            writer.format("#!%s%n", shPath);

            writer.format("# Experimaestro generated task: %s%n", path);
            writer.println();

            // Use pipefail for fine grained analysis of errors in commands
            writer.println("set -o pipefail");
            writer.println();

            if (pidFile != null) {
                writer.format("echo $? > \"%s\"%n", protect(env.resolve(pidFile, null), QUOTED_SPECIAL));
            }

            writer.println();
            if (environment() != null) {
                for (Map.Entry<String, String> pair : environment().entrySet())
                    writer.format("export %s=\"%s\"%n", pair.getKey(), protect(pair.getValue(), QUOTED_SPECIAL));
            }

            // Adds notification URL to script
            if (notificationURL != null) {
                writer.format("export %s=\"%s/%d\"%n", Constants.XPM_NOTIFICATION_URL,
                        protect(getNotificationURL().toString(), QUOTED_SPECIAL), job.getId());
            }

            if (directory() != null) {
                writer.format("cd \"%s\"%n", protect(env.resolve(directory(), null), QUOTED_SPECIAL));
            }

            // Write some command
            if (preprocessCommands != null) {
                writeCommands(env, writer, preprocessCommands);
            }

            // --- CLEANUP

            writer.format("cleanup() {%n");
            // Write something
            writer.format(" echo Cleaning up 1>&2%n");
            // Remove traps
            writer.format(" trap - 0%n");

            if (pidFile != null) {
                writer.format(" rm -f %s;%n", env.resolve(pidFile, basepath));
            }

            // Remove locks
            for (String file : lockFiles) {
                writer.format(" rm -f %s;%n", file);
            }

            // Remove temporary files
            command().commands().forEach(Functional.propagate(c -> {
                final CommandContext.NamedPipeRedirections namedRedirections = env.getNamedRedirections(c, false);
                for (Path file : Iterables.concat(namedRedirections.outputRedirections,
                        namedRedirections.errorRedirections)) {
                    writer.format(" rm -f %s;%n", env.resolve(file, basepath));
                }
            }));

            // Notify if possible
            if (notificationURL != null) {
                writer.format(" wget --tries=1 --connect-timeout=1 --read-timeout=1 --quiet -O /dev/null \"$" + Constants.XPM_NOTIFICATION_URL + "/eoj\"%n");
            }

            // Kills remaining processes
            writer.println(" test ! -z \"$PID\" && pkill -KILL -P $PID");

            writer.format("}%n%n");

            // --- END CLEANUP

            if (startlockPath != null) {
                writer.format("# Checks that the start lock is set, and removes it%n");
                writer.format("test -f %s || exit 017%n", mainConnector.resolve(startlockPath));
                writer.format("rm -f %s", mainConnector.resolve(startlockPath));
            }

            if (!lockFiles.isEmpty()) {
                writer.format("# Checks that the locks are set%n");
                for (String lockFile : lockFiles) {
                    writer.format("test -f %s || exit 017%n", lockFile);
                }
            }
            writer.println();

            if (doCleanup) {
                writer.format("# Set trap to cleanup when exiting%n");
                writer.format("trap cleanup 0%n");
            }

            // Write the command
            final StringWriter sw = new StringWriter();
            PrintWriter exitWriter = new PrintWriter(sw);
            exitWriter.format("code=$?%n");
            exitWriter.format("if test $code -ne 0; then%n");
            if (exitCodePath != null)
                exitWriter.format(" echo $code > \"%s\"%n", protect(env.resolve(exitCodePath, basepath), QUOTED_SPECIAL));
            exitWriter.format(" exit $code%n");
            exitWriter.format("fi%n");

            String exitScript = sw.toString();

            switch (input.type()) {
                case INHERIT:
                    break;
                case READ:
                    writer.format("cat \"%s\" | ", env.resolve(input.file(), basepath));
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported input redirection type: " + input.type());
            }

            writer.format("%ncheckerror()  { local e; for e in \"$@\"; do [[ \"$e\" != 0 ]] && [[ \"$e\" != 141 ]] && exit $e; done; return 0; }%n%n");
            writer.println("(");

            // The prepare all the command
            writeCommands(env, writer, command());

            writer.print(") ");

            writeRedirection(env, writer, output, 1);
            writeRedirection(env, writer, error, 2);

            // Retrieve PID
            writer.println(" & ");
            writer.println("PID=$!");
            writer.println("wait $PID");

            writer.print(exitScript);

            if (exitCodePath != null)
                writer.format("echo 0 > \"%s\"%n", protect(env.resolve(exitCodePath, basepath), QUOTED_SPECIAL));
            if (donePath != null)
                writer.format("touch \"%s\"%n", protect(env.resolve(donePath, basepath), QUOTED_SPECIAL));

            writer.close();

            // Set the file as executable
            Files.setPosixFilePermissions(runFile, PosixFilePermissions.fromString("rwxr-x---"));

            processBuilder.command(protect(pathString, SHELL_SPECIAL));

            processBuilder.detach(true);
            processBuilder.redirectOutput(output);
            processBuilder.redirectError(error);

            processBuilder.job(job);

            return processBuilder.start(fake);
        } catch (Exception e) {
            throw new LaunchException(e);
        }

    }

    private void writeRedirection(CommandContext env, PrintWriter writer, Redirect redirect, int stream) throws IOException {
        if (redirect == null) {
            writer.format(" %d> /dev/null", stream);
        } else {
            switch (redirect.type()) {
                case INHERIT:
                    break;
                case APPEND:
                    writer.format(" %d>> %s", stream, protect(env.resolve(redirect.file(), env.getWorkingDirectory()), QUOTED_SPECIAL));
                    break;
                case WRITE:
                    writer.format(" %d> %s", stream, protect(env.resolve(redirect.file(), env.getWorkingDirectory()), QUOTED_SPECIAL));
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported output redirection type: " + input.type());

            }
        }
    }

    private void writeCommands(CommandContext env, PrintWriter writer, AbstractCommand commands) throws IOException {
        final List<AbstractCommand> list = commands.reorder();

        int detached = 0;

        for (AbstractCommand command : list) {
            // Write files
            final CommandContext.NamedPipeRedirections namedRedirections = env.getNamedRedirections(command, false);

            // Write named pipes
            for (Path file : Iterables.concat(namedRedirections.outputRedirections, namedRedirections.errorRedirections)) {
                writer.format(" mkfifo \"%s\"%n", protect(env.resolve(file, env.getWorkingDirectory()), QUOTED_SPECIAL));
            }

            if (command.inputRedirect != null && command.inputRedirect.type() == Redirect.Type.READ) {
                writer.format("cat \"%s\" | ", protect(env.resolve(command.inputRedirect.file(), env.getWorkingDirectory()), QUOTED_SPECIAL));
            }

            if (command instanceof Commands) {
                writer.println("(");
                writeCommands(env, writer, command);
                writer.print(") ");
            } else {
                for (AbstractCommandComponent argument : ((Command) command).components()) {
                    if (argument instanceof Unprotected) {
                        writer.print(argument.toString(env));
                    }

                    writer.print(' ');
                    if (argument instanceof Pipe) {
                        writer.print(" | ");
                    } else if (argument instanceof SubCommand) {
                        writer.println(" (");
                        writeCommands(env, writer, ((SubCommand) argument).get());
                        writer.println();
                        writer.print(" )");
                    } else {
                        writer.print(protect(argument.toString(env), SHELL_SPECIAL));
                    }
                }
            }

            printRedirections(env, 1, writer, command.getOutputRedirect(), namedRedirections.outputRedirections);
            printRedirections(env, 2, writer, command.getErrorRedirect(), namedRedirections.errorRedirections);
            writer.print(" || checkerror \"${PIPESTATUS[@]}\" ");

            if (env.detached(command)) {
                // Just keep a pointer
                writer.format(" & CHILD_%d=$!%n", detached);
                detached++;
            } else {
                // Stop if an error occurred
                writer.println(" || exit $?");
            }
        }

        // Monitors detached jobs
        for (int i = 0; i < detached; i++) {
            writer.format("wait $CHILD_%d || exit $?%n", i);
        }
    }

    private void printRedirections(CommandContext env, int stream, PrintWriter writer, Redirect outputRedirect, List<Path> outputRedirects) throws IOException {
        if (!outputRedirects.isEmpty()) {

            // Special case : just one redirection
            if (outputRedirects.size() == 1 && outputRedirect == null) {
                writeRedirection(env, writer, Redirect.to(outputRedirects.get(0)), stream);
            } else {
                writer.format(" %d> >(tee", stream);
                for (Path file : outputRedirects) {
                    writer.format(" \"%s\"", protect(env.resolve(file, env.getWorkingDirectory()), QUOTED_SPECIAL));
                }
                writeRedirection(env, writer, outputRedirect, stream);
                writer.write(")");
            }
        } else {
            // Finally, write the main redirection
            writeRedirection(env, writer, outputRedirect, stream);
        }
    }


    @Override
    public void removeLock(Path lockFile) throws IOException {
        lockFiles.add(protect(launcher.resolve(lockFile), SHELL_SPECIAL));
    }

    @Override
    public void exitCodeFile(Path exitCodeFile) throws IOException {
        exitCodePath = exitCodeFile;
    }

    @Override
    public void pidFile(Path pidFile) {
        this.pidFile = pidFile;
    }

    @Override
    public void doneFile(Path donePath) throws IOException {
        this.donePath = donePath;
    }

    @Override
    public void startlock(Path donePath) throws IOException {
        this.startlockPath = donePath;
    }

    public void setDoCleanup(boolean doCleanup) {
        this.doCleanup = doCleanup;
    }

    public void preprocessCommands(Commands commands) {
        this.preprocessCommands = commands;
    }
}
