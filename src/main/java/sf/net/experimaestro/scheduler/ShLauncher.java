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

import java.util.ArrayList;

/**
 * Runs a command using *SH (bash, sh)
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 12/6/12
 */
@Persistent
public class ShLauncher extends UnixShellLauncher {
    /**
     * Path to the shell
     */
    String shellCommand = "/bin/bash";

    @Override
    public void launch(CommandLineTask task, ArrayList<Lock> locks) throws Exception {
        generateRunFile(task);

        final String path = task.identifier.path;
        final String command = String.format("%s %s.run > %2s.out 2> %2$s.err",
                shellCommand, CommandLineTask.protect(path, " "));
        task.getConnector().exec(command, locks);
    }


}
