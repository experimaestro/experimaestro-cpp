package sf.net.experimaestro.manager.js;

import static java.lang.String.format;

import java.util.Map;
import java.util.TreeMap;

import javax.xml.namespace.QName;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import sf.net.experimaestro.exceptions.ExperimaestroException;
import sf.net.experimaestro.manager.AlternativeInput;
import sf.net.experimaestro.manager.AlternativeType;
import sf.net.experimaestro.manager.DotName;
import sf.net.experimaestro.manager.Input;
import sf.net.experimaestro.manager.Manager;
import sf.net.experimaestro.manager.Repository;
import sf.net.experimaestro.manager.Task;
import sf.net.experimaestro.manager.TaskFactory;
import sf.net.experimaestro.manager.TaskInput;
import sf.net.experimaestro.manager.Type;
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
			String idAtt = el.getAttribute("id");
			if (idAtt == null)
				throw new RuntimeException(format("Input without id in %s",
						this.id));
			LOGGER.info("New attribute %s for task %s", idAtt, this.id);

			QName typeName = XMLUtils.parseQName(el.getAttribute("type"), el,
					Manager.PREDEFINED_PREFIXES);

			String optional = el.getAttribute("optional");
			boolean isOptional = optional != null && optional.equals("true") ? true
					: false;

			// Get the documentation
			String documentation = "";
			if (el.hasAttribute("help"))
				documentation = el.getAttribute("help");
			else {
				Node child = XMLUtils.getChild(el, new QName(
						Manager.EXPERIMAESTRO_NS, "help"));
				if (child != null)
					documentation = XMLUtils.toString(child);
			}

			// Add this to the list of inputs
			addInput(el, repository, idAtt, typeName, isOptional, documentation);

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

		init();

	}

	/**
	 * Add an input
	 * 
	 * @param el
	 * 
	 * @param repository
	 *            The repository
	 * @param id
	 *            The id of the input
	 * @param typeName
	 *            The name of the type
	 * @param isOptional
	 *            Is this type optional?
	 * @param documentation
	 *            Type documentation
	 */
	private void addInput(Element el, Repository repository, String id,
			QName typeName, boolean isOptional, String documentation) {
		LOGGER.info("Looking at [%s] of type [%s]", id, typeName);
		Type type = repository.getType(typeName);
		if (type instanceof AlternativeType) {
			LOGGER.info(
					"Detected an alternative type configuration for input [%s] of type [%s]",
					id, typeName);
			inputs.put(id, new AlternativeInput(typeName, isOptional,
					documentation, (AlternativeType) type));
		} else if (el.getNodeName().equals("task")) {
			// The input is a task
			TaskFactory factory = repository.getFactory(typeName);
			if (factory == null)
				throw new ExperimaestroException(
						"Could not find task factory with type [%s]", typeName);

			inputs.put(id, new TaskInput(factory, typeName, isOptional,
					documentation));

		} else {
			// The input is just XML
			inputs.put(id, new Input(typeName, isOptional, documentation));
		}

		// --- Check connections
		for (Element connect : XMLUtils.childIterator(el, new QName(
				Manager.EXPERIMAESTRO_NS, "connect"))) {
			final String from = connect.getAttribute("from");
			final String path = connect.getAttribute("path");
			final DotName to = new DotName(id, DotName.parse(connect.getAttribute("to")));
			LOGGER.info("Found connection between [%s in %s] and [%s]", path,
					from, to);
			Input input = inputs.get(from);
			if (input == null)
				throw new ExperimaestroException(
						"Could not find input [%s] in [%s]", from, this.id);
			
			input.addConnection(path, to, connect);
		}
	}

	private static QName getQName(Scriptable scope, NativeObject jsObject) {
		NativeJavaObject object = (NativeJavaObject) JSUtils.get(scope, "id",
				jsObject);
		return (QName) object.unwrap();
	}

	@Override
	public String getDocumentation() {
		return JSUtils.get(jsScope, "description", jsObject).toString();
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
			if (!(function instanceof Function))
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
