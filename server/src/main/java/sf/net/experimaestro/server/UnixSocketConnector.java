package sf.net.experimaestro.server;

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

import jnr.unixsocket.UnixServerSocketChannel;
import jnr.unixsocket.UnixSocketAddress;
import jnr.unixsocket.UnixSocketChannel;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.annotation.ManagedAttribute;
import org.eclipse.jetty.util.annotation.Name;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.Scheduler;
import sf.net.experimaestro.utils.log.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 26/3/13
 */
public class UnixSocketConnector extends AbstractNetworkConnector implements Connector {
    final static private Logger LOGGER = Logger.getLogger();
    private final ServerConnectorManager _manager;
    volatile UnixServerSocketChannel _acceptChannel;
    private File socketFile;
    private volatile boolean _inheritChannel = false;
    private volatile int _localPort = -1;
    private volatile int _acceptQueueSize = 0;
    private volatile boolean _reuseAddress = true;


    /* ------------------------------------------------------------ */

    /**
     * HTTP Server Connection.
     * <p>Construct a ServerConnector with a private instance of {@link org.eclipse.jetty.server.HttpConnectionFactory} as the only factory.</p>
     *
     * @param server The {@link Server} this connector will accept connection for.
     */
    public UnixSocketConnector(
            @Name("server") Server server) {
        this(server, null, null, null, 0, 0, new HttpConnectionFactory());
    }

    /* ------------------------------------------------------------ */

    /**
     * Generic Server Connection with default configuration.
     * <p>Construct a Server Connector with the passed Connection factories.</p>
     *
     * @param server    The {@link Server} this connector will accept connection for.
     * @param factories Zero or more {@link org.eclipse.jetty.server.ConnectionFactory} instances used to createSSHAgentIdentityRepository and configure connections.
     */
    public UnixSocketConnector(
            @Name("server") Server server,
            @Name("factories") ConnectionFactory... factories) {
        this(server, null, null, null, 0, 0, factories);
    }

    /* ------------------------------------------------------------ */

    /**
     * HTTP Server Connection.
     * <p>Construct a ServerConnector with a private instance of {@link HttpConnectionFactory} as the primary protocol</p>.
     *
     * @param server            The {@link Server} this connector will accept connection for.
     * @param sslContextFactory If non null, then a {@link org.eclipse.jetty.server.SslConnectionFactory} is instantiated and prepended to the
     *                          list of HTTP Connection Factory.
     */
    public UnixSocketConnector(
            @Name("server") Server server,
            @Name("sslContextFactory") SslContextFactory sslContextFactory) {
        this(server, null, null, null, 0, 0, AbstractConnectionFactory.getFactories(sslContextFactory, new HttpConnectionFactory()));
    }

    /* ------------------------------------------------------------ */

    /**
     * Generic SSL Server Connection.
     *
     * @param server            The {@link Server} this connector will accept connection for.
     * @param sslContextFactory If non null, then a {@link org.eclipse.jetty.server.SslConnectionFactory} is instantiated and prepended to the
     *                          list of ConnectionFactories, with the first factory being the default protocol for the SslConnectionFactory.
     * @param factories         Zero or more {@link ConnectionFactory} instances used to createSSHAgentIdentityRepository and configure connections.
     */
    public UnixSocketConnector(
            @Name("server") Server server,
            @Name("sslContextFactory") SslContextFactory sslContextFactory,
            @Name("factories") ConnectionFactory... factories) {
        this(server, null, null, null, 0, 0, AbstractConnectionFactory.getFactories(sslContextFactory, factories));
    }

    /**
     * Generic Server Connection.
     *
     * @param server     The server this connector will be accept connection for.
     * @param executor   An executor used to run tasks for handling requests, acceptors and selectors. I
     *                   If null then use the servers executor
     * @param scheduler  A scheduler used to schedule timeouts. If null then use the servers scheduler
     * @param bufferPool A ByteBuffer pool used to allocate buffers.  If null then createSSHAgentIdentityRepository a private pool with default configuration.
     * @param acceptors  the number of acceptor threads to use, or 0 for a default value. Acceptors accept new TCP/IP connections.
     * @param selectors  the number of selector threads, or 0 for a default value. Selectors notice and schedule established connection that can make IO progress.
     * @param factories  Zero or more {@link ConnectionFactory} instances used to createSSHAgentIdentityRepository and configure connections.
     */
    public UnixSocketConnector(
            @Name("server") Server server,
            @Name("executor") Executor executor,
            @Name("scheduler") Scheduler scheduler,
            @Name("bufferPool") ByteBufferPool bufferPool,
            @Name("acceptors") int acceptors,
            @Name("selectors") int selectors,
            @Name("factories") ConnectionFactory... factories) {
        super(server, executor, scheduler, bufferPool, acceptors, factories);
        _manager = new ServerConnectorManager(getExecutor(), getScheduler(), selectors > 0 ? selectors : Runtime.getRuntime().availableProcessors());
        addBean(_manager, true);
    }

    public void setSocketFile(File socketFile) {
        this.socketFile = socketFile;
    }

    @Override
    public void open() throws IOException {
        if (_acceptChannel == null) {
            UnixServerSocketChannel serverChannel = null;
            if (isInheritChannel()) {
                Channel channel = System.inheritedChannel();
                if (channel instanceof ServerSocketChannel)
                    serverChannel = (UnixServerSocketChannel) channel;
                else
                    LOG.warn("Unable to use System.inheritedChannel() [{}]. Trying a new ServerSocketChannel at {}:{}", channel, getHost(), getPort());
            }

            if (serverChannel == null) {
                serverChannel = UnixServerSocketChannel.open();

                UnixSocketAddress socketAddress = new UnixSocketAddress(socketFile);

                serverChannel.socket().bind(socketAddress, getAcceptQueueSize());
                socketFile.deleteOnExit();

                addBean(serverChannel);
            }

            serverChannel.configureBlocking(true);
            addBean(serverChannel);

            _acceptChannel = serverChannel;
        }
    }


    @Override
    public boolean isOpen() {
        UnixServerSocketChannel channel = _acceptChannel;
        return channel != null && channel.isOpen();
    }

    /**
     * @return whether this connector uses a channel inherited from the JVM.
     * @see System#inheritedChannel()
     */
    public boolean isInheritChannel() {
        return _inheritChannel;
    }

    /**
     * <p>Sets whether this connector uses a channel inherited from the JVM.</p>
     * <p>If true, the connector first tries to inherit from a channel provided by the system.
     * If there is no inherited channel available, or if the inherited channel is not usable,
     * then it will fall back using {@link ServerSocketChannel}.</p>
     * <p>Use it with xinetd/inetd, to launch an instance of Jetty on demand. The port
     * used to access pages on the Jetty instance is the same as the port used to
     * launch Jetty.</p>
     *
     * @param inheritChannel whether this connector uses a channel inherited from the JVM.
     */
    public void setInheritChannel(boolean inheritChannel) {
        _inheritChannel = inheritChannel;
    }


    @Override
    public Future<Void> shutdown() {
        // TODO shutdown all the connections
        return super.shutdown();
    }

    @Override
    public void close() {
        UnixServerSocketChannel serverChannel = _acceptChannel;
        _acceptChannel = null;

        if (serverChannel != null) {
            removeBean(serverChannel);

            // If the interrupt did not close it, we should close it
            if (serverChannel.isOpen()) {
                try {
                    serverChannel.close();
                } catch (IOException e) {
                    LOG.warn(e);
                }
            }

            socketFile.delete();
        }
        // super.close();
        _localPort = -2;
    }

    @Override
    public void accept(int acceptorID) throws IOException {
        UnixServerSocketChannel serverChannel = _acceptChannel;
        if (serverChannel != null && serverChannel.isOpen()) {
            UnixSocketChannel channel = serverChannel.accept();
            channel.configureBlocking(false);
            _manager.accept(channel);
        }
    }


    public UnixSelectorManager getSelectorManager() {
        return _manager;
    }

    @Override
    public Object getTransport() {
        return _acceptChannel;
    }

    @Override
    @ManagedAttribute("local port")
    public int getLocalPort() {
        return _localPort;
    }

    protected UnixSelectChannelEndPoint newEndPoint(UnixSocketChannel channel, UnixSelectorManager.ManagedSelector selectSet, SelectionKey key) throws IOException {
        return new UnixSelectChannelEndPoint(channel, selectSet, key, getScheduler(), getIdleTimeout());
    }

    /**
     * @return the accept queue size
     */
    @ManagedAttribute("Accept Queue size")
    public int getAcceptQueueSize() {
        return _acceptQueueSize;
    }

    /**
     * @param acceptQueueSize the accept queue size (also known as accept backlog)
     */
    public void setAcceptQueueSize(int acceptQueueSize) {
        _acceptQueueSize = acceptQueueSize;
    }

    /**
     * @return whether the server socket reuses addresses
     * @see java.net.ServerSocket#getReuseAddress()
     */
    public boolean getReuseAddress() {
        return _reuseAddress;
    }

    /**
     * @param reuseAddress whether the server socket reuses addresses
     * @see java.net.ServerSocket#setReuseAddress(boolean)
     */
    public void setReuseAddress(boolean reuseAddress) {
        _reuseAddress = reuseAddress;
    }

    private final class ServerConnectorManager extends UnixSelectorManager {
        private ServerConnectorManager(Executor executor, Scheduler scheduler, int selectors) {
            super(executor, scheduler, selectors);
        }

        @Override
        protected UnixSelectChannelEndPoint newEndPoint(UnixSocketChannel channel, ManagedSelector selectSet, SelectionKey selectionKey) throws IOException {
            return UnixSocketConnector.this.newEndPoint(channel, selectSet, selectionKey);
        }

        @Override
        public Connection newConnection(UnixSocketChannel channel, EndPoint endpoint, Object attachment) throws IOException {
            return getDefaultConnectionFactory().newConnection(UnixSocketConnector.this, endpoint);
        }

        @Override
        protected void endPointOpened(EndPoint endpoint) {
            super.endPointOpened(endpoint);
            onEndPointOpened(endpoint);
        }

        @Override
        protected void endPointClosed(EndPoint endpoint) {
            onEndPointClosed(endpoint);
            super.endPointClosed(endpoint);
        }


    }

}
