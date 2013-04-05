/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.debug.transport;

import org.eclipse.wst.jsdt.debug.transport.exception.DisconnectedException;
import org.eclipse.wst.jsdt.debug.transport.exception.TimeoutException;
import org.eclipse.wst.jsdt.debug.transport.packet.Packet;
import org.eclipse.wst.jsdt.debug.transport.packet.PacketReceiveManager;
import org.eclipse.wst.jsdt.debug.transport.packet.PacketSendManager;
import org.eclipse.wst.jsdt.debug.transport.packet.Response;

/**
 * A {@link DebugSession} controls a {@link PacketSendManager} and {@link PacketReceiveManager}
 * that is used to communicate to a debug target using the underlying {@link Connection}
 * 
 * @since 1.0
 */
public class DebugSession {
	
	/**
	 * The default receive manager
	 */
	private final PacketReceiveManager packetReceiveManager;
	/**
	 * The default send manager
	 */
	private final PacketSendManager packetSendManager;

	/**
	 * Constructor
	 * 
	 * Starts the send / receive managers on the given {@link Connection}.
	 * 
	 * @param connection
	 */
	public DebugSession(Connection connection) {
		packetReceiveManager = new PacketReceiveManager(connection);
		Thread receiveThread = new Thread(packetReceiveManager, "Debug Session - Receive Manager"); //$NON-NLS-1$
		receiveThread.setDaemon(true);

		packetSendManager = new PacketSendManager(connection);
		Thread sendThread = new Thread(packetSendManager, "Debug Session - Send Manager"); //$NON-NLS-1$
		sendThread.setDaemon(true);

		packetReceiveManager.setPartnerThread(sendThread);
		packetSendManager.setPartnerThread(receiveThread);

		receiveThread.start();
		sendThread.start();
	}

	/**
	 * Stops the debug sessions and disconnects the send / receive managers
	 */
	public void dispose() {
		packetReceiveManager.disconnect();
		packetSendManager.disconnect();
	}

	/**
	 * Sends the given {@link Packet} using the underlying {@link PacketSendManager}.
	 * 
	 * @param packet the {@link Packet} to send, <code>null</code> is not accepted
	 * @throws DisconnectedException if the {@link PacketSendManager} has been disconnected
	 */
	public void send(Packet packet) throws DisconnectedException {
		if(packet == null) {
			throw new IllegalArgumentException("You cannot send a null request"); //$NON-NLS-1$
		}
		packetSendManager.sendPacket(packet);
	}
	
	/**
	 * Waits for the given timeout for a {@link Response} response in the given sequence 
	 * from the {@link PacketReceiveManager}.<br>
	 * <br>
	 * This method does not return <code>null</code> - one of the listed exceptions will be thrown 
	 * if a {@link Response} cannot be returned.
	 * 
	 * @param timeout the amount of time in milliseconds to wait to a {@link Response}
	 * @return a new {@link Response} from the {@link PacketReceiveManager} never <code>null</code>
	 * @throws TimeoutException if the timeout lapses with no {@link Response} returned
	 * @throws DisconnectedException if the {@link PacketReceiveManager} has been disconnected
	 */
	public Response receiveResponse(int requestSequence, int timeout) throws TimeoutException, DisconnectedException {
		return packetReceiveManager.getResponse(requestSequence, timeout);
	}

	/**
	 * Waits for the given timeout for a {@link Packet} from the {@link PacketReceiveManager}.
	 * If a {@link Packet} is returned it is guaranteed it can be cast to the type asked for.
	 * <br><br>
	 * This method does not return <code>null</code> - one of the listed exceptions will be thrown 
	 * if a {@link Packet} cannot be returned.
	 * 
	 * @param type the type of the packet to get
	 * @param timeout the amount of time in milliseconds to wait for a {@link Packet}
	 * 
	 * @return a new {@link Packet} from the {@link PacketReceiveManager} never <code>null</code>
	 * @throws TimeoutException if the timeout lapses with no {@link Packet} returned
	 * @throws DisconnectedException if the {@link PacketReceiveManager} has been disconnected
	 */
	public Packet receive(String type, int timeout) throws TimeoutException, DisconnectedException {
		return packetReceiveManager.getCommand(type, timeout);
	}
}
