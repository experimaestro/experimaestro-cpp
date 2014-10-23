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

package sf.net.experimaestro.manager.js;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.util.SecurityUtils;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.ForwardingFilter;
import org.apache.sshd.server.auth.UserAuthNone;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.keyprovider.PEMGeneratorHostKeyProvider;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.sftp.SftpSubsystem;
import sf.net.experimaestro.utils.TemporaryDirectory;
import sf.net.experimaestro.utils.log.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

/**
 * An embedded SSH server (used for testing)
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 15/8/12
 */
public class SSHServer {
    static final private Logger LOGGER = Logger.getLogger();
    static TemporaryDirectory directory;
    /**
     * Our SSH server port
     */
    private static int socketPort = -1;

    synchronized static public int js_sshd_server() throws IOException {
        if (socketPort > 0)
            return socketPort;

        // Starting the SSH server

        SshServer Server = SshServer.setUpDefaultServer();
        socketPort = findFreeLocalPort();
        Server.setPort(socketPort);
        LOGGER.info(String.format("Starting the SSH server on port %d", socketPort));

        directory = new TemporaryDirectory("scheduler-tests", "dir");

        if (SecurityUtils.isBouncyCastleRegistered()) {
            Server.setKeyPairProvider(new PEMGeneratorHostKeyProvider(new File(directory.getFile(), "key.pem").getAbsolutePath()));
        } else {
            Server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(new File(directory.getFile(), "key.ser").getAbsolutePath()));
        }

        List<NamedFactory<Command>> list = new ArrayList<>(1);
        list.add(new NamedFactory<Command>() {
            @Override
            public String getName() {
                return "sftp";
            }

            @Override
            public Command create() {
                return new SftpSubsystem();
            }
        });
        Server.setSubsystemFactories(list);
        Server.setPasswordAuthenticator((username, password, session) -> username != null && username.equals(password));
        Server.setPublickeyAuthenticator((username, key, session) -> true);
        Server.setForwardingFilter(new ForwardingFilter() {
            public boolean canConnect(InetSocketAddress address, ServerSession session) {
                return true;
            }

            public boolean canForwardAgent(ServerSession session) {
                return true;
            }

            public boolean canForwardX11(ServerSession session) {
                return true;
            }

            public boolean canListen(InetSocketAddress address, ServerSession session) {
                return true;
            }
        });
        // Allows the execution of commands
        Server.setCommandFactory(new ScpCommandFactory(new TestCommandFactory()));
        // HACK Start
        // How do we really do simple user to directory matching?
//        Server.setFileSystemFactory(new TestFileSystemFactory());
        // HACK End
        Server.start();
        // HACK Start
        // How do we really do simple security?
        // Do this after we start the server to simplify this set up code.
        Server.getUserAuthFactories().add(new UserAuthNone.Factory());
        // HACK End

        return socketPort;
    }

    /**
     * Finds a free local socket port.
     *
     * @return a free local socket port.
     * @throws java.io.IOException
     */
    public static int findFreeLocalPort() throws IOException {
        ServerSocket server = new ServerSocket(0);
        try {
            return server.getLocalPort();
        } finally {
            server.close();
        }
    }

    /**
     * Asynchronous copy stream
     *
     * @param inputStream
     * @param out
     */
    static private void copyStream(final InputStream inputStream, final OutputStream out) {
        new Thread("Stream copy") {
            @Override
            public void run() {
                try {
                    IOUtils.copy(inputStream, out);
                } catch (IOException e) {
                    throw new AssertionError("I/O error", e);
                } finally {
                    try {
                        out.close();
                    } catch (IOException e) {
                        throw new AssertionError("I/O error", e);
                    }
                }
            }
        }.start();
    }

    /**
     * The command factory for the SSH server:
     * Handles two commands: id -u and id -G
     */
    private static class TestCommandFactory extends ScpCommandFactory {

        public Command createCommand(final String command) {
            return new Command() {
                public ExitCallback callback = null;
                public OutputStream out = null;
                public OutputStream err = null;

                @Override
                public void setInputStream(InputStream in) {
                }

                @Override
                public void setOutputStream(OutputStream out) {
                    this.out = out;
                }

                @Override
                public void setErrorStream(OutputStream err) {
                    this.err = err;
                }

                @Override
                public void setExitCallback(ExitCallback callback) {
                    this.callback = callback;

                }

                @Override
                public void start(Environment env) throws IOException {
                    final ProcessBuilder builder = new ProcessBuilder();
                    // basic split -- just for test!
                    builder.command(command.split("(?!\\\\) "));

                    if (out != null)
                        builder.redirectOutput(ProcessBuilder.Redirect.PIPE);
                    if (err != null)
                        builder.redirectError(ProcessBuilder.Redirect.PIPE);

                    final Process process = builder.start();


                    if (out != null) {
                        copyStream(process.getInputStream(), out);
                    }

                    if (err != null) {
                        copyStream(process.getErrorStream(), err);
                    }

                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                final int exitValue = process.waitFor();
                                callback.onExit(exitValue);
                            } catch (InterruptedException e) {
                                callback.onExit(-1);
                                throw new AssertionError("Error while waiting for the process to end", e);
                            }
                        }
                    }.start();
                }


                @Override
                public void destroy() {
                }
            };
        }
    }

}
