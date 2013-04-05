/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.debug.internal.rhino.debugger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.wst.jsdt.debug.internal.rhino.transport.EventPacket;
import org.eclipse.wst.jsdt.debug.internal.rhino.transport.JSONConstants;

/**
 * Rhino implementation of {@link ContextData}
 * 
 * @since 1.0
 */
public class ContextData {
	private static final int CONTEXT_RUNNING = 0;
	private static final int CONTEXT_SUSPENDED = 1;

	private static final int STEP_CONTINUE = 0;
	private static final int STEP_IN = 1;
	private static final int STEP_NEXT = 2;
	private static final int STEP_OUT = 4;

	private final Long threadId;
	private final Long contextId;
	private final RhinoDebuggerImpl debugger;

	private final LinkedList frames = new LinkedList();

	private int contextState = CONTEXT_RUNNING;
	private int stepState = STEP_CONTINUE;
	private StackFrame stepFrame;

	/**
	 * Constructor
	 * 
	 * @param threadId
	 * @param contextId
	 * @param debugger
	 */
	public ContextData(Long threadId, Long contextId, RhinoDebuggerImpl debugger) {
		this.threadId = threadId;
		this.contextId = contextId;
		this.debugger = debugger;
	}

	/**
	 * Returns the unique id of this context data
	 * 
	 * @return the unique id
	 */
	public Long getId() {
		return contextId;
	}

	/**
	 * Returns the live list of {@link DebugFrame}s from this context
	 * 
	 * @return the live list of {@link DebugFrame}s
	 */
	public synchronized List getFrameIds() {
		List result = new ArrayList();
		for (Iterator iterator = frames.iterator(); iterator.hasNext();) {
			result.add(((StackFrame) iterator.next()).getId());
		}
		return result;
	}

	/**
	 * Returns the {@link DebugFrame} with the given id or <code>null</code> if no such {@link DebugFrame} exists
	 * 
	 * @param frameId
	 * @return the {@link DebugFrame} with the given id or <code>null</code>
	 */
	public synchronized StackFrame getFrame(Long frameId) {
		StackFrame frame = null;
		for (Iterator iterator = frames.iterator(); iterator.hasNext();) {
			frame = (StackFrame) iterator.next();
			if (frame.getId().equals(frameId)) {
				return frame;
			}
		}
		return null;
	}

	/**
	 * Adds the given frame to the top of the frame stack and sends out a break event as needed
	 * 
	 * @param frame
	 * @param script
	 * @param lineNumber
	 * @param functionName
	 */
	public synchronized void pushFrame(StackFrame frame, ScriptSource script, Integer lineNumber, String functionName) {
		frames.addFirst(frame);
		Breakpoint breakpoint = script.getBreakpoint(lineNumber, functionName);
		boolean isStepBreak = stepBreak(STEP_IN);
		if (isStepBreak || breakpoint != null) {
			if (sendBreakEvent(script, frame.getLineNumber(), functionName, breakpoint, isStepBreak, false)) {
				suspendState();
			}
		}
	}

	/**
	 * Returns if the step operation should cause a break
	 * 
	 * @param step
	 * @return true if the operation should break false otherwise
	 */
	private boolean stepBreak(int step) {
		return ((0 != (step & stepState)) && (stepFrame == null || stepFrame == frames.getFirst()));
	}

	/**
	 * Suspends the state via {@link #wait()}
	 */
	private void suspendState() {
		contextState = CONTEXT_SUSPENDED;
		while (contextState == CONTEXT_SUSPENDED) {
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Removes the first {@link DebugFrame} from the current stack
	 * 
	 * @param byThrow
	 * @param resultOrException
	 */
	public synchronized void popFrame(boolean byThrow, Object resultOrException) {
		if(!frames.isEmpty()) {
			StackFrame frame = (StackFrame) frames.removeFirst();
			if (stepFrame == frame) {
				stepFrame = null;
				stepState = STEP_OUT;
			}
				
			boolean isStepBreak = stepBreak(STEP_IN | STEP_OUT);
			if (isStepBreak) {
				frame = getTopFrame();
				if (frame != null && sendBreakEvent(frame.getScript(), frame.getLineNumber(), null, null, isStepBreak, false)) {
					suspendState();
				}
			}
		}
	}

	/**
	 * Resume the state with the given step type
	 * 
	 * @param stepType
	 */
	public synchronized void resume(String stepType) {
		try {
			if (stepType == null) {
				stepState = STEP_CONTINUE;
				stepFrame = null;
			} else if (stepType.equals(JSONConstants.STEP_IN)) {
				stepState = STEP_IN;
				stepFrame = null;
			} else if (stepType.equals(JSONConstants.STEP_NEXT)) {
				stepState = STEP_NEXT;
				stepFrame = getTopFrame();
			} else if (stepType.equals(JSONConstants.STEP_OUT)) {
				if (frames.size() > 1) {
					stepState = STEP_OUT;
					stepFrame = (StackFrame) frames.get(1);
				} else {
					stepState = STEP_CONTINUE;
					stepFrame = null;
				}
			} else if (stepType.equals(JSONConstants.STEP_ANY)) {
				stepState = STEP_IN | STEP_OUT | STEP_NEXT;
				stepFrame = null;
			} else {
				throw new IllegalStateException("bad stepType: " + stepType); //$NON-NLS-1$
			}
		}
		finally {
			contextState = CONTEXT_RUNNING;
			notifyAll();
		}
	}

	/**
	 * Returns the top stack frame iff there are frames.
	 * Delegate method to prevent {@link NoSuchElementException}s
	 * 
	 * @return the top frame or <code>null</code>
	 */
	private StackFrame getTopFrame() {
		if(this.frames != null && !this.frames.isEmpty()) {
			return (StackFrame) this.frames.getFirst();
		}
		return null;
	}
	
	/**
	 * Set the step state to the suspend equivalent
	 */
	public synchronized void suspend() {
		stepState = STEP_IN | STEP_NEXT | STEP_OUT;
		stepFrame = null;
	}

	/**
	 * Handles a <code>debugger;</code> statement
	 * 
	 * @param script
	 * @param lineNumber
	 */
	public synchronized void debuggerStatement(ScriptSource script, Integer lineNumber) {
		Breakpoint breakpoint = script.getBreakpoint(lineNumber, null);
		boolean isStepBreak = stepBreak(STEP_IN | STEP_NEXT);
		if (sendBreakEvent(script, lineNumber, null, breakpoint, isStepBreak, true)) {
			suspendState();
		}
	}

	/**
	 * Handles a line change
	 * 
	 * @param script
	 * @param lineNumber
	 */
	public synchronized void lineChange(ScriptSource script, Integer lineNumber) {
		Breakpoint breakpoint = script.getBreakpoint(lineNumber, null);
		boolean isStepBreak = stepBreak(STEP_IN | STEP_NEXT);
		if (isStepBreak || breakpoint != null) {
			if (sendBreakEvent(script, lineNumber, null, breakpoint, isStepBreak, false)) {
				suspendState();
			}
		}
	}

	/**
	 * Handles forwarding an exception event
	 * 
	 * @param ex
	 */
	public synchronized void exceptionThrown(Throwable ex) {
		StackFrame frame = getTopFrame();
		if (sendExceptionEvent(frame.getScript(), frame.getLineNumber(), ex)) {
			suspendState();
		}
	}

	/**
	 * Sends a JSON message for an exception that has occurred
	 * 
	 * @param script
	 * @param lineNumber
	 * @param ex
	 * @return true if the message was sent successfully, false otherwise
	 */
	private boolean sendExceptionEvent(ScriptSource script, Integer lineNumber, Throwable ex) {
		EventPacket exceptionEvent = new EventPacket(JSONConstants.EXCEPTION);
		Map body = exceptionEvent.getBody();
		body.put(JSONConstants.CONTEXT_ID, contextId);
		body.put(JSONConstants.THREAD_ID, threadId);
		body.put(JSONConstants.SCRIPT_ID, script.getId());
		body.put(JSONConstants.LINE, lineNumber);
		body.put(JSONConstants.MESSAGE, ex.getMessage());
		return debugger.sendEvent(exceptionEvent);
	}

	/**
	 * Sends a JSON message for a break event
	 * 
	 * @param script
	 * @param lineNumber
	 * @param functionName
	 * @param breakpoint
	 * @param isStepBreak
	 * @param isDebuggerStatement
	 * @return true if the message was sent successfully, false otherwise
	 */
	private boolean sendBreakEvent(ScriptSource script, Integer lineNumber, String functionName, Breakpoint breakpoint, boolean isStepBreak, boolean isDebuggerStatement) {
		EventPacket breakEvent = new EventPacket(JSONConstants.BREAK);
		Map body = breakEvent.getBody();
		body.put(JSONConstants.THREAD_ID, threadId);
		body.put(JSONConstants.CONTEXT_ID, contextId);
		body.put(JSONConstants.SCRIPT_ID, script.getId());
		if (functionName != null) {
			body.put(JSONConstants.FUNCTION_NAME, functionName);
		}
		body.put(JSONConstants.LINE, lineNumber);
		if (breakpoint != null) {
			body.put(JSONConstants.BREAKPOINT, breakpoint.breakpointId);
		}

		if (isStepBreak) {
			String stepType;
			if (stepState == STEP_IN) {
				stepType = JSONConstants.STEP_IN;
			} else if (stepState == STEP_NEXT) {
				stepType = JSONConstants.STEP_NEXT;
			} else if (stepState == STEP_OUT) {
				stepType = JSONConstants.STEP_OUT;
			} else {
				stepType = JSONConstants.SUSPEND;
			}
			body.put(JSONConstants.STEP, stepType);
			stepState = 0;
		}

		body.put(JSONConstants.DEBUGGER_STATEMENT, Boolean.valueOf(isDebuggerStatement));
		return debugger.sendEvent(breakEvent);
	}

	/**
	 * Handles a script load event
	 * 
	 * @param script
	 */
	public synchronized void scriptLoaded(ScriptSource script) {
		if (sendScriptEvent(script)) {
			suspendState();
		}
	}

	/**
	 * Send a JSON message for a script event
	 * 
	 * @param script
	 * @return
	 */
	private boolean sendScriptEvent(ScriptSource script) {
		EventPacket scriptEvent = new EventPacket(JSONConstants.SCRIPT);
		Map body = scriptEvent.getBody();
		body.put(JSONConstants.THREAD_ID, threadId);
		body.put(JSONConstants.CONTEXT_ID, contextId);
		body.put(JSONConstants.SCRIPT_ID, script.getId());
		return debugger.sendEvent(scriptEvent);
	}

	/**
	 * Returns the string representation of the state
	 * 
	 * @return the state text
	 */
	public synchronized String getState() {
		return contextState == CONTEXT_RUNNING ? JSONConstants.RUNNING : JSONConstants.SUSPENDED;
	}

	/**
	 * Returns the underlying thread id
	 * 
	 * @return the underlying thread id
	 */
	public Long getThreadId() {
		return threadId;
	}
}
