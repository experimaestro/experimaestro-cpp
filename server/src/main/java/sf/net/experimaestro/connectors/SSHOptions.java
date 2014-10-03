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

import com.jcraft.jsch.IdentityRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.agentproxy.AgentProxyException;
import com.jcraft.jsch.agentproxy.RemoteIdentityRepository;
import com.jcraft.jsch.agentproxy.USocketFactory;
import com.jcraft.jsch.agentproxy.connector.SSHAgentConnector;
import com.jcraft.jsch.agentproxy.usocket.JNAUSocketFactory;
import com.sleepycat.persist.model.Persistent;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.provider.sftp.IdentityRepositoryFactory;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;
import org.apache.commons.vfs2.provider.sftp.SftpStreamProxy;
import sf.net.experimaestro.exceptions.XPMRuntimeException;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 25/6/12
 */
@Persistent
public class SSHOptions extends ConnectorOptions  {
    /** Password - TODO: encrypt before storing */
    private String password;

    /** Compression level */
    private String compression;

    /** Whether to use an SSH agent */
    private boolean useSSHAgent = true;

    /** Proxy for configuration */
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

    public FileSystemOptions getOptions() throws FileSystemException {
        FileSystemOptions options = new FileSystemOptions();

        final SftpFileSystemConfigBuilder builder = SftpFileSystemConfigBuilder.getInstance();

        // Use root file system
        builder.setUserDirIsRoot(options, false);

        if (compression != null)
            builder.setCompression(options, compression);

        if (useSSHAgent)
            builder.setIdentityRepositoryFactory(options, new AgentRepositoryFactory());
        else
            builder.setIdentityRepositoryFactory(options, null);


        if (proxy != null)
            proxy.configure(builder, options);

        return options;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLockFileCommand() {
        return "lockfile";
    }


    /** Use the SSH agent to connect */
    private static class AgentRepositoryFactory implements IdentityRepositoryFactory {
        @Override
        public IdentityRepository create(JSch jsch) {
            USocketFactory usf = null;
            try {
                usf = new JNAUSocketFactory();
                com.jcraft.jsch.agentproxy.Connector agent = new SSHAgentConnector(usf);
                if (SSHAgentConnector.isConnectorAvailable())
                    agent = new SSHAgentConnector(new JNAUSocketFactory());
                IdentityRepository irepo = new RemoteIdentityRepository(agent);
                return new RemoteIdentityRepository(agent);
            } catch (AgentProxyException e) {
                throw new XPMRuntimeException(e);
            }
        }
    }

    private static interface ProxyConfiguration {
        void configure(SftpFileSystemConfigBuilder builder, FileSystemOptions opts) throws FileSystemException;
    }

    @Persistent
    private static class NCProxyConfiguration implements ProxyConfiguration {
        String username;
        String hostname;
        int port;

        SSHOptions sshOptions;

        private NCProxyConfiguration() {}

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
        public void configure(SftpFileSystemConfigBuilder builder, FileSystemOptions opts) throws FileSystemException {
            builder.setProxyType(opts, SftpFileSystemConfigBuilder.PROXY_STREAM);
            builder.setProxyCommand(opts, SftpStreamProxy.NETCAT_COMMAND);
            builder.setProxyUser(opts, username);
            builder.setProxyHost(opts, hostname);
            builder.setProxyPassword(opts, "");
            builder.setProxyPort(opts, port);
            builder.setProxyOptions(opts, sshOptions.getOptions());
        }
    }
}
