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

import com.jcraft.jsch.SftpATTRS;
import org.apache.commons.vfs2.*;
import org.apache.log4j.Logger;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.util.Buffer;
import org.apache.sshd.common.util.SecurityUtils;
import org.apache.sshd.server.*;
import org.apache.sshd.server.auth.UserAuthNone;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.keyprovider.PEMGeneratorHostKeyProvider;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.sftp.SftpSubsystem;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptableObject;
import org.testng.annotations.*;
import sf.net.experimaestro.manager.Repository;
import sf.net.experimaestro.scheduler.ResourceLocator;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.TemporaryDirectory;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URL;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

/**
 * Runs the scripts contained in the directory "test/resources/js"
 * <p/>
 * Tests are defined by matching javascript functions matching "function test_XXXX()"
 */
public class ScriptTest {
    static final Logger LOGGER = Logger.getLogger(ScriptTest.class);

    private static final String JS_SCRIPT_PATH = "/js";

    static public class JavaScriptChecker {

        private FileObject file;
        private String content;
        private Context context;
        private Repository repository;
        private ScriptableObject scope;

        public JavaScriptChecker(FileObject file) throws
                IOException {
            this.file = file;
            this.content = getFileContent(file);
        }

        @Override
        public String toString() {
            return format("JavaScript for [%s]", file);
        }

        @DataProvider
        public Object[][] jsProvider() throws IOException {
            Pattern pattern = Pattern
                    .compile("function\\s+(test_[\\w]+)\\s*\\(");
            Matcher matcher = pattern.matcher(content);
            ArrayList<Object[]> list = new ArrayList<>();
            list.add(new Object[]{null});

            while (matcher.find()) {
                list.add(new Object[]{matcher.group(1)});
            }

            return list.toArray(new Object[list.size()][]);
        }

        @BeforeTest
        public void enter() {
            context = Context.enter();
            scope = context.initStandardObjects();
            repository = new Repository(new ResourceLocator());

        }

        @AfterTest
        public void exit() {
            Context.exit();
        }

        @Test(dataProvider = "jsProvider")
        public void testScript(String functionName) throws
                IOException, SecurityException, IllegalAccessException,
                InstantiationException, InvocationTargetException,
                NoSuchMethodException {
            if (functionName == null) {
                // Defines the environment
                Map<String, String> environment = System.getenv();
                XPMObject jsXPM = new XPMObject(new ResourceLocator(), context, environment, scope,
                        repository, null);

                // Adds some special functions available for tests only
                JSUtils.addFunction(ScriptTest.class, scope, "sshd_server", new Class[]{});

                context.evaluateReader(scope, new StringReader(content),
                        file.toString(), 1, null);
            } else {
                Object object = scope.get(functionName, scope);
                assert object instanceof Function : format(
                        "%s is not a function", functionName);
                Function function = (Function) object;
                function.call(context, scope, null, new Object[]{});
            }
        }
    }

    @Factory
    public static Object[] jsFactories() throws IOException {
        // Get the JavaScript files
        final URL url = ScriptTest.class.getResource(JS_SCRIPT_PATH);
        FileSystemManager fsManager = VFS.getManager();
        FileObject dir = fsManager.resolveFile(url.toExternalForm());
        FileObject[] files = dir.findFiles(new FileSelector() {
            @Override
            public boolean traverseDescendents(FileSelectInfo info)
                    throws Exception {
                return true;
            }

            @Override
            public boolean includeFile(FileSelectInfo file) throws Exception {
                return file.getFile().getName().getExtension().equals("js");
            }
        });

        Object[] r = new Object[files.length];
        for (int i = r.length; --i >= 0; )
            r[i] = new JavaScriptChecker(files[i]);

        return r;
    }

    private static String getFileContent(FileObject file)
            throws IOException {
        InputStreamReader reader = new InputStreamReader(file.getContent()
                .getInputStream());
        char[] cbuf = new char[8192];
        int read = 0;
        StringBuilder builder = new StringBuilder();
        while ((read = reader.read(cbuf, 0, cbuf.length)) > 0)
            builder.append(cbuf, 0, read);
        String s = builder.toString();
        return s;
    }



    // --- SSH related

    /**
     * Our SSH server port
     */
    private static int socketPort = -1;

    static TemporaryDirectory directory;

    synchronized static public int js_sshd_server() throws IOException {
        if (socketPort > 0)
            return socketPort;

        // Starting the SSH server
        LOGGER.info(String.format("Starting the SSH server on port %d", socketPort));

        SshServer Server = SshServer.setUpDefaultServer();
        socketPort = findFreeLocalPort();
        Server.setPort(socketPort);

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
                return new MySftpSubsystem();
            }
        });
        Server.setSubsystemFactories(list);
        Server.setPasswordAuthenticator(new PasswordAuthenticator() {
            @Override
            public boolean authenticate(String username, String password, ServerSession session) {
                return username != null && username.equals(password);
            }
        });
        Server.setPublickeyAuthenticator(new PublickeyAuthenticator() {
            public boolean authenticate(String username, PublicKey key, ServerSession session) {
                // File f = new File("/Users/" + username + "/.ssh/authorized_keys");
                return true;
            }
        });
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

    private static class MySftpSubsystem extends SftpSubsystem {
        TreeMap<String, Integer> permissions = new TreeMap<>();
        private int _version;

        protected void process(Buffer buffer) throws IOException {
            int rpos = buffer.rpos();
            int length = buffer.getInt();
            int type = buffer.getByte();
            int id = buffer.getInt();

            switch (type) {
                case SSH_FXP_SETSTAT:
                case SSH_FXP_FSETSTAT: {
                    // Get the path
                    String path = buffer.getString();
                    // Get the permission
                    SftpAttrs attrs = new SftpAttrs(buffer);
                    permissions.put(path, attrs.permissions);
//                    System.err.format("Setting [%s] permission to %o%n", path, attrs.permissions);
                    break;
                }

                case SSH_FXP_REMOVE: {
                    // Remove cached attributes
                    String path = buffer.getString();
                    permissions.remove(path);
//                    System.err.format("Removing [%s] permission cache%n", path);
                    break;
                }

                case SSH_FXP_INIT: {
                    // Just grab the version here
                    this._version = id;
                    break;
                }
            }

            buffer.rpos(rpos);
            super.process(buffer);

        }

        protected void writeAttrs(Buffer buffer, SshFile file, int flags) {
            if (!file.doesExist()) {
                throw new RuntimeException(file.getAbsolutePath());
            }


            int p = 0;

            final Integer cached = permissions.get(file.getAbsolutePath());
            if (cached != null) {
                // Use cached permissions
//                System.err.format("Using cached [%s] permission of %o%n", file.getAbsolutePath(), cached);
                p |= cached;
            } else {
                // Use permissions from Java file
                if (file.isReadable()) {
                    p |= S_IRUSR;
                }
                if (file.isWritable()) {
                    p |= S_IWUSR;
                }
                if (file.isExecutable()) {
                    p |= S_IXUSR;
                }
            }

            if (_version >= 4) {
                long size = file.getSize();
//                String username = session.getUsername();
                long lastModif = file.getLastModified();
                if (file.isFile()) {
                    buffer.putInt(SSH_FILEXFER_ATTR_PERMISSIONS);
                    buffer.putByte((byte) SSH_FILEXFER_TYPE_REGULAR);
                    buffer.putInt(p);
                } else if (file.isDirectory()) {
                    buffer.putInt(SSH_FILEXFER_ATTR_PERMISSIONS);
                    buffer.putByte((byte) SSH_FILEXFER_TYPE_DIRECTORY);
                    buffer.putInt(p);
                } else {
                    buffer.putInt(0);
                    buffer.putByte((byte) SSH_FILEXFER_TYPE_UNKNOWN);
                }
            } else {
                if (file.isFile()) {
                    p |= 0100000;
                }
                if (file.isDirectory()) {
                    p |= 0040000;
                }


                if (file.isFile()) {
                    buffer.putInt(SSH_FILEXFER_ATTR_SIZE | SSH_FILEXFER_ATTR_PERMISSIONS | SSH_FILEXFER_ATTR_ACMODTIME);
                    buffer.putLong(file.getSize());
                    buffer.putInt(p);
                    buffer.putInt(file.getLastModified() / 1000);
                    buffer.putInt(file.getLastModified() / 1000);
                } else if (file.isDirectory()) {
                    buffer.putInt(SSH_FILEXFER_ATTR_PERMISSIONS | SSH_FILEXFER_ATTR_ACMODTIME);
                    buffer.putInt(p);
                    buffer.putInt(file.getLastModified() / 1000);
                    buffer.putInt(file.getLastModified() / 1000);
                } else {
                    buffer.putInt(0);
                }
            }
        }

        private static class SftpAttrs {
            int flags = 0;
            private int uid;
            long size = 0;
            private int gid;
            private int atime;
            private int permissions;
            private int mtime;
            private String[] extended;

            private SftpAttrs(Buffer buf) {
                int flags = 0;
                flags = buf.getInt();

                if ((flags & SftpATTRS.SSH_FILEXFER_ATTR_SIZE) != 0) {
                    size = buf.getLong();
                }
                if ((flags & SftpATTRS.SSH_FILEXFER_ATTR_UIDGID) != 0) {
                    uid = buf.getInt();
                    gid = buf.getInt();
                }
                if ((flags & SftpATTRS.SSH_FILEXFER_ATTR_PERMISSIONS) != 0) {
                    permissions = buf.getInt();
                }
                if ((flags & SftpATTRS.SSH_FILEXFER_ATTR_ACMODTIME) != 0) {
                    atime = buf.getInt();
                }
                if ((flags & SftpATTRS.SSH_FILEXFER_ATTR_ACMODTIME) != 0) {
                    mtime = buf.getInt();
                }

            }
        }
    }

    /**
     * The command factory for the SSH server:
     * Handles two commands: id -u and id -G
     */
    private static class TestCommandFactory extends ScpCommandFactory {
        public Command createCommand(final String command) {
            return new Command() {
                public ExitCallback callback = null;
                public PrintStream out = null;
                public PrintStream err = null;

                @Override
                public void setInputStream(InputStream in) {
                }

                @Override
                public void setOutputStream(OutputStream out) {
                    this.out = new PrintStream(out);
                }

                @Override
                public void setErrorStream(OutputStream err) {
                    this.err = new PrintStream(err);
                }

                @Override
                public void setExitCallback(ExitCallback callback) {
                    this.callback = callback;

                }

                @Override
                public void start(Environment env) throws IOException {
                    int code = 0;
                    if (command.equals("id -G") || command.equals("id -u")) {
                        out.println(0);
                    } else {
                        if (err != null) {
                            err.format("Unknown command %s%n", command);
                        }
                        code = -1;
                    }
                    if (out != null) {
                        out.flush();
                    }
                    if (err != null) {
                        err.flush();
                    }
                    callback.onExit(code);
                }

                @Override
                public void destroy() {
                }
            };
        }
    }

    /**
     * Finds a free local socket port.
     *
     * @return a free local socket port.
     * @throws IOException
     */
    public static int findFreeLocalPort() throws IOException {
        ServerSocket server = new ServerSocket(0);
        try {
            return server.getLocalPort();
        } finally {
            server.close();
        }
    }


}