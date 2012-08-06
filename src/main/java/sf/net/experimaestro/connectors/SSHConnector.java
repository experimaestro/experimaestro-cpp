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

import com.jcraft.jsch.*;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.utilint.NotImplementedYetException;
import com.sleepycat.persist.model.Persistent;
import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.provider.sftp.SftpClientFactory;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystem;
import sf.net.experimaestro.exceptions.ExperimaestroException;
import sf.net.experimaestro.locks.Lock;
import sf.net.experimaestro.locks.UnlockableException;
import sf.net.experimaestro.scheduler.*;
import sf.net.experimaestro.scheduler.Process;
import sf.net.experimaestro.utils.log.Logger;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 7/6/12
 */
@Persistent
public class SSHConnector extends Connector {

    static final private Logger LOGGER = Logger.getLogger();

    /** Username */
    String username;

    /** Hostname to connect to */
    String hostname;

    /** Port */
    int port = 22;

    /** Connection options */
    private SSHOptions options;

    /** Used for serialization */
    public SSHConnector() {

    }

    @Override
    public Lock createLockFile(final String path) throws UnlockableException {
        try {
            ChannelExec channel = newExecChannel();
            LOGGER.info("Creating SSH lock [%s]", path);
            channel.setCommand(String.format("lockfile \"%s\"", CommandLineTask.protect(path, "\"")));
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
            throw new UnlockableException(e);
        } catch (FileSystemException e) {
            throw new UnlockableException(e);
        }

        return new SSHLock(this, path);
    }

    @Override
    public FileSystem doGetFileSystem() throws FileSystemException {
        return VFS.getManager().resolveFile(String.format("sftp://%s@%s", username, hostname)).getFileSystem();
    }

    public SSHConnector(String username, String hostname, ConnectorOptions options) {
        super(String.format("ssh://%s@%s", username, hostname));
        this.username = username;
        this.hostname = hostname;
        this.options = (SSHOptions)options;
    }

    public SSHConnector(URI uri, ConnectorOptions options) {
        this(uri.getUserInfo(), uri.getHost(), options);
    }


    @Override
    public sf.net.experimaestro.scheduler.Process exec(Job job, String command, ArrayList<Lock> locks, boolean detach, String stdoutPath, String stderrPath) throws Exception {
        final ChannelExec channel = newExecChannel();
//        command = String.format("/bin/sh %s", command);

        if (stdoutPath != null)
            command = String.format("%s > \"%s\"", command, CommandLineTask.protect(stdoutPath, "\""));
        if (stderrPath != null)
            command = String.format("%s > \"%s\"", command, CommandLineTask.protect(stderrPath, "\""));

        LOGGER.info("Executing command [%s] with SSH connector (%s@%s)", command, username, hostname);
        channel.setCommand(command);
        channel.setInputStream(null);
        channel.setErrStream(System.err, true);
        channel.setOutputStream(System.out, true);
        channel.setPty(!detach);

        channel.connect();

        return new SSHProcess(channel);
    }


    /**
     * An SSH process
     */
    @Persistent
    public static class SSHProcess extends Process {
        private ChannelExec channel;

        public SSHProcess(ChannelExec channel) {
            this.channel = channel;
        }

        @Override
        public OutputStream getOutputStream() {
            try {
                return channel.getOutputStream();
            } catch (IOException e) {
                throw new ExperimaestroException(e);
            }
        }

        @Override
        public InputStream getInputStream() {
            try {
                return channel.getInputStream();
            } catch (IOException e) {
                throw new ExperimaestroException(e);
            }
        }

        @Override
        public InputStream getErrorStream() {
            try {
                return channel.getErrStream();
            } catch (IOException e) {
                throw new ExperimaestroException(e);
            }
        }

        @Override
        public int waitFor() throws InterruptedException {
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

        protected void finalize() {
            destroy();
        }

        @Override
        public String getPID() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public boolean isRunning() {
            return channel.isConnected();
        }
    }



    /** an SSH session */
    class SSHSession {
        public Session session;
        public SftpFileSystem filesystem;

        SSHSession() throws JSchException, FileSystemException {
            init();
        }

        void init() throws JSchException, FileSystemException {
            session = SftpClientFactory.createConnection(hostname, port, username.toCharArray(), null, options.getOptions());
            session.connect();
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

    /** Get the session (creates it if necessary) */
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

    /** Static map to sessions
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
        private ConnectorDelegator connector;

        public SSHLock() {}

        public SSHLock(Connector connector, String path) {
            this.connector = connector.delegate();
            this.path = path;
        }

        @Override
        public boolean dispose() {
            throw new NotImplementedYetException();
//            final ChannelExec channel;
//            try {
//                ChannelSftp sftp = newSftpChannel();
//                LOGGER.info("Disposing of SSH lock [%s]", path);
//                sftp.connect();
//                sftp.chmod(644, path);
//                sftp.rm(path);
//                sftp.disconnect();
//            } catch (RuntimeException e) {
//                throw e;
//            } catch (Exception e) {
//                new RuntimeException(e);
//            }
//
//            return false;
        }

        @Override
        public void changeOwnership(String pid) {
            throw new NotImplementedYetException();
        }

        @Override
        public void init(Scheduler scheduler) throws DatabaseException {
            connector.init(scheduler);
        }
    }
}
