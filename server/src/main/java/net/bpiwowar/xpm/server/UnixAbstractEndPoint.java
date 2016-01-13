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

import jnr.unixsocket.UnixSocketAddress;
import org.eclipse.jetty.io.*;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.thread.Scheduler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeoutException;

public abstract class UnixAbstractEndPoint extends IdleTimeout implements EndPoint {
    private static final Logger LOG = Log.getLogger(UnixAbstractEndPoint.class);
    private final long _created = System.currentTimeMillis();
    private final UnixSocketAddress _local;
    private final UnixSocketAddress _remote;
    private final FillInterest _fillInterest = new FillInterest() {
        @Override
        protected boolean needsFill() throws IOException {
            return UnixAbstractEndPoint.this.needsFill();
        }
    };
    private final WriteFlusher _writeFlusher = new WriteFlusher(this) {
        @Override
        protected void onIncompleteFlushed() {
            UnixAbstractEndPoint.this.onIncompleteFlush();
        }
    };
    private volatile Connection _connection;

    protected UnixAbstractEndPoint(Scheduler scheduler, UnixSocketAddress local, UnixSocketAddress remote) {
        super(scheduler);
        _local = local;
        _remote = remote;
    }

    @Override
    public long getCreatedTimeStamp() {
        return _created;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return null;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return null;
    }

    @Override
    public Connection getConnection() {
        return _connection;
    }

    @Override
    public void setConnection(Connection connection) {
        _connection = connection;
    }

    @Override
    public void onOpen() {
        LOG.debug("onOpen {}", this);
        super.onOpen();
    }

    @Override
    public void onClose() {
        super.onClose();
        LOG.debug("onClose {}", this);
        _writeFlusher.onClose();
        _fillInterest.onClose();
    }

    @Override
    public void close() {
    }

    @Override
    public void fillInterested(Callback callback) throws IllegalStateException {
        notIdle();
        _fillInterest.register(callback);
    }

    @Override
    public void write(Callback callback, ByteBuffer... buffers) throws IllegalStateException {
        _writeFlusher.write(callback, buffers);
    }

    protected abstract void onIncompleteFlush();

    protected abstract boolean needsFill() throws IOException;

    protected FillInterest getFillInterest() {
        return _fillInterest;
    }

    protected WriteFlusher getWriteFlusher() {
        return _writeFlusher;
    }

    @Override
    protected void onIdleExpired(TimeoutException timeout) {
        boolean output_shutdown = isOutputShutdown();
        _fillInterest.onFail(timeout);
        _writeFlusher.onFail(timeout);
        if (output_shutdown)
            close();
    }

    @Override
    public String toString() {
        return String.format("%s@%x{%s<r-l>%s,o=%b,is=%b,os=%b,fi=%s,wf=%s,it=%d}{%s}",
                getClass().getSimpleName(),
                hashCode(),
                getRemoteAddress(),
                getLocalAddress(),
                isOpen(),
                isInputShutdown(),
                isOutputShutdown(),
                _fillInterest,
                _writeFlusher,
                getIdleTimeout(),
                getConnection());
    }
}
