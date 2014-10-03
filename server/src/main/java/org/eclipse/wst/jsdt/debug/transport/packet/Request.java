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
 * Describes the general form of a request {@link Packet}
 * 
 * @since 1.0
 */
public interface Request extends Packet {

	/**
	 * Returns the command that this {@link Request} was created for.<br>
	 * <br>
	 * This method cannot return <code>null</code>
	 * 
	 * @return the underlying command, never <code>null</code>
	 */
	public String getCommand();

	/**
	 * Returns the sequence for this request.
	 * 
	 * @return the request sequence
	 */
	public int getSequence();
	
	/**
	 * Returns the complete collection of JSON arguments in the {@link Request}.<br>
	 * <br>
	 * This method cannot return <code>null</code>, an empty map will be returned
	 * if there are no arguments.
	 * 
	 * @return the arguments or an empty map never <code>null</code>
	 */
	public Map getArguments();
}
