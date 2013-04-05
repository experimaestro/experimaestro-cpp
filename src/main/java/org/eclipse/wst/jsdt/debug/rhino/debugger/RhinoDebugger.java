/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.debug.rhino.debugger;

import org.eclipse.wst.jsdt.debug.internal.rhino.debugger.RhinoDebuggerImpl;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;

/**
 * Rhino debugger
 * 
 * @since 1.0
 */
public final class RhinoDebugger implements ContextFactory.Listener {
	
	/**
	 * Delegate debugger
	 */
	private RhinoDebuggerImpl impl;

	/**
	 * This constructor will only accept a <code>transport</code> argument
	 * of <code>socket</code>. I.e. <code>transport=socket</code>.
	 * 
	 * @param configString the configuration string, for example: <code>transport=socket,suspend=y,address=9000</code>
	 */
	public RhinoDebugger(String configString) {
		impl = new RhinoDebuggerImpl(configString);
	}
	
	/**
	 * Starts the debugger
	 * 
	 * @throws Exception if the debugger could not start
	 */
	public void start() throws Exception {
		impl.start();
	}

	/**
	 * Stops the debugger
	 */
	public void stop() throws Exception {
		impl.stop();
	}

	/* (non-Javadoc)
	 * @see org.mozilla.javascript.ContextFactory.Listener#contextCreated(org.mozilla.javascript.Context)
	 */
	public void contextCreated(Context context) {
		impl.contextCreated(context);
	}

	/* (non-Javadoc)
	 * @see org.mozilla.javascript.ContextFactory.Listener#contextReleased(org.mozilla.javascript.Context)
	 */
	public void contextReleased(Context context) {
		impl.contextReleased(context);
	}
}
