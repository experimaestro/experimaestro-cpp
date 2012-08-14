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

import com.sleepycat.persist.model.Persistent;
import org.apache.commons.lang.NotImplementedException;
import sf.net.experimaestro.exceptions.LaunchException;

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

    public ShLauncher() {
        super();
    }


    @Override
    public XPMProcessBuilder processBuilder(SingleHostConnector connector) {
        return new ProcessBuilder(connector);
    }

    public class ProcessBuilder extends UnixShellLauncher.ProcessBuilder {

        public ProcessBuilder(SingleHostConnector connector) {
            super(connector);
        }

        @Override
        public XPMProcess start() throws LaunchException {
               throw new NotImplementedException();
        }
    }

}
