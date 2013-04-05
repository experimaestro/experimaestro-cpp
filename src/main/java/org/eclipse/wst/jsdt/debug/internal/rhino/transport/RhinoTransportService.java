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
import java.net.Socket;

import org.eclipse.wst.jsdt.debug.transport.Connection;
import org.eclipse.wst.jsdt.debug.transport.packet.Packet;
import org.eclipse.wst.jsdt.debug.transport.socket.SocketConnection;
import org.eclipse.wst.jsdt.debug.transport.socket.SocketTransportService;

/**
 * Implementation of a transport service that using a {@link Socket} for communication
 * 
 * @since 1.0
 */
public class RhinoTransportService extends SocketTransportService {

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.debug.transport.socket.SocketTransportService#getConnection(java.net.Socket)
	 */
	public SocketConnection getConnection(Socket socket) throws IOException {
		return new RhinoSocketConnection(socket);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.debug.transport.socket.SocketTransportService#handleAccept(org.eclipse.wst.jsdt.debug.transport.Connection)
	 */
	public void handleAccept(Connection connection) throws IOException {
		Packet packet = connection.readPacket();
		if (packet instanceof RhinoRequest) {
			RhinoRequest request = (RhinoRequest) packet;
			if (!request.getCommand().equals(JSONConstants.CONNECT)) {
				throw new IOException("failure establishing connection"); //$NON-NLS-1$
			}
			RhinoResponse response = new RhinoResponse(request.getSequence(), request.getCommand());
			connection.writePacket(response);
			return;
		}
		throw new IOException("failure establishing connection"); //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.debug.transport.socket.SocketTransportService#handleAttach(org.eclipse.wst.jsdt.debug.transport.Connection)
	 */
	public void handleAttach(Connection connection) throws IOException {
		RhinoRequest request = new RhinoRequest(JSONConstants.CONNECT);
		connection.writePacket(request);
		Packet packet = connection.readPacket();
		if (packet instanceof RhinoResponse) {
			RhinoResponse response = (RhinoResponse) packet;
			if (!response.getCommand().equals(JSONConstants.CONNECT) || response.getRequestSequence() != request.getSequence() || !response.isSuccess() || !response.isRunning()) {
				throw new IOException("failure establishing connection"); //$NON-NLS-1$
			}
			return;
		}
		throw new IOException("failure establishing connection"); //$NON-NLS-1$
	}
}
