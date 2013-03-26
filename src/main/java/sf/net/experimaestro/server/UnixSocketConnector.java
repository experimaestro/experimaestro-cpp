/*
 * This file is part of experimaestro.
 * Copyright (c) 2013 B. Piwowarski <benjamin@bpiwowar.net>
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

package sf.net.experimaestro.server;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import sf.net.experimaestro.utils.log.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.ServerSocketChannel;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 26/3/13
 */
public class UnixSocketConnector extends ServerConnector implements Connector {
    final static private Logger LOGGER = Logger.getLogger();
    private final File socketFile;


    public UnixSocketConnector(Server server, File socketFile) throws IOException {
        super(server);
        this.socketFile = socketFile;
    }


    @Override
    public void open() throws IOException {
        if (_acceptChannel == null) {
            ServerSocketChannel serverChannel = null;
            if (isInheritChannel()) {
                Channel channel = System.inheritedChannel();
                if (channel instanceof ServerSocketChannel)
                    serverChannel = (ServerSocketChannel) channel;
                else
                    LOG.warn("Unable to use System.inheritedChannel() [{}]. Trying a new ServerSocketChannel at {}:{}", channel, getHost(), getPort());
            }

            if (serverChannel == null) {
                serverChannel = ServerSocketChannel.open();

                AFUNIXSocketAddress socketAddress = new AFUNIXSocketAddress(socketFile);

                serverChannel.socket().bind(socketAddress, getAcceptQueueSize());
                serverChannel.socket().setReuseAddress(getReuseAddress());

                addBean(serverChannel);
            }

            serverChannel.configureBlocking(true);
            addBean(serverChannel);

            _acceptChannel = serverChannel;
        }
    }

}
