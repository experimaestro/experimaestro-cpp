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
package org.eclipse.wst.jsdt.debug.internal.rhino.debugger;


/**
 * Handle for a {@link DebuggableScript} function
 * 
 * @since 1.0
 */
public class FunctionSource {
	
	final ScriptSource parent;
	final String name;
	String source;
	final int linenumber;
	
	/**
	 * Constructor
	 * 
	 * @param script the parent - cannot be <code>null</code>
	 * @param name the name of the function - can be <code>null</code>
	 * @param source the underlying source for the function - can be <code>null</code>
	 */
	public FunctionSource(ScriptSource script, String name, String source, int linenumber) {
		if(script == null) {
			throw new IllegalArgumentException("The parent script cannot be null"); //$NON-NLS-1$
		}
		this.parent = script;
		this.name = name;
		this.source = source;
		this.linenumber = linenumber;
	}

	/**
	 * @return the parent {@link ScriptSource} this function appears in
	 */
	public ScriptSource parent() {
		return this.parent;
	}
	
	/**
	 * @return the name of the function
	 */
	public String name() {
		return this.name; 
	}
	
	/**
	 * @return the source for the function
	 */
	public String source() {
		return this.source;
	}
	
	/**
	 * Sets the underlying source to the given string
	 * 
	 * @param source the new source
	 */
	public void setSource(String source) {
		this.source = source;
	}
	
	/**
	 * @return the line number this function appears on
	 */
	public int linenumber() {
		return this.linenumber;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("FunctionSource: [name - ").append(this.name).append("] [linenumber - ").append(linenumber).append("]\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		buffer.append("\tparent: ").append(parent.getLocation()); //$NON-NLS-1$
		buffer.append("\tsource: ").append(this.source).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
		return buffer.toString();
	}
}
