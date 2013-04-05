/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.debug.transport.packet;

import java.io.IOException;

import org.eclipse.wst.jsdt.debug.transport.Connection;

/**
 * Default manager for sending / receiving packets to / from the connected {@link VirtualMachine}
 * 
 * @since 1.0
 */
public abstract class PacketManager implements Runnable {

	/**
	 * Connector that performs IO to Virtual Machine.
	 */
	private final Connection connection;
	/**
	 * Thread that handles the communication the other way (e.g. if we are sending, the receiving thread).
	 */
	private volatile Thread partnerThread;
	/**
	 * The disconnected exception, if there is one
	 */
	private volatile IOException disconnectedException;

	/**
	 * Constructor
	 * 
	 * @param connection the connection to communicate on, <code>null</code> is not accepted
	 */
	protected PacketManager(Connection connection) {
		if(connection == null) {
			throw new IllegalArgumentException("You cannot create a new PacketManager with a null Connection"); //$NON-NLS-1$
		}
		this.connection = connection;
	}

	/**
	 * Returns the {@link Connection} this manager is communicating on.<br>
	 * <br>
	 * This method cannot return <code>null</code>
	 * 
	 * @return the backing {@link Connection} to the {@link VirtualMachine}
	 */
	public Connection getConnection() {
		return this.connection;
	}

	/**
	 * Used to indicate that an IO exception occurred, closes the {@link Connection} 
	 * to the {@link VirtualMachine}.
	 * 
	 * @param disconnectedException the IOException that occurred or <code>null</code>
	 */
	public void disconnect(IOException exception) {
		this.disconnectedException = exception;
		disconnect();
	}

	/**
	 * Closes the {@link Connection} to the {@link VirtualMachine}.
	 */
	public void disconnect() {
		try {
			this.connection.close();
		} catch (IOException e) {
			this.disconnectedException = e;
		}
		if (this.partnerThread != null) {
			this.partnerThread.interrupt();
		}
	}

	/**
	 * Returns if the manager is disconnected or not.
	 * 
	 * @return <code>true</code> if the manager is disconnected false otherwise
	 */
	public boolean isDisconnected() {
		return this.connection == null || !this.connection.isOpen();
	}

	/**
	 * Returns the underlying {@link IOException} from a disconnect.<br>
	 * <br>
	 * This method can return <code>null</code> if the manager has not been disconnected
	 * 
	 * @return the underlying {@link IOException} from a disconnect or <code>null</code> if none
	 */
	public IOException getDisconnectedException() {
		return this.disconnectedException;
	}

	/**
	 * Assigns thread of partner, to be notified if we have an {@link IOException}.
	 * 
	 * @param thread the new partner thread, <code>null</code> is not accepted
	 */
	public void setPartnerThread(Thread thread) {
		if(thread == null) {
			throw new IllegalArgumentException("You cannot send a null partner thread on the PacketManager"); //$NON-NLS-1$
		}
		this.partnerThread = thread;
	}
}
