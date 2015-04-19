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

import com.jcraft.jsch.*;
import com.jcraft.jsch.agentproxy.AgentProxyException;
import com.jcraft.jsch.agentproxy.RemoteIdentityRepository;
import com.jcraft.jsch.agentproxy.USocketFactory;
import com.jcraft.jsch.agentproxy.connector.SSHAgentConnector;
import com.jcraft.jsch.agentproxy.usocket.JNAUSocketFactory;
import com.pastdev.jsch.DefaultSessionFactory;
import com.pastdev.jsch.SessionFactory;
import com.pastdev.jsch.proxy.SshProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystemException;

/**
 * All the options for connecting to a host through SSH
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class SSHOptions extends ConnectorOptions {
    /**
     * Username
     */
    String username;

    /**
     * Hostname to connect to
     */
    String hostname;

    /**
     * Password - TODO: encrypt before storing
     */
    private String password;

    /** Default SSH port */
    static final int SSHD_DEFAULT_PORT = 22;

    /**
     * Port
     */
    int port = SSHD_DEFAULT_PORT;


    /**
     * Compression level
     */
    private String compression;

    /**
     * Whether to use an SSH agent
     */
    private boolean useSSHAgent = true;

    /**
     * Proxy for configuration
     */
    private ProxyConfiguration proxy;

    public void setCompression(String compression) {
        this.compression = compression;
    }

    public void setUseSSHAgent(boolean useSSHAgent) {
        this.useSSHAgent = useSSHAgent;
    }

    public void setStreamProxy(String uri, SSHOptions sshOptions) {
        try {
            proxy = new NCProxyConfiguration(uri, sshOptions);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public DefaultSessionFactory getSessionFactory() throws IOException {
        DefaultSessionFactory factory = new DefaultSessionFactory();

        // Set basic options
        factory.setHostname(hostname);
        factory.setUsername(username);
        factory.setPort(port);

//        // Use root file system
        if (compression != null) {
            throw new NotImplementedException();
        }

        if (useSSHAgent) {
            factory.setIdentityRepository(createSSHAgentIdentityRepository());
        }

        if (proxy != null) {
            proxy.configure(factory);
        }

        return factory;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public IdentityRepository createSSHAgentIdentityRepository() {
        try {
            USocketFactory usf = new JNAUSocketFactory();
            com.jcraft.jsch.agentproxy.Connector agent = new SSHAgentConnector(usf);
            if (SSHAgentConnector.isConnectorAvailable()) {
                agent = new SSHAgentConnector(new JNAUSocketFactory());
            }
            return new RemoteIdentityRepository(agent);
        } catch (AgentProxyException e) {
            throw new XPMRuntimeException(e);
        }
    }

    public void setHostName(String hostname) {
        this.hostname = hostname;
    }

    public void setUserName(String username) {
        this.username = username;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getHostName() {
        return hostname;
    }

    public String getUserName() {
        return username;
    }

    public SSHOptions copy() {
        final SSHOptions options = new SSHOptions();

        options.setUserName(username);
        options.setHostName(hostname);
        options.setPort(port);

        options.setCompression(compression);
        options.setUseSSHAgent(useSSHAgent);

        options.proxy = proxy;

        return options;
    }


    private interface ProxyConfiguration {
        void configure(DefaultSessionFactory factory) throws IOException;
    }

    public static class MySshProxy implements Proxy {
        private Channel channel;
        private InputStream inputStream;
        private OutputStream outputStream;
        private SessionFactory sessionFactory;
        private Session session;

        public MySshProxy( SessionFactory sessionFactory ) throws JSchException {
            this.sessionFactory = sessionFactory;
//            this.session = sessionFactory.newSession();
        }

        public void close() {
            if ( session != null && session.isConnected() ) {
                session.disconnect();
            }
        }

        public void connect( SocketFactory socketFactory, String host, int port, int timeout ) throws Exception {
            if (session == null || !session.isConnected()) {
                session = sessionFactory.newSession();
            }
            session.connect();

            channel = session.getStreamForwarder( host, port );
            inputStream = channel.getInputStream();
            outputStream = channel.getOutputStream();

            channel.connect( timeout );
        }

        public InputStream getInputStream() {
            return inputStream;
        }

        public OutputStream getOutputStream() {
            return outputStream;
        }

        public Socket getSocket() {
            return null;
        }

        @Override
        public String toString() {
            return "PROXY(" + sessionFactory.toString() + ")";
        }
    }

    private static class NCProxyConfiguration implements ProxyConfiguration {
        SSHOptions sshOptions;

        private NCProxyConfiguration() {
        }

        public NCProxyConfiguration(String uriString, SSHOptions sshOptions) throws URISyntaxException {
            this.sshOptions = sshOptions;

            URI uri = new URI(uriString);
            sshOptions.setUserName(uri.getUserInfo());
            sshOptions.setHostName(uri.getHost());
            if (uri.getPort() != -1) {
                sshOptions.setPort(uri.getPort());
            }
        }

        @Override
        public void configure(DefaultSessionFactory builder) throws IOException {

            DefaultSessionFactory factory = sshOptions.getSessionFactory();

            try {
                MySshProxy proxy = new MySshProxy(factory);
                builder.setProxy(proxy);
//                builder.setProxyType(opts, SftpFileSystemConfigBuilder.PROXY_STREAM);
//                builder.setProxyCommand(opts, SftpStreamProxy.NETCAT_COMMAND);
//                builder.setProxyUser(opts, username);
//                builder.setProxyHost(opts, hostname);
//                builder.setProxyPassword(opts, "");
//                builder.setProxyPort(opts, port);
//                builder.setProxyOptions(opts, sshOptions.getSessionFactory());

            } catch (JSchException e) {
                throw new IOException(e);
            }
        }
    }
}
