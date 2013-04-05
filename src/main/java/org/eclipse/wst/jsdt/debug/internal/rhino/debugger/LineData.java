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
package org.eclipse.wst.jsdt.debug.internal.rhino.debugger;

/**
 * Class to hold information about a given line such as:
 * <ul>
 * <li>FunctionSource at the line</li>
 * <li>Breakpoint set on the line</li>
 * </ul>
 * @since 1.0
 */
public class LineData {

	Breakpoint breakpoint = null;
	FunctionSource function = null;
}
