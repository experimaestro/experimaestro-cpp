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
 * Describes the general form of an event {@link Packet}
 * 
 * @since 1.0
 */
public interface Event extends Packet {
	/**
	 * Returns the name of the event.
	 * <br><br>
	 * This method cannot return <code>null</code>.
	 * 
	 * @return the name of the event
	 */
	public String getEvent();

	/**
	 * Returns the body of the {@link Event}.<br>
	 * <br>
	 * This method cannot return <code>null</code>, if there is no body
	 * an empty {@link Map} is returned.
	 *  
	 * @return the body of the {@link Event} or an empty {@link Map} never <code>null</code>
	 */
	public Map getBody();
}
