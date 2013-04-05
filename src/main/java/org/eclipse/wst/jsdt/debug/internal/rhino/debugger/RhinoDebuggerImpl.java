/*******************************************************************************
 * Copyright (c) 2009, 2012 IBM Corporation and others All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.debug.internal.rhino.debugger;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.wst.jsdt.debug.internal.rhino.transport.EventPacket;
import org.eclipse.wst.jsdt.debug.internal.rhino.transport.JSONConstants;
import org.eclipse.wst.jsdt.debug.internal.rhino.transport.JSONUtil;
import org.eclipse.wst.jsdt.debug.transport.TransportService;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.debug.DebugFrame;
import org.mozilla.javascript.debug.DebuggableScript;
import org.mozilla.javascript.debug.Debugger;

/**
 * Rhino implementation of {@link Debugger}
 * <br><br>
 * Events fired:
 * <ul>
 * <li><b>Thread enter event</b> - when a new context is created see: {@link #contextCreated(Context)}</li>
 * <li><b>Thread exit event</b> - when a context is exited see: {@link #contextReleased(Context)}</li>
 * <li><b>VM death event</b> - if the debugger dies: this event can only be received if the underlying communication channel has not been interrupted</li>
 * </ul>
 * @since 1.0
 */
public class RhinoDebuggerImpl implements Debugger, ContextFactory.Listener {

	public static final DebuggableScript[] NO_SCRIPTS = new DebuggableScript[0];
	private static final String RHINO_SCHEME = "rhino"; //$NON-NLS-1$
	
	private final Map threadToThreadId = new HashMap();
	private final Map threadIdToData = new HashMap();
	private final Map breakpoints = new HashMap();

	private long currentThreadId = 0L;
	private long currentBreakpointId = 0L;
	private long currentScriptId = 0L;
	private ArrayList disabledThreads = new ArrayList();

	/**
	 * Mapping of the URI string to the {@link ScriptSource}
	 */
	private HashMap/*<String, ScriptSource>*/ uriToScript = new HashMap();
	/**
	 * Mapping of the id to the {@link ScriptSource}
	 */
	private HashMap/*<Long, ScriptSource>*/ idToScript = new HashMap();

	private final DebugSessionManager sessionManager;
	
	/**
	 * This constructor will only accept a <code>transport</code> argument
	 * of <code>socket</code>. I.e. <code>transport=socket</code>.<br><br>
	 * 
	 * To use a differing {@link TransportService} pleas use the other constructor:
	 * {@link #RhinoDebugger(TransportService, String, boolean)}
	 * 
	 * @param configString the configuration string, for example: <code>transport=socket,suspend=y,address=9000</code>
	 */
	public RhinoDebuggerImpl(String configString) {
		sessionManager = DebugSessionManager.create(configString);
	}
	
	/**
	 * This constructor allows you to specify a custom {@link TransportService} to use other than <code>socket</code>.
	 * 
	 * @param transportService the {@link TransportService} to use for debugger communication
	 * @param address the address to communicate on
	 * @param startSuspended if the debugger should wait while accepting a connection. The wait time for stating suspended is not indefinite,
	 * @param trace if the debugger should be in tracing mode, reporting debug statements to the console 
	 * and is equal to 300000ms.
	 */
	public RhinoDebuggerImpl(TransportService transportService, String address, boolean startSuspended, boolean trace) {
		sessionManager = new DebugSessionManager( transportService,  address,  startSuspended, trace);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mozilla.javascript.debug.Debugger#getFrame(org.mozilla.javascript.Context, org.mozilla.javascript.debug.DebuggableScript)
	 */
	public synchronized DebugFrame getFrame(Context context, DebuggableScript debuggableScript) {
		ScriptSource script = getScript(debuggableScript);
		if(script != null && !script.isStdIn()) {
			ContextData contextData = (ContextData) context.getDebuggerContextData();
			ThreadData thread = (ThreadData) threadIdToData.get(contextData.getThreadId());
			FunctionSource function = script.getFunction(debuggableScript);
			return thread.getFrame(context, function, script);
		}
		return null;
	}
	
	/**
	 * Returns the root {@link ScriptSource} context
	 * 
	 * @param script
	 * @return the root {@link ScriptSource} context
	 */
	private ScriptSource getScript(DebuggableScript script) {
		synchronized (uriToScript) {
			DebuggableScript root = script;
			while (!root.isTopLevel()) {
				root = root.getParent();
			}
			URI uri = getSourceUri(root, parseSourceProperties(root.getSourceName()));
			if(uri != null) {
				return (ScriptSource) uriToScript.get(uri);
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mozilla.javascript.debug.Debugger#handleCompilationDone(org.mozilla.javascript.Context, org.mozilla.javascript.debug.DebuggableScript, java.lang.String)
	 */
	public void handleCompilationDone(Context context, DebuggableScript script, String source) {
		if (!script.isTopLevel()) {
			return;
		}
		Map properties = parseSourceProperties(script.getSourceName());
		URI uri = getSourceUri(script, properties);
		if(uri == null) {
			//if the source cannot be located don't load or notify
			return;
		}
		final ScriptSource newscript = new ScriptSource(script, source, uri, script.isGeneratedScript(), properties);
		synchronized (uriToScript) {
			ScriptSource old = (ScriptSource) uriToScript.remove(uri);
			Long id = null;
			if(old != null) {
				//recycle the id for a re-loaded script
				//https://bugs.eclipse.org/bugs/show_bug.cgi?id=306832
				id = old.getId();
				idToScript.remove(id);
				newscript.setId(id);
				//clean up the cache of breakpoints
				old.clearBreakpoints(this);
			}
			else {
				//a totally new script is loaded
				id = new Long(currentScriptId++);
				newscript.setId(id);
			}
			uriToScript.put(uri, newscript);
			idToScript.put(id, newscript);
		}
		ContextData contextData = (ContextData) context.getDebuggerContextData();
		contextData.scriptLoaded(newscript);
	}

	/**
	 * Composes a {@link URI} representing the path to the source of the given script
	 * 
	 * @param script the script to create a {@link URI} for
	 * @param properties any special properties @see {@link #parseSourceProperties(String)}
	 * @return the {@link URI} for the source or <code>null</code>
	 */
	private URI getSourceUri(DebuggableScript script, Map properties) {
			String sourceName = script.getSourceName();
			if(properties != null) {
				String jsonName = (String) properties.get(JSONConstants.NAME);
				if (jsonName != null)
					sourceName = jsonName;
			}
			
			// handle null sourceName
			if (sourceName == null)
				return null;
			
			// handle input from the Rhino Shell
			if (sourceName.equals("<stdin>")) { //$NON-NLS-1$
				sourceName = "stdin"; //$NON-NLS-1$
			} 
			if(sourceName.equals("<command>")) { //$NON-NLS-1$
				sourceName = "command"; //$NON-NLS-1$
			}
			else {	
				// try to parse it as a file
				File sourceFile = new File(sourceName);
				if (sourceFile.exists())
					return sourceFile.toURI();
				
				//try to just create a URI from the name
				try {
					return new URI(sourceName);
				} catch(URISyntaxException e) {
					//do nothing and fall through
				}
			}
			
			//fall back to creating a rhino specific URI from the script source name as a path
			try {
				if (! (sourceName.charAt(0) == '/'))
					sourceName = "/" + sourceName; //$NON-NLS-1$
				return new URI(RHINO_SCHEME, null, sourceName, null);
			} catch (URISyntaxException e) {
				return null;
			}
	}
	
	/**
	 * Returns any special properties specified in the source name or <code>null</code>
	 * 
	 * @param sourceName
	 * @return any special properties specified in the source name or <code>null</code>
	 */
	Map parseSourceProperties(String sourceName) {
		if (sourceName != null && sourceName.charAt(0) == '{') {
			try {
				Object json = JSONUtil.read(sourceName);
				if (json instanceof Map) {
					return (Map) json;
				}
			} catch (RuntimeException e) {
				// ignore
			}
		}
		return null;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mozilla.javascript.ContextFactory.Listener#contextCreated(org.mozilla.javascript.Context)
	 */
	public synchronized void contextCreated(Context context) {
		Thread thread = Thread.currentThread();
		if (disabledThreads.contains(thread)) {
			return;
		}
		Long threadId = (Long) threadToThreadId.get(thread);
		if (threadId == null) {
			threadId = new Long(currentThreadId++);
			threadToThreadId.put(thread, threadId);
		}
		ThreadData threadData = (ThreadData) threadIdToData.get(threadId);
		if (threadData == null) {
			threadData = new ThreadData(threadId, this);
			threadIdToData.put(threadId, threadData);
			sendThreadEvent(JSONConstants.ENTER, threadId);
		}
		threadData.contextCreated(context);
	}

	/**
	 * Sends a thread event for the given type
	 * 
	 * @param type the type of event to send
	 * @param threadId the id of the thread the even is for
	 * 
	 * @see JSONConstants#ENTER
	 * @see JSONConstants#EXIT
	 */
	private void sendThreadEvent(String type, Long threadId) {
		EventPacket threadEvent = new EventPacket(JSONConstants.THREAD);
		Map body = threadEvent.getBody();
		body.put(JSONConstants.TYPE, type);
		body.put(JSONConstants.THREAD_ID, threadId);
		sendEvent(threadEvent);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mozilla.javascript.ContextFactory.Listener#contextReleased(org.mozilla.javascript.Context)
	 */
	public synchronized void contextReleased(Context context) {
		Thread thread = Thread.currentThread();
		if (disabledThreads.contains(thread)) {
			return;
		}
		Long threadId = (Long) threadToThreadId.get(thread);
		if (threadId == null) {
			return;
		}
		ThreadData threadData = (ThreadData) threadIdToData.get(threadId);
		threadData.contextReleased(context);
		if (!threadData.hasContext()) {
			threadToThreadId.remove(thread);
			threadIdToData.remove(threadId);
			sendThreadEvent(JSONConstants.EXIT, threadId);
		}
	}

	/**
	 * Resumes a thread with the given id for the given step type. Has no effect if no such thread exists
	 * 
	 * @param threadId
	 * @param stepType
	 */
	public synchronized void resume(Long threadId, String stepType) {
		ThreadData threadData = (ThreadData) threadIdToData.get(threadId);
		if (threadData != null) {
			threadData.resume(stepType);
		}
	}

	/**
	 * Resumes all threads currently in the debugger
	 */
	public synchronized void resumeAll() {
		for (Iterator it = threadIdToData.keySet().iterator(); it.hasNext();) {
			Long threadId = (Long) it.next();
			resume(threadId, null);
		}
	}

	/**
	 * Suspend the thread with the given id. Has no effect if no such thread exists
	 * 
	 * @param threadId
	 */
	public synchronized void suspend(Long threadId) {
		ThreadData threadData = (ThreadData) threadIdToData.get(threadId);
		if (threadData != null) {
			threadData.suspend();
		}
	}

	/**
	 * Suspend all threads currently in the debugger
	 */
	public synchronized void suspendAll() {
		for (Iterator it = threadIdToData.keySet().iterator(); it.hasNext();) {
			Long threadId = (Long) it.next();
			suspend(threadId);
		}
	}

	/**
	 * Disconnects the debugger
	 */
	public void disconnect() {
	}

	/**
	 * Returns all of the stack frame ids for the thread with the given id. Returns an empty list if no such thread exists, never <code>null</code>
	 * 
	 * @param threadId
	 * @return the complete list of stack frame ids from the thread with the given id
	 */
	public synchronized List getFrameIds(Long threadId) {
		ThreadData threadData = (ThreadData) threadIdToData.get(threadId);
		if (threadData == null) {
			return Collections.EMPTY_LIST;
		}
		return threadData.getFrameIds();
	}

	/**
	 * Returns a {@link DebugFrame} with the given id from the thread with the given thread id. Returns <code>null</code> if the no such thread exists with the given id and / or no such {@link DebugFrame} exists with the given id
	 * 
	 * @param threadId
	 * @param frameId
	 * @return the {@link DebugFrame} with the given id from the thread with the given id
	 */
	public synchronized StackFrame getFrame(Long threadId, Long frameId) {
		ThreadData threadData = (ThreadData) threadIdToData.get(threadId);
		if (threadData != null) {
			return threadData.getFrame(frameId);
		}
		return null;
	}

	/**
	 * @return the ids of all of the scripts currently known to the debugger
	 */
	public synchronized List getScriptIds() {
		return new ArrayList(idToScript.keySet());
	}

	/**
	 * Returns the script with the given id or <code>null</code> if no such script exists with the given id
	 * 
	 * @param scriptId
	 * @return the script with the given id or <code>null</code>
	 */
	public synchronized ScriptSource getScript(Long scriptId) {
		return (ScriptSource) idToScript.get(scriptId);
	}

	/**
	 * @return the complete collection of breakpoints currently known to the debugger
	 */
	public synchronized Collection getBreakpoints() {
		return breakpoints.keySet();
	}

	/**
	 * Creates a breakpoint in the script with the given id and the given breakpoint attributes. 
	 * Returns the new breakpoint or <code>null</code> if:
	 * <ul>
	 * <li>no such script exists with the given id</li>
	 * <li>the given line number is not a valid line number</li>
	 * </ul>
	 * <p>
	 * If a breakpoint already exists at the given location it is removed and the new breakpoint is set.
	 * </p>
	 * @param scriptId
	 * @param lineNumber
	 * @param functionName
	 * @param condition
	 * @param threadId
	 * @return the new breakpoint or <code>null</code> if no script exists with the given id
	 */
	public synchronized Breakpoint setBreakpoint(Long scriptId, Integer lineNumber, String functionName, String condition, Long threadId) {
		ScriptSource script = (ScriptSource) idToScript.get(scriptId);
		if (script == null || !script.isValid(lineNumber, functionName)) {
			return null;
		}

		
		Breakpoint newbreakpoint = new Breakpoint(nextBreakpointId(), script, lineNumber, functionName, condition, threadId);
		Breakpoint oldbp = script.getBreakpoint(lineNumber, functionName);
		if(oldbp != null) {
			breakpoints.remove(oldbp.breakpointId);
		}
		breakpoints.put(newbreakpoint.breakpointId, newbreakpoint);
		script.addBreakpoint(newbreakpoint);
		return newbreakpoint;
	}

	/**
	 * @return the next unique breakpoint id to use
	 */
	private synchronized Long nextBreakpointId() {
		return new Long(currentBreakpointId++);
	}

	/**
	 * Clears the breakpoint out of the cache with the given id and returns it. Returns <code>null</code> if no breakpoint exists with the given id.
	 * 
	 * @param breakpointId
	 * @return the removed breakpoint or <code>null</code>
	 */
	public synchronized Breakpoint clearBreakpoint(Long breakpointId) {
		Breakpoint breakpoint = (Breakpoint) breakpoints.remove(breakpointId);
		if (breakpoint != null) {
			breakpoint.delete();
		}
		return breakpoint;
	}

	/**
	 * Sends the given {@link EventPacket} using the underlying {@link DebugRuntime} and returns if it was sent successfully
	 * 
	 * @param event
	 * @return true if the event was sent successfully, false otherwise
	 */
	public boolean sendEvent(EventPacket event) {
		return sessionManager.sendEvent(event);
	}

	/**
	 * Gets a breakpoint with the given id, returns <code>null</code> if no such breakpoint exists with the given id
	 * 
	 * @param breakpointId
	 * @return the breakpoint with the given id or <code>null</code>
	 */
	public Breakpoint getBreakpoint(Long breakpointId) {
		return (Breakpoint) breakpoints.get(breakpointId);
	}

	/**
	 * Gets the thread for the thread with the given id, returns <code>null</code> if no such thread exists with the given id
	 * 
	 * @param threadId
	 * @return the thread data for the thread with the given id or <code>null</code>
	 */
	public synchronized ThreadData getThreadData(Long threadId) {
		return (ThreadData) threadIdToData.get(threadId);
	}

	/**
	 * @return the complete list of thread ids known to the debugger
	 */
	public synchronized List getThreadIds() {
		return new ArrayList(threadIdToData.keySet());
	}

	/**
	 * Caches the current thread as disabled
	 */
	public synchronized void disableThread() {
		disabledThreads.add(Thread.currentThread());
	}

	/**
	 * Removes the current thread as being disabled
	 */
	public synchronized void enableThread() {
		disabledThreads.remove(Thread.currentThread());
	}

	public void start() {
		sessionManager.start(this);
	}

	public void stop() {
		sessionManager.stop();
	}
	
	/**
	 * Returns if a {@link DebugSession} has successfully connected to this debugger.
	 * 
	 * @return <code>true</code> if the debugger has a connected {@link DebugSession} <code>false</code> otherwise
	 */
	public boolean isConnected() {
		return sessionManager.isConnected();
	}
}
