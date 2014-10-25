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

import com.jcraft.jsch.IdentityRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.agentproxy.AgentProxyException;
import com.jcraft.jsch.agentproxy.RemoteIdentityRepository;
import com.jcraft.jsch.agentproxy.USocketFactory;
import com.jcraft.jsch.agentproxy.connector.SSHAgentConnector;
import com.jcraft.jsch.agentproxy.usocket.JNAUSocketFactory;
import com.pastdev.jsch.DefaultSessionFactory;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystemException;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class SSHOptions extends ConnectorOptions {
    /**
     * Password - TODO: encrypt before storing
     */
    private String password;

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

    public Object getOptions() throws FileSystemException {
//        DefaultSessionFactory factory = new DefaultSessionFactory();
//
//
//        // Use root file system
//        if (compression != null) {
//            throw new NotImplementedException();
//        }
//
//        if (useSSHAgent) {
//            builder.setIdentityRepositoryFactory(options, new AgentRepositoryFactory());
//        } else {
//            builder.setIdentityRepositoryFactory(options, null);
//        }
//
//
//        if (proxy != null)
//            proxy.configure(builder, options);
//
//        return options;
        // TODO implement getOptions
        throw new NotImplementedException();
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLockFileCommand() {
        return "lockfile";
    }



    /**
     * Use the SSH agent to connect
     */
    private static class AgentRepositoryFactory implements IdentityRepositoryFactory, sf.net.experimaestro.connectors.AgentRepositoryFactory {
        @Override
        public IdentityRepository create(JSch jsch) {
            USocketFactory usf = null;
            try {
                usf = new JNAUSocketFactory();
                com.jcraft.jsch.agentproxy.Connector agent = new SSHAgentConnector(usf);
                if (SSHAgentConnector.isConnectorAvailable()) {
                    agent = new SSHAgentConnector(new JNAUSocketFactory());
                }
                IdentityRepository irepo = new RemoteIdentityRepository(agent);
                return new RemoteIdentityRepository(agent);
            } catch (AgentProxyException e) {
                throw new XPMRuntimeException(e);
            }
        }
    }

    private static interface ProxyConfiguration {
        void configure() throws FileSystemException;
    }

    private static class NCProxyConfiguration implements ProxyConfiguration {
        String username;
        String hostname;
        int port;

        SSHOptions sshOptions;

        private NCProxyConfiguration() {
        }

        public NCProxyConfiguration(String uriString, SSHOptions sshOptions) throws URISyntaxException {
            URI uri = new URI(uriString);
            this.username = uri.getUserInfo();
            this.hostname = uri.getHost();
            this.port = uri.getPort();
            if (this.port == -1)
                this.port = SSHConnector.SSHD_DEFAULT_PORT;
            this.sshOptions = sshOptions;
        }

        @Override
        public void configure() throws FileSystemException {
            // TODO implement configure
            throw new NotImplementedException();
//            builder.setProxyType(opts, SftpFileSystemConfigBuilder.PROXY_STREAM);
//            builder.setProxyCommand(opts, SftpStreamProxy.NETCAT_COMMAND);
//            builder.setProxyUser(opts, username);
//            builder.setProxyHost(opts, hostname);
//            builder.setProxyPassword(opts, "");
//            builder.setProxyPort(opts, port);
//            builder.setProxyOptions(opts, sshOptions.getOptions());
        }
    }
}
