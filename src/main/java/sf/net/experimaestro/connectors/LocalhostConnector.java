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
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemException;
import sf.net.experimaestro.locks.FileLock;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.locks.UnlockableException;
import sf.net.experimaestro.scheduler.Job;
import sf.net.experimaestro.scheduler.ResourceLocator;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.utils.ProcessUtils;
import sf.net.experimaestro.utils.log.Logger;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

/**
 * A local host connector provides access to the current machine
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date June 2012
 */
@Persistent
public class LocalhostConnector extends SingleHostConnector {
    static final private Logger LOGGER = Logger.getLogger();

    static private LocalhostConnector singleton = new LocalhostConnector();

    public LocalhostConnector() {
        super("local:");
    }

    @Override
    protected XPMProcessBuilder processBuilder() {
        return new LocalProcessBuilder();
    }


    @Override
    protected FileSystem doGetFileSystem() throws FileSystemException {
        return Scheduler.getVFSManager().resolveFile("file://").getFileSystem();
    }

    @Override
    public Lock createLockFile(String path) throws UnlockableException {
        return new FileLock(path);
    }

    public static Connector getInstance() {
        return singleton;
    }


    public static ResourceLocator getLocator(URI uri) {
        return new ResourceLocator(singleton, uri.getPath());
    }




    @Persistent
    private class LocalProcess extends XPMProcess {
        /**
         * The running process: if we have it, easier to monitor
         */
        transient private Process process;

        public LocalProcess() {
        }

        // Check on Windows:
        // http://stackoverflow.com/questions/2318220/how-to-programmatically-detect-if-a-process-is-running-with-java-under-windows

        public LocalProcess(Job job, Process process) {
            super(String.valueOf(ProcessUtils.getPID(process)), job, true);
            this.process = process;
        }


        @Override
        public boolean isRunning() throws Exception {
            if (process != null)
                return ProcessUtils.isRunning(process);

            return singleton.resolveFile(job.getLocator().getPath() + Job.LOCK_EXTENSION).exists();
        }

        @Override
        int exitValue() throws Exception {
            // Try the easy way
            if (process != null)
                return process.exitValue();

            return super.exitValue();
        }


        @Override
        public OutputStream getOutputStream() {
            return process == null ? null : process.getOutputStream();
        }

        @Override
        public InputStream getInputStream() {
            return process == null ? null : process.getInputStream();
        }

        @Override
        public InputStream getErrorStream() {
            return process == null ? null : process.getErrorStream();
        }

        @Override
        public int waitFor() throws InterruptedException {
            return process.waitFor();
        }

        @Override
        public boolean destroy() {
            if (process != null) {
                // TODO: send a signal first?
                process.destroy();
                return true;
            }

            // TODO: finish the implementation
            throw new NotImplementedException();
        }


    }

    private class LocalProcessBuilder extends XPMProcessBuilder {
        @Override
        public XPMProcess start() {
            throw new NotImplementedException();
        }
    }
}
