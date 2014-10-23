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

import sf.net.experimaestro.exceptions.LaunchException;
import sf.net.experimaestro.exceptions.LockException;
import sf.net.experimaestro.locks.FileLock;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.scheduler.Job;
import sf.net.experimaestro.utils.ProcessUtils;
import sf.net.experimaestro.utils.log.Logger;

import javax.persistence.Entity;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.Map;

import static java.lang.String.format;

/**
 * A local host connector provides access to the current machine
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Entity
public class LocalhostConnector extends SingleHostConnector {
    static final private Logger LOGGER = Logger.getLogger();
    private static final String TMPDIR = System.getProperty("java.io.tmpdir").toString();
    static private LocalhostConnector singleton = new LocalhostConnector();

    public LocalhostConnector() {
        super("file://");
    }

    @Override
    public Path resolve(String path) {
        return new File(path).toPath();
    }

    public static LocalhostConnector getInstance() {
        return singleton;
    }


    @Override
    public AbstractProcessBuilder processBuilder() {
        return new ProcessBuilder();
    }

    @Override
    protected FileSystem doGetFileSystem() throws FileSystemException {
        return FileSystems.getDefault();
    }

    @Override
    protected boolean contains(FileSystem fileSystem) throws FileSystemException {
        return fileSystem.equals(getFileSystem());
    }

    @Override
    public Lock createLockFile(Path path, boolean wait) throws LockException {
        return new FileLock(path, wait);
    }

    @Override
    public String getHostName() {
        return "localhost";
    }

    @Override
    protected Path getTemporaryDirectory() throws FileSystemException {
        return getFileSystem().getPath(TMPDIR);
    }

    @Override
    public XPMScriptProcessBuilder scriptProcessBuilder(Path scriptFile) throws FileSystemException {
        return new UnixScriptProcessBuilder(scriptFile, this);
    }


    /**
     * Wrapper for a local thread
     */
    @Entity
    private static class LocalProcess extends XPMProcess {
        /**
         * The running process: if we have it, easier to monitor
         */
        transient private Process process;
        transient private Thread destroyThread;

        /**
         * Stores the exit value
         */
        transient private Integer exitValue;

        @SuppressWarnings("unused")
        public LocalProcess() {
        }

        // Check on Windows:
        // http://stackoverflow.com/questions/2318220/how-to-programmatically-detect-if-a-process-is-running-with-java-under-windows

        public LocalProcess(Job job, Process process, boolean detach) {
            super(LocalhostConnector.getInstance(), String.valueOf(ProcessUtils.getPID(process)), job);
            this.process = process;
            if (!detach) {
                // If we need to destroy this process
                destroyThread = new Thread() {
                    @Override
                    public void run() {
                        try {
                            LocalProcess.this.destroy();
                        } catch (FileSystemException e) {
                            LOGGER.error("Process %s could not be destroyed", LocalProcess.this);
                        }
                    }
                };
                Runtime.getRuntime().addShutdownHook(destroyThread);
            }

            startWaitProcess();
        }

        @Override
        public boolean isRunning() throws Exception {
            if (process != null)
                return ProcessUtils.isRunning(process);

            return super.isRunning();
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
            if (process != null) {
                exitValue = process.waitFor();
                return exitValue;
            }
            if (exitValue != null) {
                return exitValue;
            }
            return super.waitFor();
        }

        @Override
        public void destroy() throws FileSystemException {
            if (exitValue != null) {
                return;
            }

            if (process != null) {
                LOGGER.info("Killing job [%s] with PID [%s]", getJob(), getPID());
                // TODO: send a signal first?
                process.destroy();

                if (destroyThread != null)
                    try {
                        Runtime.getRuntime().removeShutdownHook(destroyThread);
                    } catch (IllegalStateException e) {/* Ignore */}
                process = null;
            } else {
                LOGGER.info("Process was not started by server: killing it externally with PID %s", getPID());
                AbstractProcessBuilder killingProcessBuilder = getInstance().processBuilder();
                killingProcessBuilder.command("kill", getPID()).detach(false);
                try {
                    final XPMProcess killingProcess = killingProcessBuilder.detach(false).start();
                    int code = killingProcess.waitFor();
                    if (code != 0)
                        throw new RuntimeException(format("Could not kill local process [%s]: error code %d", getPID(), code));
                } catch (LaunchException | IOException | InterruptedException e) {
                    throw new RuntimeException(format("Could not kill local process [%s]", getPID()), e);
                }
            }

            // TODO: finish the implementation ???
//            throw new NotImplementedException();
        }


    }

    /**
     * Localhost process builder
     */
    static private class ProcessBuilder extends AbstractProcessBuilder {

        static private File convert(Path file) throws FileSystemException {
            return file.toAbsolutePath().toFile();
        }

        static private java.lang.ProcessBuilder.Redirect convert(Redirect redirect) throws FileSystemException {
            if (redirect == null) redirect = Redirect.INHERIT;

            switch (redirect.type()) {
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
            Map<String, String> builderEnvironment = builder.environment();

            if (this.environment() != null)
                for (Map.Entry<String, String> entry : this.environment().entrySet()) {
                    builderEnvironment.put(entry.getKey(), entry.getValue());
                }

            if (LOGGER.isDebugEnabled()) {
                for (Map.Entry<String, String> entry : builderEnvironment.entrySet()) {
                    LOGGER.debug("[*] %s=%s", entry.getKey(), entry.getValue());
                }
            }

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

