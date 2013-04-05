/*******************************************************************************
 * Copyright (c) 2010, 2012 IBM Corporation and others All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.debug.internal.rhino.debugger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.wst.jsdt.debug.internal.rhino.transport.JSONConstants;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.debug.DebugFrame;
import org.mozilla.javascript.debug.DebuggableObject;
import org.mozilla.javascript.debug.Debugger;

/**
 * Rhino implementation of {@link DebugFrame}
 * 
 * @since 1.0
 */
public class StackFrame implements DebugFrame {

	private final Long id;
	private final Context context;
	private final ContextData contextData;
	private final ScriptSource script;
	private FunctionSource function = null;
	private final HashMap handles = new HashMap();
	private final IdentityHashMap handledObjects = new IdentityHashMap();
	private Scriptable activation;
	private Scriptable thisObj;
	private int lineNumber;

	/**
	 * Constructor
	 * 
	 * @param frameId
	 * @param context
	 * @param debuggableScript
	 * @param script
	 */
	public StackFrame(Long frameId, Context context, FunctionSource function, ScriptSource script) {
		this.id = frameId;
		this.context = context;
		this.contextData = (ContextData) context.getDebuggerContextData();
		this.function = function;
		this.script = script;
		if(function != null) {
			this.lineNumber = function.linenumber();
		}
		else {
			this.lineNumber = script.firstLine().intValue();
		}
	}

	/**
	 * Returns the id of the frame
	 * 
	 * @return the frame id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * Returns the underlying {@link Script}
	 * 
	 * @return the underlying {@link Script}
	 */
	public ScriptSource getScript() {
		return script;
	}

	/**
	 * Returns the line number for the frame
	 * 
	 * @return the frame line number
	 */
	public Integer getLineNumber() {
		return new Integer(lineNumber);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mozilla.javascript.debug.DebugFrame#onDebuggerStatement(org.mozilla.javascript.Context)
	 */
	public void onDebuggerStatement(Context cx) {
		initializeHandles();
		this.lineNumber = 1+lineNumber;
		contextData.debuggerStatement(script, new Integer(lineNumber));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mozilla.javascript.debug.DebugFrame#onEnter(org.mozilla.javascript.Context, org.mozilla.javascript.Scriptable, org.mozilla.javascript.Scriptable, java.lang.Object[])
	 */
	public void onEnter(Context cx, Scriptable activation, Scriptable thisObj, Object[] args) {
		this.activation = activation;
		this.thisObj = thisObj;
		initializeHandles();
		contextData.pushFrame(this, this.script, new Integer(lineNumber), getFunctionName());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mozilla.javascript.debug.DebugFrame#onExit(org.mozilla.javascript.Context, boolean, java.lang.Object)
	 */
	public void onExit(Context cx, boolean byThrow, Object resultOrException) {
		this.activation = null;
		this.thisObj = null;
		clearHandles();
		this.contextData.popFrame(byThrow, resultOrException);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mozilla.javascript.debug.DebugFrame#onExceptionThrown(org.mozilla.javascript.Context, java.lang.Throwable)
	 */
	public void onExceptionThrown(Context cx, Throwable ex) {
		initializeHandles();
		this.contextData.exceptionThrown(ex);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mozilla.javascript.debug.DebugFrame#onLineChange(org.mozilla.javascript.Context, int)
	 */
	public void onLineChange(Context cx, int lineNumber) {
		if (this.lineNumber == lineNumber)
			return;
		initializeHandles();
		this.lineNumber = lineNumber;
		Integer line = new Integer(this.lineNumber);
		if(this.script.isValid(line, getFunctionName())) {
			this.contextData.lineChange(this.script, line);
		}
	}

	/**
	 * @return the name of the function backing this frame or <code>null</code> if there is no function
	 * or it has no name.
	 */
	String getFunctionName() {
		return (this.function == null ? null : this.function.name());
	}
	
	/**
	 * Evaluates the given source
	 * 
	 * @param source
	 * @return
	 */
	public Object evaluate(String source) {
		RhinoDebuggerImpl rhinoDebugger = (RhinoDebuggerImpl) context.getDebugger();
		rhinoDebugger.disableThread();

		Context evalContext = context.getFactory().enterContext();
		Debugger debugger = evalContext.getDebugger();
		Object debuggerContextData = evalContext.getDebuggerContextData();
		evalContext.setDebugger(null, null);
		try {
			Object result = ScriptRuntime.evalSpecial(evalContext, activation, thisObj, new Object[] { source }, "eval", 0); //$NON-NLS-1$
			Long handle = createHandle(result);
			return serialize(handle, result);
		}
		catch (EcmaError ecma) {
			return null;
		}
		finally {
			evalContext.setDebugger(debugger, debuggerContextData);
			Context.exit();
			rhinoDebugger.enableThread();
		}
	}

	/**
	 * Evaluates the given condition
	 * 
	 * @param condition
	 * @return the status of the condition evaluation
	 */
	public boolean evaluateCondition(String condition) {
		RhinoDebuggerImpl rhinoDebugger = (RhinoDebuggerImpl) context.getDebugger();
		rhinoDebugger.disableThread();

		Context evalContext = context.getFactory().enterContext();
		Debugger debugger = evalContext.getDebugger();
		Object debuggerContextData = evalContext.getDebuggerContextData();
		evalContext.setDebugger(null, null);
		try {
			Object result = ScriptRuntime.evalSpecial(evalContext, activation, thisObj, new Object[] { condition }, JSONConstants.CONDITION, 0);
			return ScriptRuntime.toBoolean(result);
		} finally {
			evalContext.setDebugger(debugger, debuggerContextData);
			Context.exit();
			rhinoDebugger.enableThread();
		}
	}

	/**
	 * Look up the given handle in the known list of handles for this frame
	 * 
	 * @param handle
	 * @return the serialized handle never <code>null</code>
	 */
	public Object lookup(Long handle) {

		Object result = handles.get(handle);

		RhinoDebuggerImpl rhinoDebugger = (RhinoDebuggerImpl) context.getDebugger();
		rhinoDebugger.disableThread();

		Context lookupContext = context.getFactory().enterContext();
		Debugger debugger = lookupContext.getDebugger();
		Object debuggerContextData = lookupContext.getDebuggerContextData();
		lookupContext.setDebugger(null, null);
		try {
			return serialize(handle, result);
		} finally {
			lookupContext.setDebugger(debugger, debuggerContextData);
			Context.exit();
			rhinoDebugger.enableThread();
		}
	}

	/**
	 * Returns a JSON map
	 * 
	 * @return a new JSON object
	 */
	public Object toJSON() {
		Map result = new HashMap();
		result.put(JSONConstants.THREAD_ID, contextData.getThreadId());
		result.put(JSONConstants.CONTEXT_ID, contextData.getId());
		result.put(JSONConstants.FRAME_ID, id);
		result.put(JSONConstants.SCRIPT_ID, script.getId());
		result.put(JSONConstants.LINE, new Integer(lineNumber));
		result.put(JSONConstants.REF, new Integer(0));
		//TODO update this
		result.put(JSONConstants.SCOPE_NAME, null);
		return result;
	}

	/**
	 * Serializes a handle object for this frame
	 * 
	 * @param handle
	 * @param object
	 * @return the serialized handle, never <code>null</code>
	 */
	public Object serialize(Long handle, Object object) {
		Map result = new HashMap();
		result.put(JSONConstants.REF, handle);

		// "undefined", "null", "boolean", "number", "string", "object", "function" or "frame"
		if (object == Undefined.instance) {
			serializeUndefined(result);
		} else if (object == null) {
			serializeNull(result);
		} else if (object instanceof Boolean) {
			serializeSimpleType(object, JSONConstants.BOOLEAN, result);
		} else if (object instanceof Number) {
			Object value = (object == ScriptRuntime.NaNobj) ? null : object;
			serializeSimpleType(value, JSONConstants.NUMBER, result);
		} else if (object instanceof CharSequence) {
			serializeSimpleType(object, JSONConstants.STRING, result);
		} else if (object instanceof Scriptable) {
			Scriptable scriptable = (Scriptable) object;
			serializeFunctionOrObject(scriptable, result);
		} else if (object == this) {
			serializeFrame(result);
		} else {
			serializeUndefined(result);
		}
		return result;
	}

	/**
	 * Serialize the undefined value
	 * 
	 * @param result
	 * @see JSONConstants#UNDEFINED
	 */
	private void serializeUndefined(Map result) {
		result.put(JSONConstants.TYPE, JSONConstants.UNDEFINED);
	}

	/**
	 * Serialize the null value
	 * 
	 * @param result
	 * @see JSONConstants#NULL
	 */
	private void serializeNull(Map result) {
		result.put(JSONConstants.TYPE, JSONConstants.NULL);
	}

	/**
	 * Serialize the given simple type
	 * 
	 * @param object
	 * @param type
	 * @param result
	 */
	private void serializeSimpleType(Object object, String type, Map result) {
		result.put(JSONConstants.TYPE, type);
		result.put(JSONConstants.VALUE, object);
	}

	/**
	 * Serialize a function or object
	 * 
	 * @param scriptable
	 * @param result
	 * @see JSONConstants#FUNCTION
	 * @see JSONConstants#OBJECT
	 */
	private void serializeFunctionOrObject(Scriptable scriptable, Map result) {
		if (scriptable instanceof BaseFunction) {
			result.put(JSONConstants.TYPE, JSONConstants.FUNCTION);
			result.put(JSONConstants.NAME, ((BaseFunction) scriptable).getFunctionName());
		} else if (scriptable instanceof NativeArray) {
			result.put(JSONConstants.TYPE, JSONConstants.ARRAY);
		} else {
			result.put(JSONConstants.TYPE, JSONConstants.OBJECT);
		}
		result.put(JSONConstants.CLASS_NAME, scriptable.getClassName());

		Object constructorFunction = null;
		if (ScriptableObject.hasProperty(scriptable, JSONConstants.CONSTRUCTOR)) {
			constructorFunction = ScriptableObject.getProperty(scriptable, JSONConstants.CONSTRUCTOR);
		}
		result.put(JSONConstants.CONSTRUCTOR_FUNCTION, createHandle(constructorFunction));
		result.put(JSONConstants.PROTOTYPE_OBJECT, createHandle(scriptable.getPrototype()));
		if (scriptable instanceof NativeJavaObject)
			result.put(JSONConstants.PROPERTIES, createJavaObjectProperties((NativeJavaObject) scriptable));
		else
			result.put(JSONConstants.PROPERTIES, createProperties(scriptable));
	}

	/**
	 * @param javaObject
	 * @return
	 */
	private Object createJavaObjectProperties(NativeJavaObject javaObject) {
		ArrayList properties = new ArrayList();
		// TODO: The problem here is Rhino treats getters and setters differently and in some cases will call these methods
		// we need to sort out what's reasonable to display without modifying state
		return properties;
	}

	/**
	 * Serialize a frame
	 * 
	 * @param result
	 * @see JSONConstants#FRAME
	 * @see JSONConstants#THIS
	 */
	private void serializeFrame(Map result) {
		result.put(JSONConstants.TYPE, JSONConstants.FRAME);
		Set properties = new HashSet();
		properties.add(createProperty(JSONConstants.THIS, thisObj));
		properties.addAll(createProperties(activation));
		Scriptable parent = activation.getParentScope();
		while(parent != null) {
			properties.addAll(createProperties(parent));
			parent = parent.getParentScope();
		}		
		result.put(JSONConstants.PROPERTIES, properties);
	}

	/**
	 * Creates the list of properties from the given {@link Scriptable}
	 * 
	 * @param scriptable
	 * @return the live list of properties from the given {@link Scriptable}
	 */
	private List createProperties(Scriptable scriptable) {
		ArrayList properties = new ArrayList();
		Object[] ids = scriptable.getIds();
		if (scriptable instanceof DebuggableObject) {
			HashSet arrayIds = new HashSet(Arrays.asList(ids));
			arrayIds.addAll(Arrays.asList(((DebuggableObject)scriptable).getAllIds()));
			ids = arrayIds.toArray();
		}
		for (int i = 0; i < ids.length; i++) {
			Object id = ids[i];
			Object value = null;
			try {
				if (id instanceof String) {
					value = ScriptableObject.getProperty(scriptable, (String) id);
				} else if (id instanceof Number) {
					value = ScriptableObject.getProperty(scriptable, ((Number) id).intValue());
				} else {
					continue;
				}
			}
			catch(Exception e) {
				value = e.getLocalizedMessage();
			}
			Map property = createProperty(id, value);
			properties.add(property);
		}
		return properties;
	}

	/**
	 * Create a new property map for the given id and value
	 * 
	 * @param id
	 * @param value
	 * @return a new property map
	 * @see JSONConstants#NAME
	 */
	private Map createProperty(Object id, Object value) {
		Map property = createRef(value);
		property.put(JSONConstants.NAME, id);
		return property;
	}

	/**
	 * Create a new ref map for the given object
	 * 
	 * @param object
	 * @return a new ref map
	 * @see JSONConstants#REF
	 */
	private Map createRef(Object object) {
		Map map = new HashMap(2);
		map.put(JSONConstants.REF, createHandle(object));
		return map;
	}

	/**
	 * Clears all cached handles from this frame
	 */
	private void clearHandles() {
		handles.clear();
		handledObjects.clear();
	}

	/**
	 * Initializes the set of handles
	 */
	private void initializeHandles() {
		if (handles.size() != 1) {
			clearHandles();
			createHandle(this);
		}
	}

	/**
	 * Creates a new handle for the given object and caches it
	 * 
	 * @param object
	 * @return the id of the new handle
	 */
	private Long createHandle(Object object) {
		Long handle = (Long) handledObjects.get(object);
		if (handle == null) {
			handle = new Long(nextHandle());
			handles.put(handle, object);
			handledObjects.put(object, handle);
		}
		return handle;
	}

	/**
	 * @return the next handle to use when creating handles
	 */
	private int nextHandle() {
		return handles.size();
	}

	/**
	 * @return the thread id for the underlying {@link ContextData}
	 */
	public Object getThreadId() {
		return contextData.getThreadId();
	}
}