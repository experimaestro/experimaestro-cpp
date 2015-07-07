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
	String SEQ = "seq"; //$NON-NLS-1$
	String REQUEST_SEQ = "request_seq"; //$NON-NLS-1$
	String TYPE = "type"; //$NON-NLS-1$
	String REQUEST = "request"; //$NON-NLS-1$
	String COMMAND = "command"; //$NON-NLS-1$
	String CONNECT = "connect"; //$NON-NLS-1$
	String MESSAGE = "message"; //$NON-NLS-1$
	String ARGUMENTS = "arguments"; //$NON-NLS-1$
	String BODY = "body"; //$NON-NLS-1$
	String LINE = "line"; //$NON-NLS-1$
	String SCRIPT = "script"; //$NON-NLS-1$
	String SCRIPTS = "scripts"; //$NON-NLS-1$
	String FUNCTION = "function"; //$NON-NLS-1$
	String ENTER = "enter"; //$NON-NLS-1$
	String EXIT = "exit"; //$NON-NLS-1$
	String FUNCTIONS = "functions"; //$NON-NLS-1$
	String BREAK = "break"; //$NON-NLS-1$
	String RESPONSE = "response"; //$NON-NLS-1$
	String FUNCTION_NAME = "functionName"; //$NON-NLS-1$
	String EVENT = "event"; //$NON-NLS-1$
	String NOT_FOUND = "not found"; //$NON-NLS-1$
	/**
	 * The type for a VMDeathEvent
	 * 
	 * @see EventPacket#getType()
	 */
	String VMDEATH = "vmdeath"; //$NON-NLS-1$

	// value constants
	String UNDEFINED = "undefined"; //$NON-NLS-1$
	String UNKNOWN = "unknown"; //$NON-NLS-1$
	String NULL = "null"; //$NON-NLS-1$
	String BOOLEAN = "boolean"; //$NON-NLS-1$
	String NUMBER = "number"; //$NON-NLS-1$
	String STRING = "string"; //$NON-NLS-1$
	String OBJECT = "object"; //$NON-NLS-1$
	String ARRAY = "array"; //$NON-NLS-1$
	String PROPERTY = "property"; //$NON-NLS-1$
	String VARIABLE = "variable"; //$NON-NLS-1$
	String THIS = "this"; //$NON-NLS-1$
	String PROTOTYPE_OBJECT = "prototypeObject"; //$NON-NLS-1$
	String CONSTRUCTOR_FUNCTION = "constructorFunction"; //$NON-NLS-1$

	// id constants
	String THREAD_ID = "threadId"; //$NON-NLS-1$
	String FRAME_ID = "frameId"; //$NON-NLS-1$
	String BREAKPOINT_ID = "breakpointId"; //$NON-NLS-1$
	String CONTEXT_ID = "contextId"; //$NON-NLS-1$
	String SCRIPT_ID = "scriptId"; //$NON-NLS-1$

	// breakpoints
	String SETBREAKPOINT = "setbreakpoint"; //$NON-NLS-1$
	String BREAKPOINT = "breakpoint"; //$NON-NLS-1$
	String BREAKPOINTS = "breakpoints"; //$NON-NLS-1$
	String CLEARBREAKPOINT = "clearbreakpoint"; //$NON-NLS-1$
	String CONDITION = "condition"; //$NON-NLS-1$
	String LINES = "lines"; //$NON-NLS-1$
	String DEBUGGER_STATEMENT = "debuggerStatement"; //$NON-NLS-1$

	// threads
	String CONTINUE = "continue"; //$NON-NLS-1$
	String RUNNING = "running"; //$NON-NLS-1$
	String BACKTRACE = "backtrace"; //$NON-NLS-1$
	String SUCCESS = "success"; //$NON-NLS-1$
	String TOTAL_FRAMES = "totalframes"; //$NON-NLS-1$
	String STEP_ACTION = "stepaction"; //$NON-NLS-1$
	String STEP = "step"; //$NON-NLS-1$
	String STEP_TYPE = "stepType"; //$NON-NLS-1$
	String STEP_IN = "in"; //$NON-NLS-1$
	String STEP_NEXT = "next"; //$NON-NLS-1$
	String STEP_OUT = "out"; //$NON-NLS-1$
	String STEP_ANY = "any"; //$NON-NLS-1$
	String CONTEXTS = "contexts"; //$NON-NLS-1$
	String STATE = "state"; //$NON-NLS-1$
	String FRAMES = "frames"; //$NON-NLS-1$

	// target
	String THREADS = "threads"; //$NON-NLS-1$
	String THREAD = "thread"; //$NON-NLS-1$
	String VERSION = "version"; //$NON-NLS-1$
	String DISPOSE = "dispose"; //$NON-NLS-1$
	String SUSPEND = "suspend"; //$NON-NLS-1$
	String SUSPENDED = "suspended"; //$NON-NLS-1$
	String EXCEPTION = "exception"; //$NON-NLS-1$
	String VM_VERSION = "javascript.vm.version"; //$NON-NLS-1$
	String VM_NAME = "javascript.vm.name"; //$NON-NLS-1$
	String VM_VENDOR = "javascript.vm.vendor"; //$NON-NLS-1$
	String JAVASCRIPT_VERSION = "javascript.version"; //$NON-NLS-1$
	String ECMASCRIPT_VERSION = "ecmascript.version"; //$NON-NLS-1$

	// stackframe
	String EVALUATE = "evaluate"; //$NON-NLS-1$
	String EXPRESSION = "expression"; //$NON-NLS-1$
	String PROPERTIES = "properties"; //$NON-NLS-1$
	String NAME = "name"; //$NON-NLS-1$
	String LOOKUP = "lookup"; //$NON-NLS-1$
	String REF = "ref"; //$NON-NLS-1$
	String CONSTRUCTOR = "constructor"; //$NON-NLS-1$
	String CLASS_NAME = "className"; //$NON-NLS-1$
	String VALUE = "value"; //$NON-NLS-1$
	String SCOPE_NAME = "scopeName"; //$NON-NLS-1$
	String FRAME = "frame"; //$NON-NLS-1$

	// scripts
	String GENERATED = "generated"; //$NON-NLS-1$
	String SOURCE = "source"; //$NON-NLS-1$
	String LOCATION = "location"; //$NON-NLS-1$
	String BASE = "base"; //$NON-NLS-1$
	String PATH = "path"; //$NON-NLS-1$
}
