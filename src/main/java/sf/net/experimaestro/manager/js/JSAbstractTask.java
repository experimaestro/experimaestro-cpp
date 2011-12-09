/*
 *
 *  This file is part of experimaestro.
 *  Copyright (c) 2011 B. Piwowarski <benjamin@bpiwowar.net>
 *
 *  experimaestro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  experimaestro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package sf.net.experimaestro.manager.js;

import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import sf.net.experimaestro.manager.Task;
import sf.net.experimaestro.manager.TaskFactory;
import sf.net.experimaestro.manager.Value;
import sf.net.experimaestro.utils.JSUtils;

public abstract class JSAbstractTask extends Task {
	protected Scriptable jsScope;

	public JSAbstractTask(TaskFactory information, Scriptable jsScope) {
		super(information);
		this.jsScope = jsScope;
	}
	
	protected JSAbstractTask() {
	}
	
	@Override
	protected void init(Task other) {
		super.init(other);
		jsScope = ((JSAbstractTask)other).jsScope;
	}

	/**
	 * Convert a DOM element into a E4X value
	 * 
	 * @param value
	 * @return
	 */
	protected Object toE4X(Element value) {
		int nodeType = value.getNodeType();
		Object jsInput = null;
		Context jsContext = Context.getCurrentContext();
		if (nodeType == Node.ELEMENT_NODE) {
			jsInput = JSUtils.domToE4X(value, jsContext, jsScope);
		} else if (nodeType == Node.DOCUMENT_FRAGMENT_NODE) {
			// Embed in a list
			NodeList childNodes = value.getChildNodes();
			Object[] nodes = new Scriptable[childNodes.getLength()];
			for (int i = 0; i < nodes.length; i++)
				nodes[i] = JSUtils.domToE4X(childNodes.item(i), jsContext,
						jsScope);
			jsInput = jsContext.newObject(jsScope, "XMLList", nodes);
		}

		if (jsInput == null)
			throw new RuntimeException("Cannot handle type " + nodeType);
		return jsInput;
	}

	protected Document getDocument(Object result) {
		// Get node
		Node node = JSUtils.toDOM(result);

		if (node instanceof Document)
			return (Document) node;

		// Just a node - convert do document

		// first of all we request out
		// DOM-implementation:
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		// then we have to create document-loader:
		DocumentBuilder loader;
		try {
			loader = factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new RuntimeException();
		}

		// creating a new DOM-document...
		Document document = loader.newDocument();
		node = node.cloneNode(true);
		document.adoptNode(node);
		document.appendChild(node);
		return document;
	}

	@Override
	public Document doRun() {
		return getDocument(jsrun(false));
	}

	abstract protected Object jsrun(boolean convertToE4X);

	protected Scriptable getJSInputs() {
		Context cx = Context.getCurrentContext();
		Scriptable jsInputs = cx.newObject(jsScope, "Object", new Object[] {});
		for (Entry<String, Value> entry : values.entrySet()) {
			String id = entry.getKey();
			Value value = entry.getValue();
			final Object e4x = JSUtils.domToE4X(value.get(), cx, jsScope);
			jsInputs.put(id, jsInputs, e4x);
		}
		return jsInputs;
	}

}