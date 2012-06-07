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

import com.sleepycat.persist.model.Persistent;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.utils.log.Logger;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 7/6/12
 */
@Persistent
public class LocalhostConnector implements Connector {
    static final private Logger LOGGER = Logger.getLogger();

    @Override
    public PrintWriter printWriter(String identifier) throws Exception {
        return new PrintWriter(new File(identifier));
    }

    @Override
    public int exec(String[] command, String[] envp, File workingDirectory, ArrayList<Lock> locks) throws Exception {
        Process p = null;
        try {

            p = Runtime.getRuntime().exec(command, envp, workingDirectory);

            // Changing the ownership of the different logs
            final int pid = sf.net.experimaestro.utils.PID.getPID(p);
            for (Lock lock : locks) {
                lock.changeOwnership(pid);
            }

            synchronized (p) {
                LOGGER.info("Waiting for the process (PID %d) to end", pid);
                int code = -1;
                try {
                    code = p.waitFor();
                } catch(InterruptedException e) {
                    LOGGER.warn("Task has been interrupted");
                }

                if (code != 0)
                    throw new RuntimeException(
                            "Process ended with errors (code " + code + ")");

                // Everything went well
                LOGGER.info("Process (PID %d) ended without error", pid);
                return code;
            }
        } finally {
            if (p != null) {
                p.getInputStream().close();
                p.getOutputStream().close();
                p.getErrorStream().close();
            }
        }
    }
}
