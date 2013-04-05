/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.debug.transport;

import java.io.IOException;
import java.net.Socket;

import org.eclipse.wst.jsdt.debug.transport.socket.SocketTransportService;

/**
 * A transport service is used to attach and / or listen to a 
 * {@link Connection}.
 * <br><br>
 * This interface is intended to be implemented by clients.
 * A default implementation for {@link Socket} communication is provided in
 * the class {@link SocketTransportService}.
 * 
 * @since 1.0
 */
public interface TransportService {

	/**
	 * Registers that this service should begin listening to the given address
	 * 
	 * @param address the address to listen on - e.g. localhost:12000
	 * @return the key that uniquely identifies this service
	 * @throws IOException
	 */
	public ListenerKey startListening(String address) throws IOException;

	/**
	 * Stops listening for the given key
	 * 
	 * @param listenerKey
	 * @throws IOException
	 */
	public void stopListening(ListenerKey listenerKey) throws IOException;

	/**
	 * Listens for a {@link Connection} to be made to and accepts it. The method can block until a {@link Connection} is made.
	 * 
	 * @param listenerKey
	 * @param attachTimeout
	 * @param handshakeTimeout
	 * @return the resulting {@link Connection}
	 * @throws IOException
	 */
	public Connection accept(ListenerKey listenerKey, long attachTimeout, long handshakeTimeout) throws IOException;

	/**
	 * Attaches to the given address and returns the resulting {@link Connection}. This method can block until a {@link Connection} is made
	 * 
	 * @param address
	 * @param attachTimeout
	 * @param handshakeTimeout
	 * @return the resulting {@link Connection}
	 * @throws IOException
	 */
	public Connection attach(String address, long attachTimeout, long handshakeTimeout) throws IOException;

}