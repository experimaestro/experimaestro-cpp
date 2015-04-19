package sf.net.experimaestro.connectors;

/*
 * This file is part of experimaestro.
 * Copyright (c) 2014 B. Piwowarski <benjamin@bpiwowar.net>
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

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.FileSystemException;

import com.pastdev.jsch.DefaultSessionFactory;
import com.pastdev.jsch.file.SshFileSystem;
import com.pastdev.jsch.nio.file.UnixSshFileSystem;
import sf.net.experimaestro.exceptions.LaunchException;
import sf.net.experimaestro.exceptions.LockException;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.locks.FileLock;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.scheduler.CommandLineTask;
import sf.net.experimaestro.scheduler.Job;
import sf.net.experimaestro.utils.log.Logger;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static sf.net.experimaestro.connectors.UnixScriptProcessBuilder.protect;

/**
 * SSH connector backed up by commons VFS
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class SSHConnector extends SingleHostConnector {
    static final private Logger LOGGER = Logger.getLogger();

    static final int SSHD_DEFAULT_PORT = 22;

    public String temporaryPath = "/tmp";

    /**
     * Port
     */
    int port = SSHD_DEFAULT_PORT;
    /**
     * Static map to sessions
     * This is necessary since the SSHConnector object can be serialized (within a resource)
     */
    static private HashMap<SSHConnector, SSHSession> sessions = new HashMap<SSHConnector, SSHSession>();
    /**
     * Username
     */
    String username;
    /**
     * Hostname to connect to
     */
    String hostname;
    /**
     * Connection options
     */
    private SSHOptions options = new SSHOptions();
    /**
     * Cached SSH session
     */
    transient private SSHSession _session;

    /** The file system */
    transient private UnixSshFileSystem filesystem;

    /**
     * Used for serialization
     */
    private SSHConnector() {
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
    public AbstractProcessBuilder processBuilder() {
        return new SSHProcessBuilder();
    }

    public Lock createLockFile(final Path path, boolean wait) throws LockException {
        return new FileLock(path, wait);
    }

    @Override
    public String getHostName() {
        return hostname;
    }

    @Override
    protected Path getTemporaryDirectory() throws IOException {
        return getFileSystem().getPath(temporaryPath);
    }

    @Override
    public FileSystem doGetFileSystem() throws IOException {
        if (filesystem != null) {
            return filesystem;
        }

        try {
            URI uri = new URI( "ssh.unix://" + username + "@" + hostname + ":" + port + "/" );
            DefaultSessionFactory defaultSessionFactory = new DefaultSessionFactory(username, hostname, 22 );
            Map<String, Object> environment = new HashMap<>();
            environment.put( "defaultSessionFactory", defaultSessionFactory );

            filesystem = (UnixSshFileSystem) FileSystems.newFileSystem(uri, environment);
            return filesystem;
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    @Override
    protected boolean contains(FileSystem fileSystem) throws FileSystemException {
        throw new NotImplementedException();
    }

    @Override
    public XPMScriptProcessBuilder scriptProcessBuilder(Path scriptFile) throws IOException {
        return new UnixScriptProcessBuilder(scriptFile, this);
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

    private interface StreamSetter {
        public void setStream(OutputStream out, boolean dontClose);

        int streamNumber();
    }

    /**
     * An SSH process
     */
    public static class SSHProcess extends XPMProcess {

        transient private ChannelExec channel;

        private SSHProcess() {
        }

        public SSHProcess(SingleHostConnector connector, Job job, ChannelExec channel) {
            super(connector, null, job);
            this.channel = channel;
            startWaitProcess();
        }

        @Override
        public OutputStream getOutputStream() {
            try {
                return channel.getOutputStream();
            } catch (IOException e) {
                throw new XPMRuntimeException(e);
            }
        }

        @Override
        public InputStream getInputStream() {
            try {
                return channel.getInputStream();
            } catch (IOException e) {
                throw new XPMRuntimeException(e);
            }
        }

        @Override
        public InputStream getErrorStream() {
            try {
                return channel.getErrStream();
            } catch (IOException e) {
                throw new XPMRuntimeException(e);
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

        SSHSession() throws JSchException, FileSystemException {
            init();
        }

        void init() throws JSchException, FileSystemException {
//            session = createConnection(hostname, port, username.toCharArray(), null, options.getOptions());
            throw new NotImplementedException();
        }

    }

    public class SSHProcessBuilder extends AbstractProcessBuilder {

        private void setStream(StringBuilder commandBuilder, Redirect output, StreamSetter streamSetter) throws IOException {
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
            } catch (JSchException | IOException e) {
                throw new LaunchException(e);
            }

            return new SSHProcess(SSHConnector.this, job(), channel);
        }


    }
}
