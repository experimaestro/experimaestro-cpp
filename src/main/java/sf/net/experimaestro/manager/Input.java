/*
 * This file is part of experimaestro.
 * Copyright (c) 2012 B. Piwowarski <benjamin@bpiwowar.net>
 *
 * experimaestro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * experimaestro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 */

package sf.net.experimaestro.manager;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import sf.net.experimaestro.utils.log.Logger;

import javax.xml.xquery.XQException;
import javax.xml.xquery.XQStaticContext;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A parameter definition in a task factory / task
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public abstract class Input {
	final static Logger LOGGER = Logger.getLogger();

	/**
	 * Defines an optional parameter
	 */
	boolean optional;

	/**
	 * The type of the parameter
	 */
	Type type;

	/**
	 * Documentation for this parameter
	 */
	String documentation;

	/**
	 * Default value
	 */
	Document defaultValue;

	/**
	 * Unnamed option
	 */
	boolean unnamed;

	/**
	 * Returns whether the input is optional or not
	 * 
	 * @return
	 */
	public boolean isOptional() {
		return optional;
	}

	/**
	 * Get the documentation
	 * 
	 * @return A string in XHTML
	 */
	public String getDocumentation() {
		return documentation;
	}

	public Type getType() {
		return type;
	}

	/**
	 * New input type
	 * 
	 * @param type the type
	 */
	public Input(Type type) {
		this.type = type;
	}


	public void setOptional(boolean optional) {
		this.optional = optional;
	}

	public void setDocumentation(String documentation) {
		this.documentation = documentation;
	}

	abstract Value newValue();

	public void printHTML(PrintWriter out) {
		out.println(documentation);
	}

	public void setDefaultValue(Document defaultValue) {
		this.defaultValue = defaultValue;
	}

	/**
	 * Defines a connection to the
	 * 
	 * @author B. Piwowarski <benjamin@bpiwowar.net>
	 */
	static public class Connection {
		final DotName from;
		final String path;
		final DotName to;
		private NSContext context;
		private Map<String, String> namespaces;

		public Connection(DotName from, String path, DotName to, Element element) {
			this.from = from;
			this.path = path;
			this.to = to;
			this.context = new NSContext(element);
			namespaces = Manager.getNamespaces(element);
		}

		public NSContext getContext() {
			return context;
		}

		public void setNamespaces(XQStaticContext xqsc) throws XQException {
			for (Entry<String, String> mapping : namespaces.entrySet()) {
				LOGGER.debug("Setting default namespace mapping [%s] to [%s]",
						mapping.getKey(), mapping.getValue());
				xqsc.declareNamespace(mapping.getKey(), mapping.getValue());
			}
		}

	}

	ArrayList<Connection> connections = new ArrayList<Input.Connection>();

	public void addConnection(DotName from, String path, DotName to,
			Element element) {
		connections.add(new Connection(from, path, to, element));
	}

	public boolean isUnnamed() {
		return unnamed;
	}

	public void setUnnamed(boolean unnamed) {
		this.unnamed = unnamed;
	}

}
