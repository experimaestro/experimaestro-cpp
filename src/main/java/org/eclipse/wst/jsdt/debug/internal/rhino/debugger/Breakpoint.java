/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.debug.internal.rhino.debugger;

import java.util.HashMap;

import org.eclipse.wst.jsdt.debug.internal.rhino.transport.JSONConstants;


/**
 * Implementation of a breakpoint wrt Rhino debugging
 * 
 * @since 1.0
 */
public class Breakpoint {

	final Long breakpointId;
	final ScriptSource script;
	final Integer lineNumber;
	final Object functionName;
	final String condition;
	final Long threadId;

	/**
	 * Constructor
	 * 
	 * @param breakpointId the id for the breakpoint
	 * @param script the script the breakpoint applies to, <code>null</code> is not accepted
	 * @param lineNumber the line number for the breakpoint
	 * @param functionName the name of the function the breakpoint is set on, <code>null</code> is accepted
	 * @param condition the condition for the breakpoint, <code>null</code> is accepted
	 * @param threadId the id of the thread the breakpoint is for
	 */
	public Breakpoint(Long breakpointId, ScriptSource script, Integer lineNumber, String functionName, String condition, Long threadId) {
		if (script == null) {
			throw new IllegalArgumentException("The parent script cannot be null"); //$NON-NLS-1$
		}
		this.breakpointId = breakpointId;
		this.script = script;
		this.lineNumber = lineNumber;
		this.functionName = functionName;
		this.condition = condition;
		this.threadId = threadId;
	}

	/**
	 * @return the breakpoint as a JSON entry
	 */
	public Object toJSON() {
		HashMap result = new HashMap();
		result.put(JSONConstants.BREAKPOINT_ID, breakpointId);
		result.put(JSONConstants.SCRIPT_ID, script.getId());
		if (lineNumber != null) {
			result.put(JSONConstants.LINE, lineNumber);
		}
		if (functionName != null) {
			result.put(JSONConstants.FUNCTION, functionName);
		}
		if (condition != null) {
			result.put(JSONConstants.CONDITION, condition);
		}
		if (threadId != null) {
			result.put(JSONConstants.THREAD_ID, threadId);
		}
		return result;
	}

	/**
	 * Deletes the breakpoint from the script it is associated with. Does
	 * not clear any of the handle information so the deleted breakpoint can be 
	 * returned as an event if required.
	 */
	public void delete() {
		this.script.removeBreakpoint(this);
	}
}
