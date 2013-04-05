/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.debug.internal.rhino.debugger;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.wst.jsdt.debug.internal.rhino.transport.JSONConstants;
import org.eclipse.wst.jsdt.debug.internal.rhino.transport.RhinoRequest;
import org.eclipse.wst.jsdt.debug.internal.rhino.transport.RhinoResponse;

/**
 * Rhino request handler used to craft response bodies for given JSON requests
 * 
 * @since 1.0
 */
public class RequestHandler {

	public static final String VERSION_1_6 = "1.6"; //$NON-NLS-1$
	private RhinoDebuggerImpl debugger;

	/**
	 * Constructor
	 * 
	 * @param debugger
	 */
	public RequestHandler(RhinoDebuggerImpl debugger) {
		this.debugger = debugger;
	}

	/**
	 * Handles a general request - delegating to the correct command type
	 * 
	 * @param request
	 * @return
	 */
	public RhinoResponse handleRequest(RhinoRequest request) {
		String command = request.getCommand();
		RhinoResponse response = new RhinoResponse(request.getSequence(), command);
		try {
			if (command.equals(JSONConstants.VERSION)) {
				handleVersionRequest(request, response);
			} else if (command.equals(JSONConstants.SUSPEND)) {
				handleSuspendRequest(request, response);
			} else if (command.equals(JSONConstants.CONTINUE)) {
				handleContinueRequest(request, response);
			} else if (command.equals(JSONConstants.DISPOSE)) {
				handleDisposeRequest(request, response);
			} else if (command.equals(JSONConstants.THREADS)) {
				handleThreadsRequest(request, response);
			} else if (command.equals(JSONConstants.THREAD)) {
				handleThreadRequest(request, response);
			} else if (command.equals(JSONConstants.FRAMES)) {
				handleFramesRequest(request, response);
			} else if (command.equals(JSONConstants.FRAME)) {
				handleFrameRequest(request, response);
			} else if (command.equals(JSONConstants.SCRIPTS)) {
				handleScriptsRequest(request, response);
			} else if (command.equals(JSONConstants.SCRIPT)) {
				handleScriptRequest(request, response);
			} else if (command.equals(JSONConstants.EVALUATE)) {
				handleEvaluateRequest(request, response);
			} else if (command.equals(JSONConstants.LOOKUP)) {
				handleLookupRequest(request, response);
			} else if (command.equals(JSONConstants.BREAKPOINTS)) {
				handleBreakpointsRequest(request, response);
			} else if (command.equals(JSONConstants.BREAKPOINT)) {
				handleBreakpointRequest(request, response);
			} else if (command.equals(JSONConstants.SETBREAKPOINT)) {
				handleSetBreakpointRequest(request, response);
			} else if (command.equals(JSONConstants.CLEARBREAKPOINT)) {
				handleClearBreakpointRequest(request, response);
			} else {
				response.setSuccess(false);
				response.setMessage("command not supported"); //$NON-NLS-1$
			}
		} catch (Throwable t) {
			response.setSuccess(false);
			response.setMessage(t.getClass().getName() + " - " + t.getMessage()); //$NON-NLS-1$
			t.printStackTrace();
		}
		return response;
	}

	/**
	 * Handles a clear breakpoint request
	 * 
	 * @param request
	 * @param response
	 */
	private void handleClearBreakpointRequest(RhinoRequest request, RhinoResponse response) {
		Map arguments = request.getArguments();
		Long breakpointId = numberToLong((Number) arguments.get(JSONConstants.BREAKPOINT_ID));
		if (breakpointId == null) {
			response.setSuccess(false);
			response.setMessage(missingArgument(JSONConstants.BREAKPOINT_ID));
			return;
		}
		Breakpoint breakpoint = debugger.clearBreakpoint(breakpointId);
		if (breakpoint == null) {
			response.setSuccess(false);
			response.setMessage(JSONConstants.NOT_FOUND);
			return;
		}
		response.getBody().put(JSONConstants.BREAKPOINT, breakpoint.toJSON());
	}

	/**
	 * Handles a set breakpoint request
	 * 
	 * @param request
	 * @param response
	 */
	private void handleSetBreakpointRequest(RhinoRequest request, RhinoResponse response) {
		Map arguments = request.getArguments();
		Long scriptId = numberToLong((Number) arguments.get(JSONConstants.SCRIPT_ID));
		Integer line = numberToInteger((Number) arguments.get(JSONConstants.LINE));
		String function = (String) arguments.get(JSONConstants.FUNCTION);
		Long threadId = numberToLong((Number) arguments.get(JSONConstants.THREAD_ID));
		String condition = (String) arguments.get(JSONConstants.CONDITION);
		Breakpoint breakpoint = debugger.setBreakpoint(scriptId, line, function, condition, threadId);
		if(breakpoint != null) {
			response.getBody().put(JSONConstants.BREAKPOINT, breakpoint.toJSON());
		}
	}

	/**
	 * Handles a breakpoint request
	 * 
	 * @param request
	 * @param response
	 */
	private void handleBreakpointRequest(RhinoRequest request, RhinoResponse response) {
		Map arguments = request.getArguments();
		Long breakpointId = numberToLong((Number) arguments.get(JSONConstants.BREAKPOINT_ID));
		if (breakpointId == null) {
			response.setSuccess(false);
			response.setMessage(missingArgument(JSONConstants.BREAKPOINT_ID));
			return;
		}
		Breakpoint breakpoint = debugger.getBreakpoint(breakpointId);
		if (breakpoint == null) {
			response.setSuccess(false);
			response.setMessage(JSONConstants.NOT_FOUND);
			return;
		}
		response.getBody().put(JSONConstants.BREAKPOINT, breakpoint.toJSON());
	}

	/**
	 * Handles a breakpoints request
	 * 
	 * @param request
	 * @param response
	 */
	private void handleBreakpointsRequest(RhinoRequest request, RhinoResponse response) {
		Collection breakpoints = debugger.getBreakpoints();
		response.getBody().put(JSONConstants.BREAKPOINTS, breakpoints);
	}

	/**
	 * Handles an evaluate request
	 * 
	 * @param request
	 * @param response
	 */
	private void handleEvaluateRequest(RhinoRequest request, RhinoResponse response) {
		Map arguments = request.getArguments();
		Long threadId = numberToLong((Number) arguments.get(JSONConstants.THREAD_ID));
		if (threadId == null) {
			response.setSuccess(false);
			response.setMessage(missingArgument(JSONConstants.THREAD_ID));
			return;
		}
		Long frameId = numberToLong((Number) arguments.get(JSONConstants.FRAME_ID));
		if (frameId == null) {
			response.setSuccess(false);
			response.setMessage(missingArgument(JSONConstants.FRAME_ID));
			return;
		}
		String evaluate = (String) arguments.get(JSONConstants.EXPRESSION);
		if (evaluate == null) {
			response.setSuccess(false);
			response.setMessage(missingArgument(JSONConstants.EXPRESSION));
			return;
		}
		StackFrame frame = debugger.getFrame(threadId, frameId);
		if (frame == null) {
			response.setSuccess(false);
			response.setMessage(JSONConstants.NOT_FOUND);
			return;
		}
		Object result = frame.evaluate(evaluate);
		response.getBody().put(JSONConstants.EVALUATE, result);
	}

	/**
	 * Handles a lookup request
	 * 
	 * @param request
	 * @param response
	 */
	private void handleLookupRequest(RhinoRequest request, RhinoResponse response) {
		Map arguments = request.getArguments();
		Long threadId = numberToLong((Number) arguments.get(JSONConstants.THREAD_ID));
		if (threadId == null) {
			response.setSuccess(false);
			response.setMessage(missingArgument(JSONConstants.THREAD_ID));
			return;
		}
		Long frameId = numberToLong((Number) arguments.get(JSONConstants.FRAME_ID));
		if (frameId == null) {
			response.setSuccess(false);
			response.setMessage(missingArgument(JSONConstants.FRAME_ID));
			return;
		}
		Long handle = numberToLong((Number) arguments.get(JSONConstants.REF));
		if (handle == null) {
			response.setSuccess(false);
			response.setMessage(missingArgument(JSONConstants.REF));
			return;
		}
		StackFrame frame = debugger.getFrame(threadId, frameId);
		if (frame == null) {
			response.setSuccess(false);
			response.setMessage(JSONConstants.NOT_FOUND);
			return;
		}
		Object result = frame.lookup(handle);
		response.getBody().put(JSONConstants.LOOKUP, result);
	}

	/**
	 * Handles a script request
	 * 
	 * @param request
	 * @param response
	 */
	private void handleScriptRequest(RhinoRequest request, RhinoResponse response) {
		Map arguments = request.getArguments();
		Long scriptId = numberToLong((Number) arguments.get(JSONConstants.SCRIPT_ID));
		if (scriptId == null) {
			response.setSuccess(false);
			response.setMessage(missingArgument(JSONConstants.SCRIPT_ID));
			return;
		}
		ScriptSource script = debugger.getScript(scriptId);
		if (script == null) {
			response.setSuccess(false);
			response.setMessage(JSONConstants.NOT_FOUND);
			return;
		}
		response.getBody().put(JSONConstants.SCRIPT, script.toJSON());
	}

	/**
	 * Handles a scripts request
	 * 
	 * @param request
	 * @param response
	 */
	private void handleScriptsRequest(RhinoRequest request, RhinoResponse response) {
		List scriptIds = debugger.getScriptIds();
		response.getBody().put(JSONConstants.SCRIPTS, scriptIds);
	}

	/**
	 * Handles a frame request
	 * 
	 * @param request
	 * @param response
	 */
	private void handleFrameRequest(RhinoRequest request, RhinoResponse response) {
		Map arguments = request.getArguments();
		Long threadId = numberToLong((Number) arguments.get(JSONConstants.THREAD_ID));
		if (threadId == null) {
			response.setSuccess(false);
			response.setMessage(missingArgument(JSONConstants.THREAD_ID));
			return;
		}
		Long frameId = numberToLong((Number) arguments.get(JSONConstants.FRAME_ID));
		if (frameId == null) {
			response.setSuccess(false);
			response.setMessage(missingArgument(JSONConstants.FRAME_ID));
			return;
		}
		StackFrame frame = debugger.getFrame(threadId, frameId);
		if (frame == null) {
			response.setSuccess(false);
			response.setMessage(JSONConstants.NOT_FOUND);
			return;
		}
		response.getBody().put(JSONConstants.FRAME, frame.toJSON());
	}

	/**
	 * Handles a frames request
	 * 
	 * @param request
	 * @param response
	 */
	private void handleFramesRequest(RhinoRequest request, RhinoResponse response) {
		Map arguments = request.getArguments();
		Long threadId = numberToLong((Number) arguments.get(JSONConstants.THREAD_ID));
		if (threadId == null) {
			response.setSuccess(false);
			response.setMessage(missingArgument(JSONConstants.THREAD_ID));
			return;
		}
		List frameIds = debugger.getFrameIds(threadId);
		response.getBody().put(JSONConstants.FRAMES, frameIds);
	}

	/**
	 * Handles a thread request
	 * 
	 * @param request
	 * @param response
	 */
	private void handleThreadRequest(RhinoRequest request, RhinoResponse response) {
		Map arguments = request.getArguments();
		Long threadId = numberToLong((Number) arguments.get(JSONConstants.THREAD_ID));
		if (threadId == null) {
			response.setSuccess(false);
			response.setMessage(missingArgument(JSONConstants.THREAD_ID));
			return;
		}
		ThreadData threadData = debugger.getThreadData(threadId);
		if (threadData == null) {
			response.setSuccess(false);
			response.setMessage(JSONConstants.NOT_FOUND);
			return;
		}
		response.getBody().put(JSONConstants.THREAD, threadData.toJSON());
	}

	/**
	 * Reports a missing argument
	 * 
	 * @param key
	 * @return
	 */
	private String missingArgument(String key) {
		// TODO NLS this
		return "Missing Argument: "+key; //$NON-NLS-1$
	}

	/**
	 * Handles a threads request
	 * 
	 * @param request
	 * @param response
	 */
	private void handleThreadsRequest(RhinoRequest request, RhinoResponse response) {
		List threadIds = debugger.getThreadIds();
		response.getBody().put(JSONConstants.THREADS, threadIds);
	}

	/**
	 * Handles a dispose request by disconnecting the underlying {@link RhinoDebuggerImpl}
	 * 
	 * @param request
	 * @param response
	 */
	private void handleDisposeRequest(RhinoRequest request, RhinoResponse response) {
		debugger.disconnect();
	}

	/**
	 * Handles a continue request
	 * 
	 * @param request
	 * @param response
	 */
	private void handleContinueRequest(RhinoRequest request, RhinoResponse response) {
		Map arguments = request.getArguments();
		Long threadId = numberToLong((Number) arguments.get(JSONConstants.THREAD_ID));
		if (threadId == null) {
			debugger.resumeAll();
			return;
		}
		String stepType = (String) arguments.get(JSONConstants.STEP);
		debugger.resume(threadId, stepType);
	}

	/**
	 * Handles a suspend request
	 * 
	 * @param request
	 * @param response
	 */
	private void handleSuspendRequest(RhinoRequest request, RhinoResponse response) {
		Map arguments = request.getArguments();
		Long threadId = numberToLong((Number) arguments.get(JSONConstants.THREAD_ID));
		if (threadId == null) {
			debugger.suspendAll();
			return;
		}
		debugger.suspend(threadId);
	}

	/**
	 * Wrappers the given {@link Number} as a {@link Long}
	 * 
	 * @param number
	 * @return a new {@link Long}
	 */
	private static Long numberToLong(Number number) {
		if (number == null)
			return null;
		return new Long(number.longValue());
	}

	/**
	 * Wrappers the given {@link Number} as an {@link Integer}
	 * 
	 * @param number
	 * @return a new {@link Integer}
	 */
	private Integer numberToInteger(Number number) {
		if (number == null) {
			return null;
		}
		return new Integer(number.intValue());
	}

	/**
	 * Appends version infos to the given response body
	 * 
	 * @param request
	 * @param response
	 * 
	 * @see JSONConstants#ECMASCRIPT_VERSION
	 * @see JSONConstants#JAVASCRIPT_VERSION
	 * @see JSONConstants#VM_NAME
	 * @see JSONConstants#VM_VENDOR
	 * @see JSONConstants#VM_VERSION
	 */
	private void handleVersionRequest(RhinoRequest request, RhinoResponse response) {
		Map body = response.getBody();
		body.put(JSONConstants.JAVASCRIPT_VERSION, VERSION_1_6);
		body.put(JSONConstants.ECMASCRIPT_VERSION, "3"); //$NON-NLS-1$
		body.put(JSONConstants.VM_NAME, "Rhino"); //$NON-NLS-1$
		body.put(JSONConstants.VM_VERSION, VERSION_1_6);
		body.put(JSONConstants.VM_VENDOR, "Mozilla"); //$NON-NLS-1$
	}
}
