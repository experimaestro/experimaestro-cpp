/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.debug.internal.rhino.transport;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;

import org.eclipse.wst.jsdt.debug.transport.Connection;
import org.eclipse.wst.jsdt.debug.transport.packet.Packet;

/**
 * A {@link Connection} implementation that uses streams for communication
 * 
 * @since 1.0
 */
public class PipedConnection implements Connection {

	private Writer writer;
	private Reader reader;
	private boolean open = true;

	/**
	 * Constructor
	 * 
	 * @param is the {@link InputStream} to read from, <code>null</code> is not accepted
	 * @param os the {@link OutputStream} to write to, <code>null</code> is not accepted
	 * @throws IOException
	 */
	public PipedConnection(InputStream is, OutputStream os) throws IOException {
		if(is == null) {
			throw new IllegalArgumentException("You cannot create a new PipedConnection on a null InputStream"); //$NON-NLS-1$
		}
		if(os == null) {
			throw new IllegalArgumentException("You cannot create a new PipedConnection on a null OutputStream"); //$NON-NLS-1$
		}
		writer = new BufferedWriter(new OutputStreamWriter(os, Constants.UTF_8));
		reader = new BufferedReader(new InputStreamReader(is, Constants.UTF_8));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.debug.transport.Connection#isOpen()
	 */
	public synchronized boolean isOpen() {
		return open;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.debug.transport.Connection#close()
	 */
	public synchronized void close() throws IOException {
		open = false;
		writer.close();
		reader.close();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.debug.transport.Connection#writePacket(org.eclipse.wst.jsdt.debug.transport.packet.Packet)
	 */
	public void writePacket(Packet packet) throws IOException {
		String jsonString = JSONUtil.write(packet.toJSON());
		String count = Integer.toString(jsonString.length());
		writer.write(count);
		writer.write('\r');
		writer.write('\n');
		writer.write(jsonString);
		writer.flush();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.debug.transport.Connection#readPacket()
	 */
	public Packet readPacket() throws IOException {
		StringBuffer buffer = new StringBuffer();
		int c;
		while ((c = reader.read()) != -1) {
			if (c == '\r')
				break;
			buffer.append((char) c);
			if (buffer.length() > 10) {
				throw new IOException("Invalid content length: " + buffer.toString()); //$NON-NLS-1$
			}
		}

		int length = 0;
		try {
			length = Integer.parseInt(buffer.toString());
		} catch (NumberFormatException e) {
			throw new IOException("Failed to parse content length: " + buffer.toString()); //$NON-NLS-1$
		}

		if ('\r' != c || '\n' != (char) reader.read())
			throw new IOException("Missing CRLF after content length"); //$NON-NLS-1$

		char[] message = new char[length];
		int n = 0;
		int off = 0;
		while (n < length) {
			int count = reader.read(message, off + n, length - n);
			if (count < 0)
				throw new EOFException();
			n += count;
		}

		Map json = (Map) JSONUtil.read(new String(message));
		String type = RhinoPacket.getType(json);
		if (EventPacket.TYPE.equals(type))
			return new EventPacket(json);
		if (JSONConstants.REQUEST.equals(type))
			return new RhinoRequest(json);
		if (JSONConstants.RESPONSE.equals(type))
			return new RhinoResponse(json);

		throw new IOException("Unknown packet type: " + type); //$NON-NLS-1$
	}
}
