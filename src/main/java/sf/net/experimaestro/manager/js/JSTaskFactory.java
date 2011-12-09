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

import static java.lang.String.format;

import java.util.Map;
import java.util.TreeMap;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import sf.net.experimaestro.exceptions.ExperimaestroException;
import sf.net.experimaestro.manager.AlternativeInput;
import sf.net.experimaestro.manager.AlternativeType;
import sf.net.experimaestro.manager.DotName;
import sf.net.experimaestro.manager.Input;
import sf.net.experimaestro.manager.Manager;
import sf.net.experimaestro.manager.Module;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.manager.Repository;
import sf.net.experimaestro.manager.Task;
import sf.net.experimaestro.manager.TaskFactory;
import sf.net.experimaestro.manager.TaskInput;
import sf.net.experimaestro.manager.Type;
import sf.net.experimaestro.manager.XMLInput;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.XMLUtils;
import sf.net.experimaestro.utils.log.Logger;

/**
 * A task factory as defined by a JavaScript object
 * 
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class JSTaskFactory extends TaskFactory {
	final static private Logger LOGGER = Logger.getLogger();

	/**
	 * The scope
	 */
	Scriptable jsScope;

	/**
	 * The server
	 */
	protected NativeObject jsObject;

	/**
	 * The inputs
	 */
	protected Map<String, Input> inputs;

	/**
	 * The outputs
	 */
	private TreeMap<String, QName> outputs;

	/**
	 * Creates a new task information from a javascript object
	 * 
	 * @param context
	 *            The context
	 * @param scope
	 *            The scope
	 * @param jsObject
	 *            The object
	 */
	public JSTaskFactory(Scriptable scope, NativeObject jsObject,
			Repository repository) {
		super(repository, getQName(scope, jsObject), JSUtils.get(scope,
				"version", jsObject, "1.0"), null);
		this.jsScope = scope;
		this.jsObject = jsObject;

		// --- Look up the module
		Module module = JSModule.getModule(repository,
				JSUtils.get(jsScope, "module", (NativeObject) jsObject, null));
		if (module != null)
			setModule(module);

		// --- Get the task inputs
		Object input = JSUtils.get(scope, "inputs", jsObject);
		inputs = new TreeMap<String, Input>();
		if (!JSUtils.isXML(input))
			throw new RuntimeException(format(
					"Property input is not in XML format in %s (%s)", this.id,
					input.getClass()));

		LOGGER.info("Class is %s in %s", input.getClass(), this.id);
		Node dom = JSUtils.toDOM(input);
		NodeList list = dom.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node item = list.item(i);
			if (item.getNodeType() != Node.ELEMENT_NODE)
				continue;
			Element el = (Element) item;
			addInput(el, repository);

		}

		// --- Process connections
		for (int i = 0; i < list.getLength(); i++) {
			Node item = list.item(i);
			if (item.getNodeType() != Node.ELEMENT_NODE)
				continue;
			Element el = (Element) item;
			final String inputToId = el.getAttribute("id");
			if (inputs.get(inputToId) == null)
				throw new AssertionError();

			// Add this to the list of inputs
			for (Element connect : XMLUtils.childIterator(el, new QName(
					Manager.EXPERIMAESTRO_NS, "connect"))) {
				final DotName from = DotName
						.parse(connect.getAttribute("from"));
				final String path = connect.getAttribute("path");
				if ("".equals(path))
					throw new ExperimaestroException(
							"Attribute path has to be defined (and not at the same time)");
				final DotName to = new DotName(inputToId, DotName.parse(connect
						.getAttribute("to")));
				LOGGER.info("Found connection between [%s in %s] and [%s]",
						path, from, to);
				Input inputFrom = inputs.get(from.get(0));

				if (inputFrom == null)
					throw new ExperimaestroException(
							"Could not find input [%s] in [%s]", from.get(0),
							this.id);

				inputFrom.addConnection(from.offset(1), path, to, connect);
			}
		}

		// --- Get the task outputs
		Object output = JSUtils.get(scope, "outputs", jsObject);
		outputs = new TreeMap<String, QName>();
		if (!JSUtils.isXML(output))
			throw new RuntimeException(format(
					"Property output is not in XML format in %s (%s)", this.id,
					input.getClass()));

		dom = JSUtils.toDOM(output);
		list = dom.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node item = list.item(i);
			if (item.getNodeType() != Node.ELEMENT_NODE)
				continue;
			Element el = (Element) item;

			String id = el.getAttribute("id");
			QName typeName = XMLUtils.parseQName(el.getAttribute("type"), el,
					Manager.PREDEFINED_PREFIXES);
			LOGGER.info("Output [%s] is of type [%s]", id, typeName);
			outputs.put(id, typeName);
		}

		// --- Are we an alternative?

		QName altType = JSUtils.get(jsScope, "alternative", jsObject, null);
		if (altType != null) {
			if (outputs.size() != 1)
				throw new ExperimaestroException(
						"Wrong number of outputs (%d) to be an alternative",
						outputs.size());
			QName qname = outputs.values().iterator().next();

			Type type = repository.getType(qname);
			if (type == null || !(type instanceof AlternativeType))
				throw new ExperimaestroException(
						"Type %s is not an alternative", qname == null ? "null"
								: qname.toString());

			((AlternativeType) type).add(id, this);
			return;
		}

		init();

	}

	/**
	 * Add an input
	 * 
	 * @param el
	 * 
	 * @param repository
	 *            The repository
	 */
	private void addInput(Element el, Repository repository) {
		// Get the id
		String id = el.getAttribute("id");
		if (id == null)
			throw new RuntimeException(
					format("Input without id in %s", this.id));

		LOGGER.debug("New input [%s] for task [%s]", id, this.id);

		final String typeAtt = el.getAttribute("type");
		QName typeName = !typeAtt.equals("") ? XMLUtils.parseQName(typeAtt, el,
				Manager.PREDEFINED_PREFIXES) : new QName(
				Manager.EXPERIMAESTRO_NS, "xml");

		// Choose the appropriate input type
		LOGGER.debug("Looking at [%s] of type [%s]", id, typeName);
		final Input input;

		Type type = repository.getType(typeName);

		if (type instanceof AlternativeType) {
			LOGGER.debug(
					"Detected an alternative type configuration for input [%s] of type [%s]",
					id, typeName);
			input = new AlternativeInput(typeName, (AlternativeType) type);
		} else if (el.getNodeName().equals("task")) {
			// The input is a task
			TaskFactory factory = repository.getFactory(typeName);
			if (factory == null)
				throw new ExperimaestroException(
						"Could not find task factory with type [%s]", typeName);

			input = new TaskInput(factory, typeName);

		} else {
			// The input is just XML
			input = new XMLInput(typeName);
		}

		if ("false".equals(el.getAttribute("named")))
			input.setUnnamed(true);

		inputs.put(id, input);

		// Set the optional flag
		String optional = el.getAttribute("optional");
		boolean isOptional = optional != null && optional.equals("true") ? true
				: false;
		input.setOptional(isOptional);

		// Set the documentation
		if (el.hasAttribute("help"))
			input.setDocumentation(el.getAttribute("help"));
		else {
			Node child = XMLUtils.getChild(el, new QName(
					Manager.EXPERIMAESTRO_NS, "help"));
			if (child != null)
				input.setDocumentation(XMLUtils.toString(child));
		}

		// Set the default value
		if (el.hasAttribute("default")) {
			input.setDefaultValue(Task.wrapValue(el.getAttribute("default")));
		} else {
			Node child = XMLUtils.getChild(el, new QName(
					Manager.EXPERIMAESTRO_NS, "default"));
			if (child != null) {
				Document document = XMLUtils.newDocument();
				child = child.cloneNode(true);
				document.adoptNode(child);
				NodeList childNodes = child.getChildNodes();
				for (int i = 0; i < childNodes.getLength(); i++)
					document.appendChild(childNodes.item(i));
				input.setDefaultValue(document);
			}
		}

	}

	private static QName getQName(Scriptable scope, NativeObject jsObject) {
		return (QName) JSUtils.get(scope, "id", jsObject);
	}

	@Override
	public String getDocumentation() {
		final Object object = JSUtils.get(jsScope, "description", jsObject,
				null);
		if (object != null)
			return object.toString();
		return "";
	}

	@Override
	public Task create() {
		// Get the "create" method
		Object function = JSUtils.get(jsScope, "create", jsObject, null);

		// If we don't have one, then it might be a "direct" task, i.e.
		// not implying any object creation
		if (!(function instanceof Function)) {
			// Case of a configuration object
			function = JSUtils.get(jsScope, "run", jsObject, null);
			if (function != null && !(function instanceof Function))
				throw new RuntimeException(
						"Could not find the create or run functions.");

			JSDirectTask jsConfigurationTask = new JSDirectTask(this, jsScope,
					jsObject, (Function) function);
			jsConfigurationTask.init();
			return jsConfigurationTask;
		}

		// Call it
		Context jsContext = Context.getCurrentContext();
		Function f = (Function) function;
		Object result = f.call(jsContext, jsScope, jsScope, new Object[] {});
		LOGGER.info("Created a new experiment: %s (%s)", result,
				result.getClass());
		JSAbstractTask jsTask = new JSTask(this, jsContext, jsScope,
				(NativeObject) result);
		jsTask.init();
		return jsTask;
	}

	@Override
	public Map<String, Input> getInputs() {
		return inputs;
	}

	@Override
	public Map<String, QName> getOutputs() {
		return outputs;
	}

}
