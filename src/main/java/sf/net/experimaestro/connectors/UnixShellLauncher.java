/*
 *
 *  This file is part of experimaestro.
 *  Copyright (c) 2012 B. Piwowarski <benjamin@bpiwowar.net>
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

package sf.net.experimaestro.connectors;

import com.sleepycat.persist.model.Persistent;

/**
 * Base class for all the launchers that are based on writing UNIX shell scripts
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 13/6/12
 */
@Persistent
public abstract class UnixShellLauncher implements Launcher {

    public UnixShellLauncher() {
        super();
    }



    static public abstract class ProcessBuilder extends XPMProcessBuilder {
        private String shPath = "/bin/bash";
        private SingleHostConnector connector;

        public ProcessBuilder(SingleHostConnector connector) {
            this.connector = connector;
        }

//        /**
//         * Generates a run file for a given command line task
//         *
//         * @param connector The connector
//         */
//        protected void generateRunFile(SingleHostConnector connector) throws Exception {
//            // Write file
//            final String path = task.getLocator().getPath();
//
//            final String quotedPath = "\"" + CommandLineTask.protect(path, "\"") + "\"";
//            final String runId = String.format("%s.run", path);
//            final FileObject runFile = connector.resolveFile(runId);
//            PrintWriter writer = new PrintWriter(runFile.getContent().getOutputStream());
//
//            writer.format("#!%s%n", shPath);
//
//            writer.format("# Experimaestro generated task: %s%n", path);
//            writer.println();
//            if (task.envp != null) {
//                for (String env : task.envp)
//                    writer.println(env);
//                writer.println();
//            }
//            if (task.workingDirectory != null) {
//                writer.format("cd %s%n", quotedPath);
//            }
//
//            writer.format("%n# Set traps to remove locks when exiting%n%n");
//            writer.format("trap cleanup EXIT%n");
//            writer.format("cleanup() {%n");
//            writer.format("  rm -f %s.lock;%n", quotedPath);
//            writer.format("}%n%n");
//
//
//            // Write the command
//            writer.println(CommandLineTask.getCommandLine(task.getCommand()));
//            writer.format("%n%n");
//
//            writer.format("# Creates a output files %n");
//            writer.format("code=$?%n", quotedPath);
//            writer.format("echo $code > %s.code%n", quotedPath);
//            writer.format("test $code -eq 0 && touch %s.done%n", quotedPath);
//
//            writer.close();
//
//            // Set the file as executable
//            runFile.setExecutable(true, false);
//        }
    }
}
