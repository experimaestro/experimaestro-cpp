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

import com.jcraft.jsch.IdentityRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.agentproxy.AgentProxyException;
import com.jcraft.jsch.agentproxy.RemoteIdentityRepository;
import com.jcraft.jsch.agentproxy.USocketFactory;
import com.jcraft.jsch.agentproxy.connector.PageantConnector;
import com.jcraft.jsch.agentproxy.connector.SSHAgentConnector;
import com.jcraft.jsch.agentproxy.usocket.JNAUSocketFactory;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.provider.sftp.IdentityRepositoryFactory;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 25/6/12
 */
public class SSHOptions extends ConnectorOptions {
    FileSystemOptions options;


    public void setCompression(String name) throws FileSystemException {
        SftpFileSystemConfigBuilder.getInstance().setCompression(options, name);
    }

    public void setUseSSHAgent(boolean use) throws FileSystemException {
        if (use)
            SftpFileSystemConfigBuilder.getInstance().setIdentityRepositoryFactory(options, new AgentRepositoryFactory());
        else
            SftpFileSystemConfigBuilder.getInstance().setIdentityRepositoryFactory(options, null);

    }

    public FileSystemOptions getOptions() {
        return options;
    }

    /** Use the SSH agent to connect */
    private static class AgentRepositoryFactory implements IdentityRepositoryFactory {
        @Override
        public IdentityRepository create(JSch jsch) {
            USocketFactory usf = null;
            try {
                usf = new JNAUSocketFactory();
                com.jcraft.jsch.agentproxy.Connector agent = new SSHAgentConnector(usf);
                if (PageantConnector.isConnectorAvailable())
                    agent = new PageantConnector();
                IdentityRepository irepo = new RemoteIdentityRepository(agent);
                return new RemoteIdentityRepository(agent);
            } catch (AgentProxyException e) {
                throw new ExperimaestroRuntimeException(e);
            }
        }
    }
}
