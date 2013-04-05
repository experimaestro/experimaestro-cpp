/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.debug.internal.rhino.transport;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.wst.jsdt.debug.transport.packet.Response;

/**
 * Default {@link RhinoResponse} implementation using JSON
 * 
 * @since 1.0
 */
public class RhinoResponse extends RhinoPacket implements Response {

	private final String command;
	private final int requestSequence;
	private final Map body = Collections.synchronizedMap(new HashMap());
	private volatile boolean success = true;
	private volatile boolean running = true;
	private volatile String message;

	/**
	 * Constructor
	 * 
	 * @param requestSequence the sequence
	 * @param command the command, <code>null</code> is not accepted
	 */
	public RhinoResponse(int requestSequence, String command) {
		super(JSONConstants.RESPONSE);
		if(command == null) {
			throw new IllegalArgumentException("The command string for a response packet cannot be null"); //$NON-NLS-1$
		}
		this.requestSequence = requestSequence;
		this.command = command.intern();
	}

	/**
	 * Constructor
	 * 
	 * @param json the JSON map for a response, <code>null</code> is not accepted
	 */
	public RhinoResponse(Map json) {
		super(json);
		Number packetRequestSeq = (Number) json.get(JSONConstants.REQUEST_SEQ);
		requestSequence = packetRequestSeq.intValue();

		String packetCommand = (String) json.get(JSONConstants.COMMAND);
		command = packetCommand.intern();

		Map packetBody = (Map) json.get(JSONConstants.BODY);
		body.putAll(packetBody);

		Boolean packetSuccess = (Boolean) json.get(JSONConstants.SUCCESS);
		success = packetSuccess.booleanValue();

		Boolean packetRunning = (Boolean) json.get(JSONConstants.RUNNING);
		running = packetRunning.booleanValue();

		message = (String) json.get(JSONConstants.MESSAGE);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.debug.transport.packet.Response#getRequestSequence()
	 */
	public int getRequestSequence() {
		return requestSequence;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.debug.transport.packet.Response#getCommand()
	 */
	public String getCommand() {
		return command;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.debug.transport.packet.Response#getBody()
	 */
	public Map getBody() {
		return body;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.debug.transport.packet.Response#isSuccess()
	 */
	public boolean isSuccess() {
		return success;
	}

	/**
	 * Set the success flag for the response
	 * 
	 * @param success the new success flag
	 */
	public void setSuccess(boolean success) {
		this.success = success;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.debug.transport.packet.Response#isRunning()
	 */
	public boolean isRunning() {
		return running;
	}

	/**
	 * Sets the running state of the underlying command
	 * 
	 * @param running the new running state for the underlying command
	 */
	public void setRunning(boolean running) {
		this.running = running;
	}

	/**
	 * Returns the status message for this {@link RhinoResponse}.<br>
	 * <br>
	 * This method can return <code>null</code>
	 * 
	 * @return the status message for this {@link RhinoResponse} or <code>null</code>
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Set the status message for this {@link RhinoResponse}
	 * 
	 * @param message the new message, <code>null</code> is accepted
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.debug.internal.rhino.transport.RhinoPacket#toJSON()
	 */
	public Map toJSON() {
		Map json = super.toJSON();
		json.put(JSONConstants.REQUEST_SEQ, new Integer(requestSequence));
		json.put(JSONConstants.COMMAND, command);
		json.put(JSONConstants.BODY, body);
		json.put(JSONConstants.SUCCESS, new Boolean(success));
		json.put(JSONConstants.RUNNING, new Boolean(running));
		if (message != null) {
			json.put(JSONConstants.MESSAGE, message);
		}
		return json;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("RhinoResponse: ").append(JSONUtil.write(toJSON())); //$NON-NLS-1$
		return buffer.toString();
	}
}
