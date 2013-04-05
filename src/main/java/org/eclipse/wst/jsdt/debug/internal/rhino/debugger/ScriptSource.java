/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.debug.internal.rhino.debugger;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.wst.jsdt.debug.internal.rhino.transport.Constants;
import org.eclipse.wst.jsdt.debug.internal.rhino.transport.JSONConstants;
import org.mozilla.javascript.debug.DebuggableScript;
import org.mozilla.javascript.debug.Debugger;

/**
 * Rhino script implementation
 * 
 * @since 1.0
 */
public class ScriptSource {

	/**
	 * No line information
	 */
	static final LineData[] NO_LINES = new LineData[0];
	
	/**
	 * The id of the script
	 */
	private Long scriptId;
	/**
	 * Any special source properties to consider
	 */
	private final Map properties;
	/**
	 * The backing source String that was compiled to give us this proxy
	 */
	private final String source;
	/**
	 * If this {@link ScriptSource} was generated
	 */
	private final boolean generated;
	/**
	 * The ordered list of function names
	 */
	private ArrayList functionNames = null;

	private Integer firstLine = null;
	
	/**
	 * The location of the script
	 */
	private URI uri = null;
	/**
	 * The array of cached {@link FunctionSource}s
	 */
	private FunctionSource[] functionSources = null;
	/**
	 * Sparse array of line information. This array only has entries present for 
	 * lines that are returned 
	 */
	private LineData[] lines = null;
	/**
	 * Mapping of the {@link DebuggableScript} backing the FunctionSource to the FunctionSource handle
	 */
	private HashMap/*<DebuggableScript, FunctionSource>*/ scriptToFunction = new HashMap();
	
	/**
	 * Constructor
	 * 
	 * @param scriptId
	 * @param debuggableScript
	 * @param source
	 */
	public ScriptSource(DebuggableScript script, String source, URI uri, boolean generated, Map properties) {
		this.uri = uri;
		this.source = source;
		this.properties = properties;
		this.generated = generated;

		if(isStdIn()) {
			//do not compute line data
			return;
		}
		int[] rootlines = script.getLineNumbers();		
		DebuggableScript[] functions = collectFunctions(script);
		int flength = functions.length;
		int max = 0;
		int min = Integer.MAX_VALUE;
		HashSet lineNumbers = new HashSet(flength+rootlines.length+1);
		//dump in the line #'s from the root script
		for (int i = 0; i < rootlines.length; i++) {
			int line = rootlines[i];
			if(line > max) {
				max = line;
			}
			if(line < min) {
				min = line;
			}
			lineNumbers.add(new Integer(line));
		}
		//dump in the line numbers from the expanded set of functions
		if(flength > 0) {
			functionSources = new FunctionSource[flength];
			functionNames = new ArrayList(flength);
			
			int start = 0, end = 0;;
			for (int i = 0; i < functions.length; i++) {
				int[] lines = functions[i].getLineNumbers();
				if(lines != null && lines.length > 0) {
					start = lines[0] + 1;
					for (int j = 0; j < lines.length; j++) {
						int currentLine = lines[j];
						if(currentLine > max) {
							max = currentLine;
						}
						if(currentLine < min) {
							min = currentLine;
						}
						if (currentLine < start) {
							start = currentLine;
						}
						else if(currentLine > end) {
							end = currentLine;
						}
						lineNumbers.add(new Integer(currentLine));
					}
				}
				String name = functions[i].getFunctionName();
				name = (name == null ? Constants.EMPTY_STRING : name);
				functionSources[i] = new FunctionSource(this, name, computeFunctionSource(0, 0, source), start);
				functionNames.add(name);
				scriptToFunction.put(functions[i], functionSources[i]);
				start = 0;
			}
		}
		//create the sparse array and populate the valid line numbers
		if(max == 0) {
			this.lines = NO_LINES;
		}
		else {
			this.lines = new LineData[max+1];
			for (Iterator iter = lineNumbers.iterator(); iter.hasNext();) {
				Integer value = (Integer) iter.next();
				this.lines[value.intValue()] = new LineData();
				iter.remove();
			}
			lineNumbers = null;
		}
		this.firstLine = new Integer(min);
	}

	/**
	 * Returns the first executable line in the script
	 * 
	 * @return the first executable line in the script
	 */
	public Integer firstLine() {
		return this.firstLine;
	}
	
	/**
	 * @return if this script represents the stdin script
	 */
	public boolean isStdIn() {
		return this.uri.toString().endsWith("stdin"); //$NON-NLS-1$
	}
	
	/**
	 * Clears the breakpoints from this script out of the given {@link Debugger}
	 */
	void clearBreakpoints(RhinoDebuggerImpl debugger) {
		if(this.lines != null) {
			for (int i = 0; i < this.lines.length; i++) {
				if(lines[i] != null) {
					Breakpoint bp = lines[i].breakpoint;
					if(bp != null) {
						debugger.clearBreakpoint(bp.breakpointId);
					}
				}
			}
		}
	}
	
	/**
	 * Collects all of the {@link DebuggableScript} objects for the functions
	 * @param root the root script
	 * @return the collected array of functions - {@link DebuggableScript} - objects or an empty array
	 */
	DebuggableScript[] collectFunctions(DebuggableScript root) {
		ArrayList functions = new ArrayList();
		collectFunctions(root, functions);
		int size = functions.size();
		if(size < 1) {
			return RhinoDebuggerImpl.NO_SCRIPTS;
		}
		DebuggableScript[] funcs = new DebuggableScript[size];
		functions.toArray(funcs);
		return funcs;
	}
	
	/**
	 * Recursively collects function {@link DebuggableScript}s
	 * 
	 * @param root
	 * @param collector
	 */
	void collectFunctions(DebuggableScript root, List collector) {
		if(root.isFunction()) {
			collector.add(root);
		}
		for (int i = 0; i < root.getFunctionCount(); i++) {
			collectFunctions(root.getFunction(i), collector);
		}
	}
	
	/**
	 * Computes the functions' source from the given compiled buffer
	 * 
	 * @param start
	 * @param end
	 * @param source
	 * @return the string for the source or <code>null</code> if it could not be computed
	 */
	String computeFunctionSource(int start, int end, String source) {
		if(start > -1 && end <= source.length()) {
			return source.substring(start, end);
		}
		return null;
	}
	
	/**
	 * Sets the id for the script, <code>null</code> will throw and {@link IllegalArgumentException}
	 * @param id
	 * @throws IllegalArgumentException if <code>null</code> is specified as the new id
	 */
	public void setId(Long id) throws IllegalArgumentException {
		if(id == null) {
			throw new IllegalArgumentException();
		}
		this.scriptId = id;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if(obj instanceof ScriptSource) {
			return this.uri.toString().equals(((ScriptSource)obj).uri.toString());
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return this.uri.toString().hashCode();
	}

	/**
	 * @return a new JSON map
	 */
	public Object toJSON() {
		HashMap result = new HashMap();
		result.put(JSONConstants.SCRIPT_ID, this.scriptId);
		result.put(JSONConstants.LOCATION, this.uri.toString());
		result.put(JSONConstants.PROPERTIES, properties);
		result.put(JSONConstants.SOURCE, source);
		result.put(JSONConstants.GENERATED, Boolean.valueOf(generated));
		if(lines != null) {
			HashSet lineNumbers = new HashSet();
			for (int i = 0; i < lines.length; i++) {
				if(lines[i] != null) {
					lineNumbers.add(new Integer(i));
				}
			}
			result.put(JSONConstants.LINES, (lineNumbers == null ? Collections.EMPTY_SET : lineNumbers));
		}
		else {
			result.put(JSONConstants.LINES, Collections.EMPTY_SET);
		}
		result.put(JSONConstants.FUNCTIONS, (functionNames == null ? Collections.EMPTY_LIST : functionNames));
		return result;
	}

	/**
	 * @return the id for this script object
	 */
	public Long getId() {
		return scriptId;
	}
	
	/**
	 * Returns the {@link Breakpoint} at the given line number or <code>null</code>
	 * if there isn't one
	 * @param lineNumber
	 * @return
	 */
	public Breakpoint getBreakpoint(Integer line, String functionName) {
		synchronized (lines) {
			if(line != null) {
				int value = line.intValue();
				if (value > -1 && value < lines.length && lines[value] != null) {
					return lines[value].breakpoint;
				}
			}
			if(functionNames != null) {
				int index = functionNames.indexOf(functionName);
				FunctionSource func = functionAt(index);
				if(func != null && lines[func.linenumber()] != null) {
					return lines[func.linenumber()].breakpoint;
				}
			}
			return null;
		}
	}

	/**
	 * Returns if the line number is valid wrt this script
	 * 
	 * @param lineNumber
	 * @param functionName
	 * @return true if the line number is valid wrt this script
	 */
	public boolean isValid(Integer line, String functionName) {
		synchronized (lines) {
			if(line != null) {
				int value = line.intValue();
				if (value > -1 && value < lines.length) {
					return lines[value] != null;
				}
			}
			if(functionNames != null) {
				int index = functionNames.indexOf(functionName);
				FunctionSource func = functionAt(index);
				if(func != null) {
					return lines[func.linenumber()] != null;
				}
			}
		}
		return false;
	}
	
	/**
	 * Adds a breakpoint to this script
	 * 
	 * @param breakpoint
	 */
	public void addBreakpoint(Breakpoint breakpoint) {
		synchronized (lines) {
			Integer lineNumber = breakpoint.lineNumber;
			if(lineNumber != null) {
				int value = lineNumber.intValue();
				if(lines[value] != null) {
					lines[value].breakpoint = breakpoint;
				}
			}
			else if(functionNames != null) {
				int index = functionNames.indexOf(breakpoint.functionName);
				FunctionSource func = functionAt(index);
				if(func != null) {
					if(lines[func.linenumber] != null) {
						lines[func.linenumber].breakpoint = breakpoint;
					}
				}
			}
		}
	}

	/**
	 * Removes a breakpoint from this script
	 * 
	 * @param breakpoint
	 */
	public void removeBreakpoint(Breakpoint breakpoint) {
		synchronized (lines) {
			Integer lineNumber = breakpoint.lineNumber;
			if(lineNumber != null) {
				int value = lineNumber.intValue();
				if(lines[value] != null) {
					lines[value].breakpoint = null;
				}
			}
			else if(functionNames != null) {
				int index = functionNames.indexOf(breakpoint.functionName);
				FunctionSource func = functionAt(index);
				if(func != null) {
					if(lines[func.linenumber] != null) {
						lines[func.linenumber].breakpoint = null;
					}
				}
			}
		}
	}

	/**
	 * @return the string location of this script
	 */
	public String getLocation() {
		return this.uri.toString();
	}
	
	/**
	 * Returns the {@link FunctionSource} at the given position iff the given index
	 * is within the bounds of the sources array.
	 * 
	 * @param index
	 * @return the {@link FunctionSource} at the given index or <code>null</code>
	 */
	public FunctionSource functionAt(int index) {
		synchronized (functionSources) {
			if(functionSources != null && (index < functionSources.length && index > -1)) {
				return functionSources[index];
			}
		}
		return null;
	}
	
	/**
	 * Returns the {@link FunctionSource} for the given {@link DebuggableScript}
	 * 
	 * @param script
	 * @return the {@link FunctionSource} for the given {@link DebuggableScript} or <code>null</code>
	 */
	public FunctionSource getFunction(DebuggableScript script) {
		synchronized (scriptToFunction) {
			return (FunctionSource) scriptToFunction.get(script);
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("ScriptSource: [id - ").append(scriptId).append("] [uri - ").append(uri.toString()).append("]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		buffer.append("[generated - ").append(generated).append("]\n"); //$NON-NLS-1$ //$NON-NLS-2$
		if(lines != null) {
			buffer.append("\tline numbers: {"); //$NON-NLS-1$
			for (int i = 0; i < lines.length; i++) {
				if(lines[i] != null) {
					buffer.append(i);
					if(i < lines.length-1) {
						buffer.append(", "); //$NON-NLS-1$
					}
				}
			}
			buffer.append("}\n"); //$NON-NLS-1$
		}
		if(functionNames != null) {
			buffer.append("\tfunction names: {"); //$NON-NLS-1$
			for (int i = 0; i < functionNames.size(); i++) {
				buffer.append(functionNames.get(i));
				if(i < functionNames.size()-1) {
					buffer.append(", "); //$NON-NLS-1$
				}
			}
			buffer.append("}\n"); //$NON-NLS-1$
		}
		if(functionSources != null) {
			buffer.append("\tfunction sources:\n"); //$NON-NLS-1$
			for (int i = 0; i < functionSources.length; i++) {
				buffer.append(functionSources[i]).append("\n"); //$NON-NLS-1$
			}
		}
		return buffer.toString();
	}
}
