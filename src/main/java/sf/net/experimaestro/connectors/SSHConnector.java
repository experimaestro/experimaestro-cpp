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

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.model.Persistent;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.provider.sftp.SftpClientFactory;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystem;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.exceptions.LaunchException;
import sf.net.experimaestro.exceptions.LockException;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.scheduler.CommandLineTask;
import sf.net.experimaestro.scheduler.Job;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.utils.log.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;

import static sf.net.experimaestro.connectors.UnixProcessBuilder.protect;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 7/6/12
 */
@Persistent
public class SSHConnector extends SingleHostConnector {

    static final private Logger LOGGER = Logger.getLogger();
    static final int SSHD_DEFAULT_PORT = 22;

    /**
     * Username
     */
    String username;

    /**
     * Hostname to connect to
     */
    String hostname;

    /**
     * Port
     */
    int port = SSHD_DEFAULT_PORT;

    /**
     * Connection options
     */
    private SSHOptions options = new SSHOptions();


    /**
     * Used for serialization
     */
    private SSHConnector() {
    }

    @Override
    public XPMProcessBuilder processBuilder() {
        return new SSHProcessBuilder();
    }

    /**
     * Construct from a username, hostname, port triplet
     *
     * @param username
     * @param hostname
     * @param port
     */
    public SSHConnector(String username, String hostname, int port) {
        super(String.format("ssh://%s@%s:%d", username, port, hostname));
        this.username = username;
        this.hostname = hostname;
        this.port = port < 0 ? SSHD_DEFAULT_PORT : port;
    }

    public SSHConnector(String username, String hostname) {
        this(username, hostname, SSHD_DEFAULT_PORT);
    }

    public Lock createLockFile(final String path) throws LockException {
        try {
            ChannelExec channel = newExecChannel();
            LOGGER.info("Creating SSH lock [%s]", path);
            channel.setCommand(String.format("%s \"%s\"", options.getLockFileCommand(), protect(path, "\"")));
            channel.setInputStream(null);
            channel.setErrStream(System.err, true);
            channel.setOutputStream(System.out, true);
            channel.connect();
            while (!channel.isClosed()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    LOGGER.error(e);
                }
            }
            channel.disconnect();
        } catch (JSchException e) {
            throw new LockException(e);
        } catch (FileSystemException e) {
            throw new LockException(e);
        }

        return new SSHLock(this, path);
    }

    @Override
    public String getHostName() {
        return hostname;
    }

    @Override
    protected FileObject getTemporaryDirectory() throws FileSystemException {
        // FIXME: hardcoded value
        return getFileSystem().resolveFile("/tmp");
    }

    @Override
    public FileSystem doGetFileSystem() throws FileSystemException {
        final FileSystem fileSystem = VFS.getManager()
                .resolveFile(String.format("sftp://%s@%s:%d/", username, hostname, port), options.getOptions()).getFileSystem();
        return fileSystem;
    }

    /**
     * @param username
     * @param hostname
     * @param port
     * @param options
     */
    public SSHConnector(String username, String hostname, int port, ConnectorOptions options) {
        super(String.format("ssh://%s:%d@%s", username, port, hostname));
        this.username = username;
        this.hostname = hostname;
        this.port = port > 0 ? port : SSHD_DEFAULT_PORT;
        if (options != null)
            this.options = (SSHOptions) options;
    }

    public SSHConnector(URI uri, ConnectorOptions options) {
        this(uri.getUserInfo(), uri.getHost(), uri.getPort(), options);
    }

    @Override
    public XPMScriptProcessBuilder scriptProcessBuilder(SingleHostConnector connector, FileObject scriptFile) {
        return new ShLauncher.ProcessBuilder(scriptFile, connector);
    }


    /**
     * An SSH process
     */
    @Persistent
    public static class SSHProcess extends XPMProcess {

        transient private ChannelExec channel;

        private SSHProcess() {
        }

        public SSHProcess(SingleHostConnector connector, Job job, ChannelExec channel) {
            super(connector, null, job, true);
            this.channel = channel;
        }

        @Override
        public OutputStream getOutputStream() {
            try {
                return channel.getOutputStream();
            } catch (IOException e) {
                throw new ExperimaestroRuntimeException(e);
            }
        }

        @Override
        public InputStream getInputStream() {
            try {
                return channel.getInputStream();
            } catch (IOException e) {
                throw new ExperimaestroRuntimeException(e);
            }
        }

        @Override
        public InputStream getErrorStream() {
            try {
                return channel.getErrStream();
            } catch (IOException e) {
                throw new ExperimaestroRuntimeException(e);
            }
        }

        @Override
        public int waitFor() throws InterruptedException {
            if (channel != null)
                return super.waitFor();

            while (channel.isConnected()) {
                Thread.sleep(1000);
            }
            return channel.getExitStatus();
        }

        @Override
        public int exitValue() {
            return channel.getExitStatus();
        }

        @Override
        public void destroy() {
            if (channel != null) {
                channel.disconnect();
                channel = null;
            }
        }

        protected void finalize() throws Throwable {
            destroy();
            super.finalize();
        }


        @Override
        public boolean isRunning() {
            return channel.isConnected();
        }
    }


    /**
     * an SSH session
     */
    class SSHSession {
        public Session session;
        public SftpFileSystem filesystem;

        SSHSession() throws JSchException, FileSystemException {
            init();
        }

        void init() throws JSchException, FileSystemException {
            session = SftpClientFactory.createConnection(hostname, port, username.toCharArray(), null, options.getOptions());
        }

        SftpFileSystem getFileSystem() {
            return filesystem;
        }
    }

    ChannelSftp newSftpChannel() throws JSchException, FileSystemException {
        return (ChannelSftp) getSession().openChannel("sftp");
    }

    ChannelExec newExecChannel() throws JSchException, FileSystemException {
        return (ChannelExec) getSession().openChannel("exec");
    }

    /**
     * Get the session (creates it if necessary)
     */
    private Session getSession() throws JSchException, FileSystemException {
        if (_session == null) {
            _session = sessions.get(this);
            if (_session == null) {
                sessions.put(this, _session = new SSHSession());
            }
        }

        // If we are not connected, do it now
        if (!_session.session.isConnected())
            _session.session.connect();

        // Returns
        return _session.session;
    }

    /**
     * Static map to sessions
     * This is necessary since the SSHConnector object can be serialized (within a resource)
     */
    static private HashMap<SSHConnector, SSHSession> sessions = new HashMap<SSHConnector, SSHSession>();

    /**
     * Cached SSH session
     */
    transient private SSHSession _session;

    /**
     * A lock
     */
    @Persistent
    static public class SSHLock implements Lock {
        private String path;

        private String connectorId;

        transient private Connector connector;

        private SSHLock() {
        }

        public SSHLock(Connector connector, String path) {
            this.connector = connector;
            this.connectorId = connector.getIdentifier();
            this.path = path;
        }

        @Override
        public void close() {
            try {
                ChannelSftp sftp = ((SSHConnector) connector.getMainConnector()).newSftpChannel();
                LOGGER.info("Disposing of SSH lock [%s]", path);
                sftp.connect();
                sftp.chmod(644, path);
                sftp.rm(path);
                sftp.disconnect();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                new RuntimeException(e);
            }
        }

        @Override
        public void changeOwnership(String pid) {
        }

        @Override
        public void init(Scheduler scheduler) throws DatabaseException {
            connector = scheduler.getConnector(connectorId);
        }
    }


    private interface StreamSetter {
        public void setStream(OutputStream out, boolean dontClose);

        int streamNumber();
    }


    public class SSHProcessBuilder extends XPMProcessBuilder {

        private void setStream(StringBuilder commandBuilder, Redirect output, StreamSetter streamSetter) {
            final Redirect.Type type = output == null ? Redirect.Type.INHERIT : output.type();

            switch (type) {
                case PIPE:
                    streamSetter.setStream(null, false);
                    break;
                case INHERIT:
                    streamSetter.setStream(System.err, true);
                    break;
                case WRITE:
                    commandBuilder.append(String.format(" %d> \"%s\"", streamSetter.streamNumber(), protect(resolve(output.file()), "\"")));
                    break;
                case APPEND:
                    commandBuilder.append(String.format(" %d>> \"%s\"", streamSetter.streamNumber(), protect(resolve(output.file()), "\"")));
                    break;
                default:
                    throw new RuntimeException("Unhandled redirection type: " + type);
            }
        }

        @Override
        public XPMProcess start() throws LaunchException {
            final ChannelExec channel;
            try {
                channel = newExecChannel();
                StringBuilder commandBuilder = new StringBuilder();
                commandBuilder.append(CommandLineTask.getCommandLine(command()));

                // Set default
                channel.setOutputStream(System.out, true);
                channel.setErrStream(System.err, true);
                channel.setInputStream(null);


                setStream(commandBuilder, output, new StreamSetter() {
                    @Override
                    public void setStream(OutputStream out, boolean dontClose) {
                        channel.setOutputStream(out, dontClose);
                    }

                    @Override
                    public int streamNumber() {
                        return 1;
                    }
                });

                setStream(commandBuilder, error, new StreamSetter() {
                    @Override
                    public void setStream(OutputStream out, boolean dontClose) {
                        channel.setErrStream(out, dontClose);
                    }

                    @Override
                    public int streamNumber() {
                        return 2;
                    }
                });

                final Redirect.Type inputType = input == null ? Redirect.Type.INHERIT : input.type();
                switch (inputType) {
                    case INHERIT:
                    case PIPE:
                        break;
                    case READ:
                        commandBuilder.append(String.format("< \"%s\"", protect(resolve(input.file()), "\"")));
                    default:
                        throw new RuntimeException("Unhandled redirection type: " + inputType);
                }


                String command = commandBuilder.toString();
                LOGGER.info("Executing command [%s] with SSH connector (%s@%s)", command, username, hostname);
                channel.setCommand(command);
                channel.setPty(!detach());

                channel.connect();
            } catch (JSchException | FileSystemException e) {
                throw new LaunchException(e);
            }

            return new SSHProcess(SSHConnector.this, job(), channel);
        }


    }
}
