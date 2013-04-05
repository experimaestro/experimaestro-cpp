/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.debug.internal.rhino.debugger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.wst.jsdt.debug.internal.rhino.transport.EventPacket;
import org.eclipse.wst.jsdt.debug.internal.rhino.transport.JSONConstants;
import org.eclipse.wst.jsdt.debug.internal.rhino.transport.RhinoRequest;
import org.eclipse.wst.jsdt.debug.internal.rhino.transport.RhinoResponse;
import org.eclipse.wst.jsdt.debug.internal.rhino.transport.RhinoTransportService;
import org.eclipse.wst.jsdt.debug.transport.Connection;
import org.eclipse.wst.jsdt.debug.transport.DebugSession;
import org.eclipse.wst.jsdt.debug.transport.ListenerKey;
import org.eclipse.wst.jsdt.debug.transport.TransportService;
import org.eclipse.wst.jsdt.debug.transport.exception.DisconnectedException;
import org.eclipse.wst.jsdt.debug.transport.exception.TimeoutException;

/**
 * Delegate for {@link DebugSession} communication
 * 
 * @since 1.1
 */
public class DebugSessionManager {

	public class DebugSessionThread extends Thread {

		private ListenerKey listenerKey;
		private Connection connection;
		private RequestHandler requestHandler;

		public DebugSessionThread(String name, RhinoDebuggerImpl debugger) {
			super(name);
			requestHandler = new RequestHandler(debugger);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Runnable#run()
		 */
		public void run() {
			try {
				listenerKey = transportService.startListening(address);
				while (!shutdown) {
					try {
						acceptConnection(10000);
					} catch (IOException e) {
						if (connection == null) {
							continue;
						}
					}
					while (!shutdown && connection.isOpen()) {
						try {
							RhinoRequest request = (RhinoRequest) debugSession.receive(JSONConstants.REQUEST, 1000);
							if (DEBUG)
								System.out.println(request);
							RhinoResponse response = requestHandler.handleRequest(request);
							if (DEBUG)
								System.out.println(response);
							debugSession.send(response);
						} catch (TimeoutException e) {
							// ignore
						} catch (DisconnectedException e) {
							break;
						}
					}
					closeConnection();
				}
			} catch (IOException e) {
				sendDeathEvent();
			} finally {
				try {
					if (listenerKey != null)
						transportService.stopListening(listenerKey);
				} catch (IOException e) {
					sendDeathEvent();
				}
			}
		}

		/**
		 * Close the active connection
		 * 
		 * @throws IOException
		 */
		private void closeConnection() throws IOException {
			if (connection != null) {
				setDebugSession(null);
				connection.close();
				connection = null;
			}
		}

		/**
		 * Waits for a connection for the given timeout
		 * 
		 * @param timeout
		 * @throws IOException
		 */
		private void acceptConnection(long timeout) throws IOException {
			if (connection == null) {
				connection = transportService.accept(listenerKey, timeout, timeout);
				setDebugSession(new DebugSession(connection));
			}
		}

	}

	private static boolean DEBUG = false;
	
	private static final String ADDRESS = "address"; //$NON-NLS-1$
	private static final String SOCKET = "socket"; //$NON-NLS-1$
	private static final String TRANSPORT = "transport"; //$NON-NLS-1$

	private final TransportService transportService;
	private final String address;
	private final boolean startSuspended;

	private DebugSession debugSession;
	private Thread debuggerThread;
	private volatile boolean shutdown = false;

	/**
	 * Constructor
	 * 
	 * @param transportService
	 * @param address
	 * @param startSuspended
	 */
	public DebugSessionManager(TransportService transportService, String address, boolean startSuspended, boolean debug) {
		this.transportService = transportService;
		this.address = address;
		this.startSuspended = startSuspended;
		DEBUG = debug;
	}

	/**
	 * Creates a new session manager
	 * 
	 * @param configString
	 * @return
	 */
	static DebugSessionManager create(String configString) {
		Map config = parseConfigString(configString);
		String transport = (String) config.get(TRANSPORT);
		if (!SOCKET.equals(transport)) {
			throw new IllegalArgumentException("Transport service must be 'socket': " + transport); //$NON-NLS-1$
		}
		TransportService parsedTransportService = new RhinoTransportService();
		String parsedAddress = (String) config.get(ADDRESS);
		String suspend = (String) config.get(JSONConstants.SUSPEND);
		boolean parsedStartSuspended = false;
		if (suspend != null) {
			parsedStartSuspended = (Boolean.valueOf(suspend).booleanValue() || suspend.trim().equalsIgnoreCase("y")); //$NON-NLS-1$
		}
		String debug = (String) config.get("trace"); //$NON-NLS-1$
		boolean debugging = false;
		if(debug != null) {
			debugging = (Boolean.valueOf(debug).booleanValue() || debug.trim().equalsIgnoreCase("y")); //$NON-NLS-1$
		}
		return new DebugSessionManager(parsedTransportService, parsedAddress, parsedStartSuspended, debugging);
	}

	/**
	 * Parses the command line configuration string
	 * 
	 * @param configString
	 * @return the map of command line arguments
	 */
	private static Map parseConfigString(String configString) {
		Map config = new HashMap();
		StringTokenizer tokenizer = new StringTokenizer(configString, ","); //$NON-NLS-1$
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			int equalsIndex = token.indexOf('=');
			if (equalsIndex == -1)
				config.put(token, null);
			else
				config.put(token.substring(0, equalsIndex), token.substring(equalsIndex + 1));
		}
		return config;
	}

	/**
	 * @return true if the <code>suspend=true</code> command line argument is
	 *         set
	 */
	public boolean isStartSuspended() {
		return startSuspended;
	}

	/**
	 * Returns if a {@link DebugSession} has successfully connected to this
	 * debugger.
	 * 
	 * @return <code>true</code> if the debugger has a connected
	 *         {@link DebugSession} <code>false</code> otherwise
	 */
	public synchronized boolean isConnected() {
		return debugSession != null;
	}

	/**
	 * Starts the debugger
	 */
	public synchronized void start(RhinoDebuggerImpl debugger) {
		debuggerThread = new DebugSessionThread("RhinoDebugger - RhinoRequest Handler", debugger); //$NON-NLS-1$
		debuggerThread.start();
		if (startSuspended) {
			try {
				wait(300000);
			} catch (InterruptedException e) {
				/* e.printStackTrace(); */
			}
			// TODO: We might want to check if debugSession is null and if so
			// call "stop" and throw an exception
		}
	}

	/**
	 * Stops the debugger
	 */
	public synchronized void stop() {
		shutdown = true;
		try {
			debuggerThread.interrupt();
			while(debuggerThread.isAlive()) {
				wait(1000);
			}
			debuggerThread.join();
		} catch (InterruptedException e) {
			/* e.printStackTrace(); */
		}
	}

	private synchronized void setDebugSession(DebugSession session) {
		if (debugSession != session) {
			if (debugSession != null) {
				debugSession.dispose();
			}
			debugSession = session;
			notify();
		}
	}

	/**
	 * Sends the given {@link EventPacket} using the underlying
	 * {@link DebugRuntime} and returns if it was sent successfully
	 * 
	 * @param event
	 * @return true if the event was sent successfully, false otherwise
	 */
	public synchronized boolean sendEvent(EventPacket event) {
		try {
			if (debugSession != null) {
				if (DEBUG) {
					System.out.println(event);
				}
				debugSession.send(event);
				return true;
			}
		} catch (DisconnectedException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Sends out an event that the debugger has died in an unexpected way.
	 * Debugger death can result from:
	 * <ul>
	 * <li>an {@link IOException} while the debugger is running</li>
	 * <li>an {@link InterruptedException} processing I/O</li>
	 * </ul>
	 */
	private void sendDeathEvent() {
		EventPacket event = new EventPacket(JSONConstants.VMDEATH);
		sendEvent(event);
	}
}
