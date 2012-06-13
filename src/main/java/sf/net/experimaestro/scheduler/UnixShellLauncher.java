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

package sf.net.experimaestro.scheduler;

import bpiwowar.argparser.utils.Formatter;
import bpiwowar.argparser.utils.Output;
import com.sleepycat.persist.model.Persistent;
import sf.net.experimaestro.utils.arrays.ListAdaptator;

import java.io.PrintWriter;

/**
 * Base class for all the launchers that are based on unix shell (sh) scripts
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 13/6/12
 */
@Persistent
public abstract class UnixShellLauncher extends Launcher {

    /**
     * Generates a run file for a given command line task
     * @param task The task at hand
     * @return
     * @throws Exception
     */
    protected void generateRunFile(CommandLineTask task) throws Exception {
        // Write command
        Connector connector = task.getConnector();
        final String path = task.identifier.path;
        final String quotedPath = "\"" + CommandLineTask.protect(path, "\"") + "\"";
        final String runId = String.format("%s.run", path);
        PrintWriter writer = connector.printWriter(runId);


        writer.format("# Experimaestro generated task: %s%n", path);
        writer.println();
        if (task.envp != null) {
            for (String env : task.envp)
                writer.println(env);
            writer.println();
        }
        if (task.workingDirectory != null) {
            writer.format("cd %s%n", quotedPath);
        }

        writer.println("trap cleanup EXIT");
        writer.format("cleanup() { rm -f %s.lock; }%n%n", quotedPath);


        // Write the command
        writer.println(Output.toString(" ", ListAdaptator.create(task.command), new Formatter<String>() {
            public String format(String t) {
                return CommandLineTask.protect(t, " \"'");
            }
        }));

        writer.format("test $? -eq 0 && touch %s.done", quotedPath);

        writer.close();
    }



    @Override
    public ResourceState getState(CommandLineTask task) throws Exception {
        final String path = task.identifier.path;
        final Connector connector = task.getConnector();

        // First, is it running?
        if (connector.fileExists(path + Job.LOCK_EXTENSION))
            return ResourceState.RUNNING;

        // Then, is it done?
        if (connector.fileExists(path + Job.DONE_EXTENSION))
            return ResourceState.DONE;

        // Hmm, we don't know
        return null;
    }

}
