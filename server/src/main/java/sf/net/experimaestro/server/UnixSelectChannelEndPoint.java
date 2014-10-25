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

import jnr.unixsocket.UnixSocketChannel;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.thread.Scheduler;

import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An ChannelEndpoint that can be scheduled by {@link UnixSelectorManager}.
 */
public class UnixSelectChannelEndPoint extends UnixChannelEndPoint implements UnixSelectorManager.SelectableEndPoint {
    public static final Logger LOG = Log.getLogger(UnixSelectChannelEndPoint.class);

    private final Runnable _updateTask = new Runnable() {
        @Override
        public void run() {
            try {
                if (getChannel().isOpen()) {
                    int oldInterestOps = _key.interestOps();
                    int newInterestOps = _interestOps.get();
                    if (newInterestOps != oldInterestOps)
                        setKeyInterests(oldInterestOps, newInterestOps);
                }
            } catch (CancelledKeyException x) {
                LOG.debug("Ignoring key update for concurrently closed channel {}", this);
                close();
            } catch (Exception x) {
                LOG.warn("Ignoring key update for " + this, x);
                close();
            }
        }
    };

    /**
     */
    private final AtomicBoolean _open = new AtomicBoolean();
    private final UnixSelectorManager.ManagedSelector _selector;
    private final SelectionKey _key;
    /**
     * The desired value for {@link SelectionKey#interestOps()}
     */
    private final AtomicInteger _interestOps = new AtomicInteger();

    public UnixSelectChannelEndPoint(UnixSocketChannel channel, UnixSelectorManager.ManagedSelector selector, SelectionKey key, Scheduler scheduler, long idleTimeout) {
        super(scheduler, channel);
        _selector = selector;
        _key = key;
        setIdleTimeout(idleTimeout);
    }

    @Override
    protected boolean needsFill() {
        updateLocalInterests(SelectionKey.OP_READ, true);
        return false;
    }

    @Override
    protected void onIncompleteFlush() {
        updateLocalInterests(SelectionKey.OP_WRITE, true);
    }

    @Override
    public void onSelected() {
        assert _selector.isSelectorThread();
        int oldInterestOps = _key.interestOps();
        int readyOps = _key.readyOps();
        int newInterestOps = oldInterestOps & ~readyOps;
        setKeyInterests(oldInterestOps, newInterestOps);
        updateLocalInterests(readyOps, false);
        if (_key.isReadable())
            getFillInterest().fillable();
        if (_key.isWritable())
            getWriteFlusher().completeWrite();
    }


    private void updateLocalInterests(int operation, boolean add) {
        while (true) {
            int oldInterestOps = _interestOps.get();
            int newInterestOps;
            if (add)
                newInterestOps = oldInterestOps | operation;
            else
                newInterestOps = oldInterestOps & ~operation;

            if (isInputShutdown())
                newInterestOps &= ~SelectionKey.OP_READ;
            if (isOutputShutdown())
                newInterestOps &= ~SelectionKey.OP_WRITE;

            if (newInterestOps != oldInterestOps) {
                if (_interestOps.compareAndSet(oldInterestOps, newInterestOps)) {
                    LOG.debug("Local interests updated {} -> {} for {}", oldInterestOps, newInterestOps, this);
                    _selector.submit(_updateTask);
                } else {
                    LOG.debug("Local interests update conflict: now {}, was {}, attempted {} for {}", _interestOps.get(), oldInterestOps, newInterestOps, this);
                    continue;
                }
            } else {
                LOG.debug("Ignoring local interests update {} -> {} for {}", oldInterestOps, newInterestOps, this);
            }
            break;
        }
    }


    private void setKeyInterests(int oldInterestOps, int newInterestOps) {
        assert _selector.isSelectorThread();
        LOG.debug("Key interests updated {} -> {}", oldInterestOps, newInterestOps);
        _key.interestOps(newInterestOps);
    }

    @Override
    public void close() {
        if (_open.compareAndSet(true, false)) {
            super.close();
            _selector.destroyEndPoint(this);
        }
    }

    @Override
    public boolean isOpen() {
        // We cannot rely on super.isOpen(), because there is a race between calls to close() and isOpen():
        // a thread may call close(), which flips the boolean but has not yet called super.close(), and
        // another thread calls isOpen() which would return true - wrong - if based on super.isOpen().
        return _open.get();
    }

    @Override
    public void onOpen() {
        if (_open.compareAndSet(false, true))
            super.onOpen();
    }

    @Override
    public String toString() {
        // Do NOT use synchronized (this)
        // because it's very easy to deadlock when debugging is enabled.
        // We do a best effort to print the right toString() and that's it.
        try {
            boolean valid = _key != null && _key.isValid();
            int keyInterests = valid ? _key.interestOps() : -1;
            int keyReadiness = valid ? _key.readyOps() : -1;
            return String.format("%s{io=%d,kio=%d,kro=%d}",
                    super.toString(),
                    _interestOps.get(),
                    keyInterests,
                    keyReadiness);
        } catch (CancelledKeyException x) {
            return String.format("%s{io=%s,kio=-2,kro=-2}", super.toString(), _interestOps.get());
        }
    }
}
