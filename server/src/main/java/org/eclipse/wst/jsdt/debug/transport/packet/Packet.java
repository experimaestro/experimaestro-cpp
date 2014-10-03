/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.debug.transport.packet;

import java.util.Map;

/**
 * Describes the general form of a packet
 * 
 * @since 1.0
 */
public interface Packet {

	/**
	 * Returns the type of this packet. Where a <code>type</code> can be any string describing the type of
	 * packet this is. For example a type could be <code>request</code> or <code>response</code>.<br>
	 * <br>
	 * This method cannot return <code>null</code>
	 * 
	 * @return the type, never <code>null</code>
	 */
	public String getType();
	
	/**
	 * Returns the JSON representation of this packet. 
	 * Implementors are free to compose the JSON however they wish.<br>
	 * <br>
	 * This method cannot return <code>null</code>
	 * @return the composed JSON map
	 */
	public Map toJSON();
}
