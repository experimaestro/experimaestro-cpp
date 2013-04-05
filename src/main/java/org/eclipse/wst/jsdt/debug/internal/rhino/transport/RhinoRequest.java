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

import org.eclipse.wst.jsdt.debug.transport.packet.Request;

/**
 * Default request implementation using JSON
 * 
 * @since 1.0
 */
public class RhinoRequest extends RhinoPacket implements Request {

	private final String command;
	private final Map arguments = Collections.synchronizedMap(new HashMap());
	private static int currentSequence = 0;
	private final int sequence;
	
	/**
	 * Constructor
	 * 
	 * @param command the command, <code>null</code> is not accepted
	 */
	public RhinoRequest(String command) {
		super(JSONConstants.REQUEST);
		if(command == null) {
			throw new IllegalArgumentException("The request command kind cannot be null"); //$NON-NLS-1$
		}
		this.command = command.intern();
		this.sequence = nextSequence();
	}

	/**
	 * Constructor
	 * 
	 * @param json map of JSON attributes, <code>null</code> is not accepted
	 */
	public RhinoRequest(Map json) {
		super(json);
		if(json == null) {
			throw new IllegalArgumentException("The JSON map for a request packet cannot be null"); //$NON-NLS-1$
		}
		String packetCommand = (String) json.get(JSONConstants.COMMAND);
		this.command = packetCommand.intern();
		Map packetArguments = (Map) json.get(JSONConstants.ARGUMENTS);
		arguments.putAll(packetArguments);
		Number packetSeq = (Number) json.get(JSONConstants.SEQ);
		this.sequence = packetSeq.intValue();
	}

	/**
	 * @return a next value for the sequence
	 */
	private static synchronized int nextSequence() {
		return ++currentSequence;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.debug.transport.packet.Request#getSequence()
	 */
	public int getSequence() {
		return sequence;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.debug.transport.packet.Request#getCommand()
	 */
	public String getCommand() {
		return command;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.debug.transport.packet.Request#getArguments()
	 */
	public Map getArguments() {
		return arguments;
	}

	/**
	 * Sets the given argument in the JSON map.
	 * 
	 * @param key the key for the attribute, <code>null</code> is not accepted
	 * @param argument the value for the argument, <code>null</code> is not accepted
	 */
	public void setArgument(String key, Object argument) {
		if(key == null) {
			throw new IllegalArgumentException("The argument key cannot be null"); //$NON-NLS-1$
		}
		if(argument == null) {
			throw new IllegalArgumentException("A null argument is not allowed"); //$NON-NLS-1$
		}
		arguments.put(key, argument);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.debug.internal.rhino.transport.RhinoPacket#toJSON()
	 */
	public Map toJSON() {
		Map json = super.toJSON();
		json.put(JSONConstants.SEQ, new Integer(sequence));
		json.put(JSONConstants.COMMAND, command);
		json.put(JSONConstants.ARGUMENTS, arguments);
		return json;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("RhinoRequest: ").append(JSONUtil.write(toJSON())); //$NON-NLS-1$
		return buffer.toString();
	}
}
