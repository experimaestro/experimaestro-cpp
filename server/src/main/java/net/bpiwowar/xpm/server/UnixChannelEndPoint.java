package net.bpiwowar.xpm.server;

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

import jnr.unixsocket.UnixSocketChannel;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.thread.Scheduler;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.GatheringByteChannel;

/**
 * Channel End Point.
 * <p>Holds the channel and socket for an NIO endpoint.
 */
public class UnixChannelEndPoint extends UnixAbstractEndPoint //implements SocketBased
{
    private static final Logger LOG = Log.getLogger(UnixChannelEndPoint.class);

    private final ByteChannel _channel;
    private final UnixSocketChannel _socket;
    private volatile boolean _ishut;
    private volatile boolean _oshut;

    public UnixChannelEndPoint(Scheduler scheduler, UnixSocketChannel channel) {
        super(scheduler,
                channel.getLocalSocketAddress(),
                channel.getRemoteSocketAddress()
        );
        _channel = channel;
        _socket = channel;
    }

    @Override
    public boolean isOpen() {
        return _channel.isOpen();
    }

    protected void shutdownInput() {
        LOG.debug("ishut {}", this);
        _ishut = true;
        if (_oshut)
            close();
    }

    @Override
    public void shutdownOutput() {
        LOG.debug("oshut {}", this);
        _oshut = true;
        if (_channel.isOpen()) {
            try {
//                if (!_socket.isOutputShutdown())
                _socket.shutdownOutput();
            } catch (IOException e) {
                LOG.debug(e);
            } finally {
                if (_ishut) {
                    close();
                }
            }
        }
    }

    @Override
    public boolean isOutputShutdown() {
        return _oshut || !_channel.isOpen(); // || _socket.isOutputShutdown();
    }

    @Override
    public boolean isInputShutdown() {
        return _ishut || !_channel.isOpen(); // || _socket.isInputShutdown();
    }

    @Override
    public void close() {
        LOG.debug("close {}", this);
        try {
            _channel.close();
        } catch (IOException e) {
            LOG.debug(e);
        } finally {
            _ishut = true;
            _oshut = true;
        }
    }

    @Override
    public int fill(ByteBuffer buffer) throws IOException {
        if (_ishut)
            return -1;

        int pos = BufferUtil.flipToFill(buffer);
        try {
            int filled = _channel.read(buffer);
            LOG.debug("filled {} {}", filled, this);

            if (filled > 0)
                notIdle();
            else if (filled == -1)
                shutdownInput();

            return filled;
        } catch (IOException e) {
            LOG.debug(e);
            shutdownInput();
            return -1;
        } finally {
            BufferUtil.flipToFlush(buffer, pos);
        }
    }

    @Override
    public boolean flush(ByteBuffer... buffers) throws IOException {
        int flushed = 0;
        try {
            if (buffers.length == 1)
                flushed = _channel.write(buffers[0]);
            else if (buffers.length > 1 && _channel instanceof GatheringByteChannel)
                flushed = (int) ((GatheringByteChannel) _channel).write(buffers, 0, buffers.length);
            else {
                for (ByteBuffer b : buffers) {
                    if (b.hasRemaining()) {
                        int l = _channel.write(b);
                        if (l > 0)
                            flushed += l;
                        if (b.hasRemaining())
                            break;
                    }
                }
            }
            LOG.debug("flushed {} {}", flushed, this);
        } catch (IOException e) {
            throw new EofException(e);
        }

        if (flushed > 0)
            notIdle();

        for (ByteBuffer b : buffers)
            if (!BufferUtil.isEmpty(b))
                return false;

        return true;
    }

    public ByteChannel getChannel() {
        return _channel;
    }

    @Override
    public Object getTransport() {
        return _channel;
    }

    @Override
    public boolean tryFillInterested(Callback callback) {
        return false;
    }

    @Override
    public boolean isFillInterested() {
        return false;
    }

    @Override
    public boolean isOptimizedForDirectBuffers() {
        return false;
    }

    @Override
    public void upgrade(Connection newConnection) {

    }

    //    @Override
    public Socket getSocket() {
        return null;
//        return _socket;
    }

    @Override
    protected void onIncompleteFlush() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean needsFill() throws IOException {
        throw new UnsupportedOperationException();
    }
}
