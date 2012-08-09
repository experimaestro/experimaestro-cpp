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
import org.apache.commons.vfs2.*;
import sf.net.experimaestro.locks.FileLock;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.locks.UnlockableException;
import sf.net.experimaestro.scheduler.*;
import sf.net.experimaestro.scheduler.Process;
import sf.net.experimaestro.utils.ProcessUtils;
import sf.net.experimaestro.utils.log.Logger;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;

/**
 * A local host connector provides access to the current machine
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date June 2012
 */
@Persistent
public class LocalhostConnector extends Connector {
    static final private Logger LOGGER = Logger.getLogger();

    public LocalhostConnector() {
        super("local:");
    }

    @Override
    public sf.net.experimaestro.scheduler.Process exec(final Job job, String command, ArrayList<Lock> locks, boolean detach, String stdoutPath, String stderrPath) throws Exception {
        LOGGER.info("Running command [%s]", command);
        return new LocalProcess(Runtime.getRuntime().exec(new String[]{command}), detach);
    }


    @Override
    public Lock createLockFile(String path) throws UnlockableException {
        return new FileLock(path);
    }

    @Override
    protected FileSystem doGetFileSystem() throws FileSystemException {
        return Scheduler.getVFSManager().resolveFile("file://").getFileSystem();
    }

    public static Connector getInstance() {
        return singleton;
    }

    static private LocalhostConnector singleton = new LocalhostConnector();

    public static Locator getLocator(URI uri) {
        return new Locator(singleton, uri.getPath());
    }


    /**
     * A local process
     */
    private static class LocalProcess extends Process {
        private final java.lang.Process process;

        public LocalProcess(java.lang.Process process, boolean detach) {
            this.process = process;
        }

        @Override
        public OutputStream getOutputStream() {
            return process.getOutputStream();
        }

        @Override
        public InputStream getInputStream() {
            return process.getInputStream();
        }

        @Override
        public InputStream getErrorStream() {
            return process.getErrorStream();
        }

        @Override
        public int waitFor() throws InterruptedException {
            return process.waitFor();
        }

        @Override
        public int exitValue() {
            return process.exitValue();
        }

        @Override
        public void destroy() {
            process.destroy();
        }

        protected void finalize() throws Throwable {
            destroy();
            super.finalize();
        }

        @Override
        public String getPID() {
            return String.valueOf(ProcessUtils.getPID(process));
        }

        @Override
        public boolean isRunning() {
            try {
                process.exitValue();
                return true;
            } catch (IllegalThreadStateException e) {
                return false;
            }
        }
    }


    @Persistent
    private class LocalhostJobMonitor extends XPMProcess {
        /**
         * The running process: if we have it, easier to monitor
         */
        transient private Process process;

        public LocalhostJobMonitor() {
        }

        // Check on Windows:
        // http://stackoverflow.com/questions/2318220/how-to-programmatically-detect-if-a-process-is-running-with-java-under-windows

        public LocalhostJobMonitor(Job job, Process process) {
            super(job, process, true);
            this.process = process;
        }


        @Override
        public boolean isRunning() throws Exception {
            if (process != null)
                return ProcessUtils.isRunning(process);

            return singleton.resolveFile(job.getLocator().getPath() + Job.LOCK_EXTENSION).exists();
        }

        @Override
        int getCode() throws Exception {
            // Try the easy way
            if (process != null)
            return process.exitValue();

            // If not done, returns -1
            if (singleton.resolveFile(job.getLocator().getPath() + Job.DONE_EXTENSION).exists())
                return -1;

            return 0;
        }
    }
}
