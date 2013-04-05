/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.debug.transport;

import java.net.Socket;

import org.eclipse.wst.jsdt.debug.transport.socket.SocketListenerKey;

/**
 * Describes a key for a {@link TransportService} that uniquely identifies an address to communicate with.
 * <br><br>
 * This interface is intended to be implemented by clients. A default
 * implementation for {@link Socket} communication is provided in the class
 * {@link SocketListenerKey}.
 * 
 * @since 1.0
 */
public interface ListenerKey {
	/**
	 * Returns the address string for the listener.
	 * <br><br>
	 * This method cannot return <code>null</code>
	 * 
	 * @return the address to try and communicate with, never <code>null</code>
	 */
	public String address();
}