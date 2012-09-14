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
import sf.net.experimaestro.exceptions.LaunchException;
import sf.net.experimaestro.scheduler.CommandLineTask;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 13/9/12
 */
public abstract class UnixProcessBuilder extends XPMScriptProcessBuilder {

    public static final String SHELL_SPECIAL = " \"'";
    public static final String QUOTED_SPECIAL = "\"$";

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

    private String shPath = "/bin/bash";

    /**
     * Commands to execute
     */
    ArrayList<String> commands = new ArrayList<>();

    /**
     * Lock files to remove
     */
    ArrayList<String> lockFiles = new ArrayList<>();

    /**
     * File where the exit code is written
     */
    private String exitCodePath;

    /**
     * File where the exit code is written
     */
    private String donePath;

    public UnixProcessBuilder(FileObject file, SingleHostConnector connector) {
        super(connector, file);
    }

    /**
     * Generates the run file
     */
    protected void generateRunFile() throws Exception {
        // Write file
//            final String quotedPath = "\"" + CommandLineTask.protect(path, "\"") + "\"";

        final FileObject runFile = connector.resolveFile(path);
        PrintWriter writer = new PrintWriter(runFile.getContent().getOutputStream());

        writer.format("#!%s%n", shPath);

        writer.format("# Experimaestro generated task: %s%n", path);
        writer.println();
        if (environment() != null) {
            for (Map.Entry<String, String> pair : environment().entrySet())
                writer.format("%s=%s%n", pair.getKey(), pair.getValue());
        }

        if (directory() != null) {
            writer.format("cd \"%s\"%n", protect(directory(), QUOTED_SPECIAL));
        }

        writer.format("%n# Set traps to remove locks when exiting%n%n");
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
        for (String command : commands) {
            writer.println(command);
            writer.print(exitScript);
        }

        if (exitCodePath != null)
            writer.format("echo 0 > \"%s\"%n", protect(exitCodePath, QUOTED_SPECIAL));
        if (donePath != null)
            writer.format("touch \"%s\"%n", protect(donePath, QUOTED_SPECIAL));

        writer.close();

        // Set the file as executable
        runFile.setExecutable(true, false);
    }

    @Override
    final public XPMProcess start() throws LaunchException, IOException {
        // First generate the run file
        try {
            generateRunFile();
        } catch (Exception e) {
            throw new LaunchException(e);
        }

        return doStart();
    }

    /**
     * Called by {@linkplain sf.net.experimaestro.connectors.XPMProcessBuilder#start()} after the script file has been generated and pipes
     * handled
     */
    protected abstract XPMProcess doStart() throws LaunchException, IOException;

    @Override
    public void removeLock(FileObject lockFile) {
        lockFiles.add(protect(connector.resolve(lockFile), SHELL_SPECIAL));
    }

    @Override
    public XPMProcessBuilder command(List<String> command) {
        // FIXME: Use redirection information
        String prefix = "";

        switch (input.type()) {
            case INHERIT:
                break;
            case READ:
                final String path = connector.resolve(input.file());
                prefix = String.format("cat \"%s\" | ", protect(path, QUOTED_SPECIAL));
                break;
            default:
                throw new UnsupportedOperationException("Unsupported input redirection type: " + input.type());

        }

        commands.add(prefix + CommandLineTask.getCommandLine(command));
        error = output = input = Redirect.INHERIT;
        return this;
    }


    @Override
    public void exitCodeFile(FileObject exitCodeFile) {
        exitCodePath = connector.resolve(exitCodeFile);
    }

    @Override
    public void doneFile(FileObject doneFile) {
        donePath = connector.resolve(doneFile);
    }
}
