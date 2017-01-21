package net.bpiwowar.xpm.connectors;

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
import com.pastdev.jsch.nio.file.UnixSshFileSystem;
import com.pastdev.jsch.nio.file.UnixSshSftpHybridFileSystemProvider;
import net.bpiwowar.xpm.commands.Redirect;
import net.bpiwowar.xpm.exceptions.LaunchException;
import net.bpiwowar.xpm.exceptions.LockException;
import net.bpiwowar.xpm.locks.FileLock;
import net.bpiwowar.xpm.locks.Lock;
import net.bpiwowar.xpm.manager.scripting.Expose;
import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.manager.scripting.Context;
import net.bpiwowar.xpm.scheduler.CommandLineTask;
import net.bpiwowar.xpm.scheduler.Scheduler;
import net.bpiwowar.xpm.scheduler.TypeIdentifier;
import net.bpiwowar.xpm.utils.log.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static net.bpiwowar.xpm.utils.PathUtils.QUOTED_SPECIAL;
import static net.bpiwowar.xpm.utils.PathUtils.protect;

/**
 * SSH connector backed up by jsch-nio
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
@Exposed
@TypeIdentifier("SSH")
public class SSHConnector extends SingleHostConnector {
    static final private Logger LOGGER = Logger.getLogger();
    public static final String SSH_UNIX_SCHEME = UnixSshSftpHybridFileSystemProvider.SCHEME_SSH_SFTP_HYBRID_UNIX;

    /**
     * Static map to sessions
     * This is necessary since the SSHConnector object can be serialized (within a resource)
     */
    static private HashMap<SSHConnector, SSHSession> sessions = new HashMap<>();

    /**
     * Temporary path on host
     */
    public String temporaryPath = "/tmp";

    /**
     * Base path to resolve paths
     */
    private String basePath = "/";

    private SSHOptions options = new SSHOptions();

    /**
     * Cached SSH session
     */
    transient private SSHSession _session;

    /**
     * The file system
     */
    transient private UnixSshFileSystem filesystem;

    /**
     * Used for serialization
     */
    public SSHConnector(Long id, String identifier) {
        super(identifier);
        setId(id);
    }

    @Expose
    public SSHConnector(String identifier, String uri, ConnectorOptions options) throws URISyntaxException {
        this(identifier, new URI(uri), options);
    }

    public SSHConnector(String identifier, URI uri, ConnectorOptions options) {
        this(identifier, uri.getUserInfo(), uri.getHost(), uri.getPort(), options);
        this.basePath = uri.getPath();
        if (this.basePath.isEmpty()) {
            basePath = "/";
        }
        dataLoaded = true;
    }

    @Override
    protected void replaceBy(Connector connector) {
        final SSHConnector sshConnector = (SSHConnector) connector;
        this.options = sshConnector.options();
        this.basePath = sshConnector.basePath;
        this.temporaryPath = sshConnector.temporaryPath;
        this.dataLoaded = sshConnector.dataLoaded;
        if (_session != null && !_session.isConnected()) {
            synchronized (sessions) {
                this._session = null;
                sessions.remove(this);
            }
        }
    }

    /**
     * @param identifier The identifier
     * @param username
     * @param hostname
     * @param port
     * @param options
     */
    public SSHConnector(String identifier, String username, String hostname, int port, ConnectorOptions options) {
        super(identifier);
        dataLoaded = true;
        this.options = options != null ? ((SSHOptions) options).copy() : new SSHOptions();

        this.options().setHostName(hostname);
        this.options().setUserName(username);
        if (port > 0) {
            this.options().setPort(port);
        }
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
        loadData();
        return options().getHostName();
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

        loadData();

        try {
            URI uri = new URI(SSH_UNIX_SCHEME + "://" + options().getUserName() + "@" + options().getHostName() + ":" + options().getPort() + basePath);

            try {
                return FileSystems.getFileSystem(uri);
            } catch (FileSystemNotFoundException e) {
                // just ignore
            }

            final DefaultSessionFactory sessionFactory = options().getSessionFactory();

            Map<String, Object> environment = new HashMap<>();
            environment.put("defaultSessionFactory", sessionFactory);

            try {
                filesystem = (UnixSshFileSystem) FileSystems.newFileSystem(uri, environment);
            } catch(RuntimeException e) {
                filesystem = (UnixSshFileSystem) FileSystems.getFileSystem(uri);
            }
            return filesystem;
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    @Override
    protected boolean contains(FileSystem fileSystem) throws FileSystemException {
        try {
            if (!(fileSystem instanceof UnixSshFileSystem)) return false;
            final UnixSshFileSystem ours = (UnixSshFileSystem) doGetFileSystem();

            final URI ourUri = ours.getUri();
            final URI theirUri = ((UnixSshFileSystem) fileSystem).getUri();

            return ourUri.equals(theirUri);
        } catch (IOException e) {
            return false;
        }
    }


    ChannelSftp newSftpChannel() throws JSchException, IOException {
        return (ChannelSftp) getSession().openChannel("sftp");
    }

    ChannelExec newExecChannel() throws JSchException, IOException {
        return (ChannelExec) getSession().openChannel("exec");
    }

    /**
     * Get the session (creates it if necessary)
     */
    private Session getSession() throws JSchException, IOException {
        synchronized (this) {
            if (_session == null) {
                LOGGER.info("Creating new session for SSH to %s", this.getHostName());
                _session = this.identifier != null ? sessions.get(this) : null;
                if (_session == null) {
                    _session = new SSHSession();
                    if (this.identifier != null) {
                        sessions.put(this, _session);
                    }
                }
            }

            // If we are not connected, do it now
            if (!_session.session.isConnected()) {
                LOGGER.info("Opening dropped SSH connection to %s", this.getHostName());
                _session.session.connect();
            }
        }

        // Returns
        return _session.session;
    }

    @Override
    public Path defaultTemporaryPath() {
        return Paths.get("/tmp");
    }

    /**
     * Connection options
     */
    SSHOptions options() {
        loadData();
        return options;
    }

    /**
     * an SSH session
     */
    class SSHSession {
        public Session session;

        SSHSession() throws JSchException, IOException {
            init();
        }

        void init() throws JSchException, IOException {
            session = options().getSessionFactory().newSession();
        }

        boolean isConnected() {
            return session != null && session.isConnected();
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
        public XPMProcess start(boolean fake) throws LaunchException {
            if (fake) return null;
            final ChannelExec channel;
            try {
                channel = newExecChannel();
                StringBuilder commandBuilder = new StringBuilder();
                final String commandLine = CommandLineTask.getCommandLine(command());

                // Set the environment (do not use SSH since they are filtered)
                if (environment() != null) {
                    for (Map.Entry<String, String> x : environment().entrySet()) {
                        commandBuilder.append("export ");
                        commandBuilder.append(x.getKey());
                        commandBuilder.append("=\"");
                        commandBuilder.append(protect(x.getValue(), QUOTED_SPECIAL));
                        commandBuilder.append("\";");
                    }
                }

                commandBuilder.append(commandLine);

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
                LOGGER.info("Executing command [%s] with SSH connector (%s@%s)", command, options().getUserName(),
                        options().getHostName());
                channel.setCommand(command);
                channel.setPty(!detach());
                channel.connect();
            } catch (JSchException | IOException e) {
                throw new LaunchException(e);
            }

            return new SSHProcess(SSHConnector.this, job(), channel, detach());
        }
    }

    @Expose(optional = 1, optionalsAtStart = true)
    public String env(Launcher launcher, String key) throws IOException, LaunchException, InterruptedException {
        return execute(launcher, "echo", "$" + key);
    }

    private String execute(Launcher launcher, String... command) throws IOException, LaunchException, InterruptedException {
        if (launcher == null) {
            launcher = new DirectLauncher(Scheduler.get().getLocalhostConnector());
        }
        final Logger logger = Context.get().getMainLogger();
        AbstractProcessBuilder builder = launcher.processBuilder(null);
        builder.command(command);
        String returned = builder.execute(logger);
        return returned;
    }
}
