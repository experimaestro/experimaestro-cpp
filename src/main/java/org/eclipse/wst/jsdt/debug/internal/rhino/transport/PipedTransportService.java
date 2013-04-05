/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.debug.internal.rhino.transport;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.wst.jsdt.debug.transport.Connection;
import org.eclipse.wst.jsdt.debug.transport.ListenerKey;
import org.eclipse.wst.jsdt.debug.transport.TransportService;

/**
 * {@link TransportService} specialization for a {@link PipedConnection}
 * 
 * @since 1.0
 */
public class PipedTransportService implements TransportService {

	/**
	 * Key specialized for a {@link PipedTransportService}
	 */
	public static class PipedListenerKey implements ListenerKey {

		private String address;

		public PipedListenerKey(String address) {
			this.address = address;
		}

		public String address() {
			return address;
		}

		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((address == null) ? 0 : address.hashCode());
			return result;
		}

		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			PipedListenerKey other = (PipedListenerKey) obj;
			if (address == null) {
				if (other.address != null)
					return false;
			} else if (!address.equals(other.address))
				return false;
			return true;
		}
	}

	Map listeners = new HashMap();

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.debug.transport.TransportService#startListening(java.lang.String)
	 */
	public synchronized ListenerKey startListening(String address) throws IOException {
		ListenerKey key = new PipedListenerKey(address == null ? Constants.EMPTY_STRING : address);
		listeners.put(key, null);
		return key;
	};

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.debug.transport.TransportService#stopListening(org.eclipse.wst.jsdt.debug.transport.ListenerKey)
	 */
	public void stopListening(ListenerKey key) throws IOException {
		synchronized (key) {
			if (listeners.containsKey(key)) {
				key.notify();
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.debug.transport.TransportService#accept(org.eclipse.wst.jsdt.debug.transport.ListenerKey, long, long)
	 */
	public Connection accept(ListenerKey key, long attachTimeout, long handshakeTimeout) throws IOException {
		long timeout = attachTimeout;
		if (timeout > 0) {
			if (timeout > Integer.MAX_VALUE) {
				timeout = Integer.MAX_VALUE; // approximately 25 days!
			}
		}

		synchronized (listeners) {
			if (!listeners.containsKey(key))
				throw new IllegalStateException("not listening"); //$NON-NLS-1$

			if (listeners.get(key) != null)
				throw new IllegalStateException("PipedTransport only accepts one accept at a time"); //$NON-NLS-1$

			PipedInputStream serveris = new PipedInputStream();
			PipedOutputStream clientos = new PipedOutputStream();
			serveris.connect(clientos);

			PipedOutputStream serveros = new PipedOutputStream();
			PipedInputStream clientis = new PipedInputStream();
			serveros.connect(clientis);

			listeners.put(key, new PipedConnection(clientis, clientos));
			listeners.notifyAll();
			long startTime = System.currentTimeMillis();
			while (true) {
				try {
					listeners.wait(timeout);
				} catch (InterruptedException e) {
					throw new IOException("accept failed: interrupted"); //$NON-NLS-1$
				}
				if (!listeners.containsKey(key))
					throw new IOException("accept failed: stopped listening"); //$NON-NLS-1$

				if (listeners.get(key) != null) {
					if (System.currentTimeMillis() - startTime > timeout) {
						listeners.put(key, null);
						throw new IOException("accept failed: timed out"); //$NON-NLS-1$
					}
					continue;
				}
				return new PipedConnection(serveris, serveros);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.debug.transport.TransportService#attach(java.lang.String, long, long)
	 */
	public Connection attach(String address, long attachTimeout, long handshakeTimeout) throws IOException {
		ListenerKey key = new PipedListenerKey(address == null ? Constants.EMPTY_STRING : address);
		Connection connection;
		long startTime = System.currentTimeMillis();
		synchronized (listeners) {
			connection = (Connection) listeners.get(key);
			while (connection == null) {
				if (System.currentTimeMillis() - startTime > attachTimeout)
					throw new IOException("attach failed: timed out"); //$NON-NLS-1$
				try {
					listeners.wait(attachTimeout);
				} catch (InterruptedException e) {
					throw new IOException("attach failed: interrupted"); //$NON-NLS-1$
				}
				connection = (Connection) listeners.get(key);
			}
			listeners.put(key, null);
			listeners.notifyAll();
		}
		return connection;
	}
}
