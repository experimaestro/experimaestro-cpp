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
import org.apache.commons.vfs2.FileObject;
import sf.net.experimaestro.exceptions.LaunchException;

import java.io.IOException;

/**
 * Runs a command using *SH (bash, sh)
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 12/6/12
 */
@Persistent
public class ShLauncher implements Launcher {
    /**
     * Path to the shell
     */
    String shellCommand = "/bin/bash";

    public ShLauncher() {
        super();
    }


    @Override
    public XPMProcessBuilder processBuilder(SingleHostConnector connector) {
        return new ProcessBuilder(null, connector);
    }

    @Override
    public XPMScriptProcessBuilder scriptProcessBuilder(SingleHostConnector connector, FileObject scriptFile) {
        return new ProcessBuilder(scriptFile, connector);
    }

    static public class ProcessBuilder extends UnixProcessBuilder {

        public ProcessBuilder(FileObject file, SingleHostConnector connector) {
            super(file, connector);
        }

        @Override
        public XPMProcess doStart() throws LaunchException, IOException {

            // Start the process
            final XPMProcessBuilder builder = connector.processBuilder();
            builder.command(protect(path, SHELL_SPECIAL));

            builder.detach(detach);
            builder.redirectOutput(output);
            builder.redirectError(error);

            return builder.start();
        }
    }

}
