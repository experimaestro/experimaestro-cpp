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
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemException;
import sf.net.experimaestro.exceptions.LaunchException;
import sf.net.experimaestro.locks.FileLock;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.locks.UnlockableException;
import sf.net.experimaestro.scheduler.Job;
import sf.net.experimaestro.scheduler.ResourceLocator;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.utils.ProcessUtils;
import sf.net.experimaestro.utils.log.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.util.Map;

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
    public XPMProcessBuilder processBuilder() {
        return new ProcessBuilder();
    }


    @Override
    protected FileSystem doGetFileSystem() throws FileSystemException {
        return Scheduler.getVFSManager().resolveFile("file://").getFileSystem();
    }

    @Override
    public Lock createLockFile(String path) throws UnlockableException {
        return new FileLock(path);
    }

    @Override
    public String getHostName() {
        return "localhost";
    }

    public static LocalhostConnector getInstance() {
        return singleton;
    }


    public static ResourceLocator getLocator(URI uri) {
        return new ResourceLocator(singleton, uri.getPath());
    }

    @Override
    public XPMProcessBuilder processBuilder(SingleHostConnector connector) {
        return new ProcessBuilder();
    }


    @Persistent
    private static class LocalProcess extends XPMProcess {
        /**
         * The running process: if we have it, easier to monitor
         */
        transient private Process process;
        transient private Thread destroyThread;

        public LocalProcess() {
        }

        // Check on Windows:
        // http://stackoverflow.com/questions/2318220/how-to-programmatically-detect-if-a-process-is-running-with-java-under-windows

        public LocalProcess(Job job, Process process, boolean detach) {
            super(LocalhostConnector.getInstance(), String.valueOf(ProcessUtils.getPID(process)), job, true);
            this.process = process;
            if (detach) {
                destroyThread = new Thread() {
                    @Override
                    public void run() {
                        LocalProcess.this.destroy();
                    }
                };
                Runtime.getRuntime().addShutdownHook(destroyThread);
            }
        }

        @Override
        public boolean isRunning() throws Exception {
            if (process != null)
                return ProcessUtils.isRunning(process);

            return singleton.resolveFile(job.getLocator().getPath() + Job.LOCK_EXTENSION).exists();
        }

        @Override
        public int exitValue() {
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
            final int exitCode = process.waitFor();
            return exitCode;
        }

        @Override
        public void destroy() {
            if (process != null) {
                // TODO: send a signal first?
                process.destroy();

                if (destroyThread != null)
                    try {
                      Runtime.getRuntime().removeShutdownHook(destroyThread);
                    } catch(IllegalStateException e) {/* Ignore */}
                process = null;
            }

            // TODO: finish the implementation
            throw new NotImplementedException();
        }


    }

    /**
     * Localhost process builder
     */
    static private class ProcessBuilder extends XPMProcessBuilder {

        static private File convert(FileObject file) throws FileSystemException {
            URL url = file.getURL();
            if (url.getProtocol().contentEquals("file"))
                return new File(url.getPath());
            throw new FileSystemException("Cannot convert file for redirection on localhost", file);
        }

        static private java.lang.ProcessBuilder.Redirect convert(Redirect redirect) throws FileSystemException {
            if (redirect == null) redirect = Redirect.INHERIT;

            switch(redirect.type()) {
                case PIPE:
                    return java.lang.ProcessBuilder.Redirect.PIPE;
                case INHERIT:
                    return java.lang.ProcessBuilder.Redirect.INHERIT;
                case WRITE:
                    return java.lang.ProcessBuilder.Redirect.to(convert(redirect.file()));
                case APPEND:
                    return java.lang.ProcessBuilder.Redirect.appendTo(convert(redirect.file()));
                case READ:
                    return java.lang.ProcessBuilder.Redirect.from(convert(redirect.file()));
            }
            throw new AssertionError("Should not be here - enum not handled: " + redirect.type());
        }

        @Override
        public XPMProcess start() throws LaunchException, IOException {
            java.lang.ProcessBuilder builder = new java.lang.ProcessBuilder();

            // Set the environment
            Map<String,String> environment = builder.environment();
            if (environment() != null)
                for(Map.Entry<String,String> entry: environment().entrySet())
                    environment.put(entry.getKey(), entry.getValue());

            builder.redirectError(convert(error));
            builder.redirectOutput(convert(output));
            builder.redirectInput(convert(input));

            builder.command(command());

            final Process process = builder.start();
            LOGGER.info("Started local job with command [%s]", command().get(0));

            return new LocalProcess(job, process, detach());
        }
    }
}
