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
 * Describes the general form of a response {@link Packet}
 * 
 * @since 1.0
 */
public interface Response extends Packet {

	/**
	 * Returns the name of the original issued command.<br>
	 * <br>
	 * This method cannot return <code>null</code>
	 * 
	 * @return the name of the original issued command, never <code>null</code>
	 */
	public String getCommand();
	
	/**
	 * Returns the request sequence
	 * 
	 * @return the request sequence
	 */
	public int getRequestSequence();
	
	/**
	 * Returns the body of the {@link Response}.<br>
	 * <br>
	 * This method cannot return <code>null</code>, if there is no body
	 * an empty {@link Map} is returned.
	 *  
	 * @return the body of the {@link Response} or an empty {@link Map} never <code>null</code>
	 */
	public Map getBody();

	/**
	 * Returns <code>true</code> if the original {@link Request} was successful
	 * 
	 * @return <code>true</code> if the original {@link Request} was successful, <code>false</code> otherwise
	 */
	public boolean isSuccess();
	
	/**
	 * Returns <code>true</code> if the issued command left the connected VM in a running state.
	 * 
	 * @return <code>true</code> if the underlying command is running, <code>false</code> otherwise
	 */
	public boolean isRunning();
}
