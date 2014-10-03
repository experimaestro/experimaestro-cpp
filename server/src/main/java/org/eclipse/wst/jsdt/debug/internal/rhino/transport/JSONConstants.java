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
package org.eclipse.wst.jsdt.debug.internal.rhino.transport;

/**
 * Collection of constants used for JSON
 * 
 * @since 1.0
 */
public interface JSONConstants {

	// request / response
	public static final String SEQ = "seq"; //$NON-NLS-1$
	public static final String REQUEST_SEQ = "request_seq"; //$NON-NLS-1$
	public static final String TYPE = "type"; //$NON-NLS-1$
	public static final String REQUEST = "request"; //$NON-NLS-1$
	public static final String COMMAND = "command"; //$NON-NLS-1$
	public static final String CONNECT = "connect"; //$NON-NLS-1$
	public static final String MESSAGE = "message"; //$NON-NLS-1$
	public static final String ARGUMENTS = "arguments"; //$NON-NLS-1$
	public static final String BODY = "body"; //$NON-NLS-1$
	public static final String LINE = "line"; //$NON-NLS-1$
	public static final String SCRIPT = "script"; //$NON-NLS-1$
	public static final String SCRIPTS = "scripts"; //$NON-NLS-1$
	public static final String FUNCTION = "function"; //$NON-NLS-1$
	public static final String ENTER = "enter"; //$NON-NLS-1$
	public static final String EXIT = "exit"; //$NON-NLS-1$
	public static final String FUNCTIONS = "functions"; //$NON-NLS-1$
	public static final String BREAK = "break"; //$NON-NLS-1$
	public static final String RESPONSE = "response"; //$NON-NLS-1$
	public static final String FUNCTION_NAME = "functionName"; //$NON-NLS-1$
	public static final String EVENT = "event"; //$NON-NLS-1$
	public static final String NOT_FOUND = "not found"; //$NON-NLS-1$
	/**
	 * The type for a VMDeathEvent
	 * 
	 * @see EventPacket#getType()
	 */
	public static final String VMDEATH = "vmdeath"; //$NON-NLS-1$

	// value constants
	public static final String UNDEFINED = "undefined"; //$NON-NLS-1$
	public static final String UNKNOWN = "unknown"; //$NON-NLS-1$
	public static final String NULL = "null"; //$NON-NLS-1$
	public static final String BOOLEAN = "boolean"; //$NON-NLS-1$
	public static final String NUMBER = "number"; //$NON-NLS-1$
	public static final String STRING = "string"; //$NON-NLS-1$
	public static final String OBJECT = "object"; //$NON-NLS-1$
	public static final String ARRAY = "array"; //$NON-NLS-1$
	public static final String PROPERTY = "property"; //$NON-NLS-1$
	public static final String VARIABLE = "variable"; //$NON-NLS-1$
	public static final String THIS = "this"; //$NON-NLS-1$
	public static final String PROTOTYPE_OBJECT = "prototypeObject"; //$NON-NLS-1$
	public static final String CONSTRUCTOR_FUNCTION = "constructorFunction"; //$NON-NLS-1$

	// id constants
	public static final String THREAD_ID = "threadId"; //$NON-NLS-1$
	public static final String FRAME_ID = "frameId"; //$NON-NLS-1$
	public static final String BREAKPOINT_ID = "breakpointId"; //$NON-NLS-1$
	public static final String CONTEXT_ID = "contextId"; //$NON-NLS-1$
	public static final String SCRIPT_ID = "scriptId"; //$NON-NLS-1$

	// breakpoints
	public static final String SETBREAKPOINT = "setbreakpoint"; //$NON-NLS-1$
	public static final String BREAKPOINT = "breakpoint"; //$NON-NLS-1$
	public static final String BREAKPOINTS = "breakpoints"; //$NON-NLS-1$
	public static final String CLEARBREAKPOINT = "clearbreakpoint"; //$NON-NLS-1$
	public static final String CONDITION = "condition"; //$NON-NLS-1$
	public static final String LINES = "lines"; //$NON-NLS-1$
	public static final String DEBUGGER_STATEMENT = "debuggerStatement"; //$NON-NLS-1$

	// threads
	public static final String CONTINUE = "continue"; //$NON-NLS-1$
	public static final String RUNNING = "running"; //$NON-NLS-1$
	public static final String BACKTRACE = "backtrace"; //$NON-NLS-1$
	public static final String SUCCESS = "success"; //$NON-NLS-1$
	public static final String TOTAL_FRAMES = "totalframes"; //$NON-NLS-1$
	public static final String STEP_ACTION = "stepaction"; //$NON-NLS-1$
	public static final String STEP = "step"; //$NON-NLS-1$
	public static final String STEP_TYPE = "stepType"; //$NON-NLS-1$
	public static final String STEP_IN = "in"; //$NON-NLS-1$
	public static final String STEP_NEXT = "next"; //$NON-NLS-1$
	public static final String STEP_OUT = "out"; //$NON-NLS-1$
	public static final String STEP_ANY = "any"; //$NON-NLS-1$
	public static final String CONTEXTS = "contexts"; //$NON-NLS-1$
	public static final String STATE = "state"; //$NON-NLS-1$
	public static final String FRAMES = "frames"; //$NON-NLS-1$

	// target
	public static final String THREADS = "threads"; //$NON-NLS-1$
	public static final String THREAD = "thread"; //$NON-NLS-1$
	public static final String VERSION = "version"; //$NON-NLS-1$
	public static final String DISPOSE = "dispose"; //$NON-NLS-1$
	public static final String SUSPEND = "suspend"; //$NON-NLS-1$
	public static final String SUSPENDED = "suspended"; //$NON-NLS-1$
	public static final String EXCEPTION = "exception"; //$NON-NLS-1$
	public static final String VM_VERSION = "javascript.vm.version"; //$NON-NLS-1$
	public static final String VM_NAME = "javascript.vm.name"; //$NON-NLS-1$
	public static final String VM_VENDOR = "javascript.vm.vendor"; //$NON-NLS-1$
	public static final String JAVASCRIPT_VERSION = "javascript.version"; //$NON-NLS-1$
	public static final String ECMASCRIPT_VERSION = "ecmascript.version"; //$NON-NLS-1$

	// stackframe
	public static final String EVALUATE = "evaluate"; //$NON-NLS-1$
	public static final String EXPRESSION = "expression"; //$NON-NLS-1$
	public static final String PROPERTIES = "properties"; //$NON-NLS-1$
	public static final String NAME = "name"; //$NON-NLS-1$
	public static final String LOOKUP = "lookup"; //$NON-NLS-1$
	public static final String REF = "ref"; //$NON-NLS-1$	
	public static final String CONSTRUCTOR = "constructor"; //$NON-NLS-1$
	public static final String CLASS_NAME = "className"; //$NON-NLS-1$
	public static final String VALUE = "value"; //$NON-NLS-1$
	public static final String SCOPE_NAME = "scopeName"; //$NON-NLS-1$
	public static final String FRAME = "frame"; //$NON-NLS-1$

	// scripts
	public static final String GENERATED = "generated"; //$NON-NLS-1$
	public static final String SOURCE = "source"; //$NON-NLS-1$
	public static final String LOCATION = "location"; //$NON-NLS-1$
	public static final String BASE = "base"; //$NON-NLS-1$	
	public static final String PATH = "path"; //$NON-NLS-1$
}
