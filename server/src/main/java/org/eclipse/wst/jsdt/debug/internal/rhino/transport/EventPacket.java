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


/**
 * An {@link EventPacket} is a specialized {@link RhinoPacket}
 * that only handles <code>event</code> data.
 * 
 * @since 1.0
 */
public class EventPacket extends RhinoPacket {

	public static final String TYPE = JSONConstants.EVENT;
	private final String event;
	private final Map body = Collections.synchronizedMap(new HashMap());

	/**
	 * Constructor
	 * @param event
	 */
	public EventPacket(String event) {
		super(TYPE);
		this.event = event.intern();
	}

	/**
	 * Constructor
	 * @param json
	 */
	public EventPacket(Map json) {
		super(json);
		String packetEvent = (String) json.get(JSONConstants.EVENT);
		event = packetEvent.intern();
		Map packetBody = (Map) json.get(JSONConstants.BODY);
		body.putAll(packetBody);
	}

	/**
	 * Returns the underlying event data
	 * @return the event data
	 */
	public String getEvent() {
		return event;
	}

	/**
	 * Returns the underlying body of the event packet
	 * @return the body of the packet
	 */
	public Map getBody() {
		return body;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.debug.internal.core.jsdi.connect.Packet#toJSON()
	 */
	public Map toJSON() {
		Map json = super.toJSON();
		json.put(JSONConstants.EVENT, event);
		json.put(JSONConstants.BODY, body);
		return json;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("EventPacket: ").append(JSONUtil.write(toJSON())); //$NON-NLS-1$
		return buffer.toString();
	}
}
