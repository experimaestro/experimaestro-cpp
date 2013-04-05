/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.debug.rhino.debugger.shell;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import org.eclipse.wst.jsdt.debug.internal.rhino.debugger.RhinoDebuggerImpl;
import org.eclipse.wst.jsdt.debug.internal.rhino.transport.RhinoTransportService;
import org.eclipse.wst.jsdt.debug.transport.TransportService;
import org.mozilla.javascript.tools.shell.Main;

/**
 * Entry point for launching a Rhino debugger
 * 
 * @since 1.0
 */
public final class DebugShell {
	
    /**
	 * Constant representing the default port
	 * <br><br>
	 * Value is: <code>9888</code>
	 */
	public static final String DEFAULT_PORT = "9888"; //$NON-NLS-1$
	/**
	 * Constant representing the trace argument
	 * <br><br>
	 * Value is: <code>-trace</code>
	 */
	public static final String ARG_TRACE = "-trace"; //$NON-NLS-1$
	/**
	 * Constant representing the suspend argument
	 * <br><br>
	 * Value is: <code>-suspend</code>
	 */
	public static final String ARG_SUSPEND = "-suspend"; //$NON-NLS-1$
	/**
	 * Constant representing the port argument
	 * <br><br>
	 * Value is: <code>-port</code>
	 */
	public static final String ARG_PORT = "-port"; //$NON-NLS-1$

	public static void main(String args[]) {
    	
    	String port = DEFAULT_PORT;
    	boolean suspend = false;
    	boolean trace = true;
    	
    	ArrayList argList = new ArrayList();
    	for (int i = 0; i < args.length; i++) {
    		String arg = args[i];
    		if (ARG_PORT.equals(arg)) {
    			port = args[++i];
    			continue;
    		} else if (ARG_SUSPEND.equals(arg)) {
    			suspend = isSuspend(args[++i]);
    			continue;
    		}
    		else if(ARG_TRACE.equals(arg)) {
    			trace = Boolean.valueOf(args[++i]).booleanValue();
    			continue;
    		}
    		//forward all other args to Rhino
    		argList.add(arg);
		}
    	String[] newArgs = (String[]) argList.toArray(new String[0]); 
    	
		TransportService service = new RhinoTransportService();
		RhinoDebuggerImpl debugger = new RhinoDebuggerImpl(service, port, suspend, trace);
		try {
			if(trace) {
				prettyPrintHeader(suspend, port);
			}
			debugger.start();
			Main.shellContextFactory.addListener(debugger);
			Main.exec(newArgs);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
	
	/**
	 * Pretty print the header for the debugger
	 * 
	 * @since 1.1
	 */
	static void prettyPrintHeader(boolean suspended, String port) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Rhino debugger\n"); //$NON-NLS-1$
		buffer.append("Start at time: ").append(getStartAtDate()); //$NON-NLS-1$
		buffer.append("\nListening to socket on "); //$NON-NLS-1$
		buffer.append("port: ").append(port); //$NON-NLS-1$
		if (suspended) {
			buffer.append("\nStarted suspended - waiting for client resume..."); //$NON-NLS-1$
		}
		System.out.println(buffer.toString());
	}
	
	/**
	 * Returns the formatted date
	 * 
	 * @return the formatted date
	 * @see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4981314
	 * @since 1.1
	 */
	static String getStartAtDate() {
		try {
			return DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(Calendar.getInstance().getTime());
		} catch (Throwable t) {
			return "<unknown>"; //$NON-NLS-1$
		}
	}
	
	/**
	 * Returns if the given argument should suspend 
	 * @param arg
	 * @return <code>true</code> if the argument is "true" or "y"
	 */
	public static boolean isSuspend(String arg) {
		return Boolean.valueOf(arg).booleanValue() || 
		       "y".equals(arg.toLowerCase());  //$NON-NLS-1$
	}
}
