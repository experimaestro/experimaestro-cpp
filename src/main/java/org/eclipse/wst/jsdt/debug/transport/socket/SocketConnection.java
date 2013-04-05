/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.debug.transport.socket;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.Socket;

import org.eclipse.wst.jsdt.debug.transport.Connection;
import org.eclipse.wst.jsdt.debug.transport.Constants;
import org.eclipse.wst.jsdt.debug.transport.packet.Packet;

/**
 * A specialized {@link Connection} that communicates using {@link Socket}s
 * 
 * @since 1.0
 */
public abstract class SocketConnection implements Connection {

	private Writer writer;
	private Reader reader;
	private Socket socket;

	/**
	 * Constructor
	 * 
	 * @param socket the underlying {@link Socket}, <code>null</code> is not accepted
	 * 
	 * @throws IOException
	 */
	public SocketConnection(Socket socket) throws IOException {
		if(socket == null) {
			throw new IllegalArgumentException("You cannot create a new SocketConnection on a null Socket"); //$NON-NLS-1$
		}
		this.socket = socket;
		writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), Constants.UTF_8));
		reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), Constants.UTF_8));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.debug.transport.Connection#isOpen()
	 */
	public boolean isOpen() {
		return !socket.isClosed();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.debug.transport.Connection#close()
	 */
	public void close() throws IOException {
		socket.close();
	}
	
	/**
	 * Returns the {@link Writer} used to write to the underlying {@link Socket}.
	 * <br><br>
	 * This method will never return <code>null</code> 
	 * 
	 * @return the writer for the socket, never <code>null</code>
	 */
	public Writer getWriter() {
		return writer;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.debug.transport.Connection#writePacket(org.eclipse.wst.jsdt.debug.transport.packet.Packet)
	 */
	public abstract void writePacket(Packet packet) throws IOException;

	/**
	 * Returns the {@link Reader} used to read from the underlying {@link Socket}.
	 * <br><br>
	 * This method will never return <code>null</code> 
	 * 
	 * @return the reader for the socket, never <code>null</code>
	 */
	public Reader getReader() {
		return reader;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.debug.transport.Connection#readPacket()
	 */
	public abstract Packet readPacket() throws IOException;
}
