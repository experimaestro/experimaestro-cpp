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

package sf.net.experimaestro.connectors;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import sf.net.experimaestro.exceptions.LaunchException;
import sf.net.experimaestro.scheduler.Command;
import sf.net.experimaestro.scheduler.CommandComponent;
import sf.net.experimaestro.scheduler.CommandEnvironment;
import sf.net.experimaestro.scheduler.Commands;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Map;

import static sf.net.experimaestro.scheduler.Command.SubCommand;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 13/9/12
 */
public class UnixScriptProcessBuilder extends XPMScriptProcessBuilder {

    public static final String SHELL_SPECIAL = " \"'<>\n";
    public static final String QUOTED_SPECIAL = "\"$";
    /**
     * Lock files to delete
     */
    ArrayList<String> lockFiles = new ArrayList<>();
    private String shPath = "/bin/bash";
    /**
     * File where the exit code is written
     */
    private String exitCodePath;
    /**
     * File where the exit code is written
     */
    private String donePath;

    public UnixScriptProcessBuilder(FileObject file, SingleHostConnector connector) throws FileSystemException {
        super(connector, file, null);
    }

    public UnixScriptProcessBuilder(FileObject scriptFile, SingleHostConnector connector, AbstractProcessBuilder processBuilder) throws FileSystemException {
        super(connector, scriptFile, processBuilder);
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

    @Override
    final public XPMProcess start() throws LaunchException, IOException {
        final FileObject runFile = connector.resolveFile(path);
        final FileObject basepath = runFile.getParent();
        final String baseName = runFile.getName().getBaseName();

        try (CommandEnvironment env = new CommandEnvironment.FolderEnvironment(connector, basepath, baseName)) {
            // First generate the run file
            PrintWriter writer = new PrintWriter(runFile.getContent().getOutputStream());

            writer.format("#!%s%n", shPath);

            writer.format("# Experimaestro generated task: %s%n", path);
            writer.println();

            // A command fails if any of the piped commands fail
            writer.println("set -o pipefail");
            writer.println();

            writer.println();
            if (environment() != null) {
                for (Map.Entry<String, String> pair : environment().entrySet())
                    writer.format("export %s=\"%s\"%n", pair.getKey(), protect(pair.getValue(), QUOTED_SPECIAL));
            }

            if (directory() != null) {
                writer.format("cd \"%s\"%n", protect(env.resolve(directory()), QUOTED_SPECIAL));
            }

            writer.format("%n#Checks that the locks are set%n");
            for (String lockFile : lockFiles) {
                writer.format("test -f %s || exit 017%n", lockFile);
            }

            writer.format("%n%n# Set traps to remove locks when exiting%n%n");
            writer.format("trap cleanup EXIT%n");
            writer.format("cleanup() {%n");
            for (String lockFile : lockFiles)
                writer.format("  rm -f %s;%n", lockFile);
            writer.format("}%n%n");


            // Write the command
            final StringWriter sw = new StringWriter();
            PrintWriter exitWriter = new PrintWriter(sw);
            exitWriter.format("code=$?; if test $code -ne 0; then%n");
            if (exitCodePath != null)
                exitWriter.format(" echo $code > \"%s\"%n", protect(exitCodePath, QUOTED_SPECIAL));
            exitWriter.format(" exit $code%n");
            exitWriter.format("fi%n");

            String exitScript = sw.toString();

            writer.format("%n%n");

            switch (input.type()) {
                case INHERIT:
                    break;
                case READ:
                    writer.format("cat \"%s\" | ", connector.resolve(input.file()));
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported input redirection type: " + input.type());
            }

            writer.println("(");

            // The prepare all the commands
            writeCommands(env, writer, commands());

            writer.print(") ");

            switch (output.type()) {
                case INHERIT:
                    break;
                case APPEND:
                    writer.format(" >> %s", protect(connector.resolve(output.file()), QUOTED_SPECIAL));
                    break;
                case WRITE:
                    writer.format(" > %s", protect(connector.resolve(output.file()), QUOTED_SPECIAL));
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported output redirection type: " + input.type());

            }

            switch (error.type()) {
                case INHERIT:
                    break;
                case APPEND:
                    writer.format(" 2>> %s", protect(connector.resolve(error.file()), QUOTED_SPECIAL));
                    break;
                case WRITE:
                    writer.format(" 2> %s", protect(connector.resolve(error.file()), QUOTED_SPECIAL));
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported error redirection type: " + input.type());
            }

            writer.println();
            writer.print(exitScript);

            if (exitCodePath != null)
                writer.format("echo 0 > \"%s\"%n", protect(exitCodePath, QUOTED_SPECIAL));
            if (donePath != null)
                writer.format("touch \"%s\"%n", protect(donePath, QUOTED_SPECIAL));

            writer.close();

            // Set the file as executable
            runFile.setExecutable(true, false);

            processBuilder.command(protect(path, SHELL_SPECIAL));

            processBuilder.detach(true);
            processBuilder.redirectOutput(output);
            processBuilder.redirectError(error);

            processBuilder.job(job);

            return processBuilder.start();
        } catch (Exception e) {
            throw new LaunchException(e);
        }

    }

    private void writeCommands(CommandEnvironment env, PrintWriter writer, Commands commands) throws IOException {
        commands.reorder();

        for (Command command : commands) {
            for (CommandComponent argument : command.list()) {
                writer.print(' ');
                if (argument instanceof Command.Pipe) {
                    writer.print(" | ");
                } else if (argument instanceof SubCommand) {
                    writer.println(" (");
                    writeCommands(env, writer, ((SubCommand) argument).commands());
                    writer.println();
                    writer.print(" )");
                } else {
                    writer.print(protect(argument.prepare(env), SHELL_SPECIAL));
                }
            }
            // Stop if an error occured
            writer.println(" || exit $?");
        }
    }


    @Override
    public void removeLock(FileObject lockFile) throws FileSystemException {
        lockFiles.add(protect(connector.resolve(lockFile), SHELL_SPECIAL));
    }

    @Override
    public void exitCodeFile(FileObject exitCodeFile) throws FileSystemException {
        exitCodePath = connector.resolve(exitCodeFile);
    }

    @Override
    public void doneFile(FileObject doneFile) throws FileSystemException {
        donePath = connector.resolve(doneFile);
    }
}
