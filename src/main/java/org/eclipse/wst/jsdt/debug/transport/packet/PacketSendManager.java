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
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.wst.jsdt.debug.transport.Connection;
import org.eclipse.wst.jsdt.debug.transport.exception.DisconnectedException;



/**
 * Default implementation of {@link PacketManager} for sending packets to the 
 * {@link VirtualMachine}
 * 
 * @since 1.0
 */
public final class PacketSendManager extends PacketManager {
	
	/** 
	 * List of packets to be sent to Virtual Machine 
	 */
	private final List outgoingPackets = new ArrayList();

	/**
	 * Create a new thread that send packets to the {@link VirtualMachine}.
	 * 
	 * @param connection the underlying connection to communicate on, <code>null</code> is not accepted
	 */
	public PacketSendManager(Connection connection) {
		super(connection);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.debug.internal.core.jsdi.connect.PacketManager#disconnect()
	 */
	public void disconnect() {
		super.disconnect();
		synchronized (outgoingPackets) {
			outgoingPackets.notifyAll();
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		while (!isDisconnected()) {
			try {
				sendAvailablePackets();
			}
			// in each case if the remote runtime fails, or has been interrupted, disconnect and force a clean up, don't wait for it to happen
			catch (InterruptedException e) {
				disconnect();
			} catch (InterruptedIOException e) {
				disconnect(e);
			} catch (IOException e) {
				disconnect(e);
			}
		}
	}

	/**
	 * Sends the given {@link Packet}.
	 * 
	 * @param packet the packet to send, <code>null</code> is not accepted
	 * 
	 * @throws DisconnectedException
	 */
	public void sendPacket(Packet packet) throws DisconnectedException {
		if(packet == null) {
			throw new IllegalArgumentException("You cannot send a null packet"); //$NON-NLS-1$
		}
		if (isDisconnected()) {
			throw new DisconnectedException("Runtime disconnected", getDisconnectedException()); //$NON-NLS-1$
		}
		synchronized (outgoingPackets) {
			// Add packet to list of packets to send.
			outgoingPackets.add(packet);
			// Notify PacketSendThread that data is available.
			outgoingPackets.notifyAll();
		}
	}

	/**
	 * Send available packets to the Virtual Machine.
	 */
	private void sendAvailablePackets() throws InterruptedException, IOException {
		Object[] packetsToSend;
		synchronized (outgoingPackets) {
			while (outgoingPackets.size() == 0) {
				outgoingPackets.wait();
			}
			packetsToSend = outgoingPackets.toArray();
			outgoingPackets.clear();
		}

		// Put available packets on Output Stream.
		for (int i = 0; i < packetsToSend.length; i++) {
			getConnection().writePacket((Packet) packetsToSend[i]);
		}
	}
}
