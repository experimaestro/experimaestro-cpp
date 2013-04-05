/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.debug.internal.rhino.transport;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.wst.jsdt.debug.transport.packet.Packet;

/**
 * Abstract description of a packet for sending / receiving information to the debug client
 * using JSON
 *  
 *  @since 1.0
 */
abstract public class RhinoPacket implements Packet {

	private final String type;

	/**
	 * Constructor
	 * 
	 * @param type the type for the {@link RhinoPacket} <code>null</code> is not accepted
	 */
	protected RhinoPacket(String type) {
		if(type == null) {
			throw new IllegalArgumentException("The type for a packet cannot be null"); //$NON-NLS-1$
		}
		this.type = type.intern();
	}

	/**
	 * Constructor
	 * 
	 * @param json the pre-composed map of attributes for the packet, <code>null</code> is not accepted
	 */
	protected RhinoPacket(Map json) {
		if(json == null) {
			throw new IllegalArgumentException("The JSON map for a packet cannot be null"); //$NON-NLS-1$
		}
		String packetType = (String) json.get(JSONConstants.TYPE);
		this.type = packetType.intern();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.debug.transport.packet.Packet#getType()
	 */
	public String getType() {
		return type;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.debug.transport.packet.Packet#toJSON()
	 */
	public Map toJSON() {
		Map json = new HashMap();
		json.put(JSONConstants.TYPE, type);
		return json;
	}

	/**
	 * Returns the type from the given JSON map.<br>
	 * <br>
	 * This method can return <code>null</code> if the map is not correctly
	 * formed.
	 * 
	 * @param json the JSON map, <code>null</code> is not accepted
	 * @return the type from the JSON map or <code>null</code>
	 */
	public static String getType(Map json) {
		if(json == null) {
			throw new IllegalArgumentException("A null JSON map is not allowed when trying to get the packet type"); //$NON-NLS-1$
		}
		return (String) json.get(JSONConstants.TYPE);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("RhinoPacket: ").append(JSONUtil.write(toJSON())); //$NON-NLS-1$
		return buffer.toString();
	}
}
