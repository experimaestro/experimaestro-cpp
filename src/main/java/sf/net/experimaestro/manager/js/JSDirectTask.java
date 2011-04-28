package sf.net.experimaestro.manager.js;

import java.util.Map.Entry;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import sf.net.experimaestro.exceptions.ExperimaestroException;
import sf.net.experimaestro.manager.Manager;
import sf.net.experimaestro.manager.Value;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.XMLUtils;
import sf.net.experimaestro.utils.log.Logger;

public class JSDirectTask extends JSAbstractTask {
	final static private Logger LOGGER = Logger.getLogger();

	private final Function runFunction;
	private final NativeObject jsObject;

	public JSDirectTask(JSTaskFactory jsTaskFactory, Scriptable jsScope,
			NativeObject jsObject, Function runFunction) {
		super(jsTaskFactory, jsScope, jsObject);
		this.jsObject = jsObject;
		this.runFunction = runFunction;

	}

	@Override
	public Object jsrun(boolean convertToE4X) {
		LOGGER.info("[Running] task: %s", factory.getId());

		final Context cx = Context.getCurrentContext();

		// Get the inputs
		Object result = null;

		if (runFunction != null) {
			// We have a run function
			Scriptable jsInputs = getJSInputs();
			final Object returned = runFunction.call(cx, jsScope, jsObject,
					new Object[] { jsInputs });
			LOGGER.info("Returned %s", returned);
			if (returned == Undefined.instance)
				throw new ExperimaestroException(
						"Undefined returned by the function run");

			result = (Scriptable) returned;
		} else {
			// We just copy the inputs as an output

			Document document = XMLUtils.newDocument();
			result = document;

			Element root = document.createElementNS(Manager.EXPERIMAESTRO_NS,
					"outputs");
			document.appendChild(root);

			for (Entry<String, Value> entry : values.entrySet()) {
				Value value = entry.getValue();
				if (value != null) {
					Element outputs = document.createElementNS(
							Manager.EXPERIMAESTRO_NS, "outputs");
					outputs.setIdAttributeNS(Manager.EXPERIMAESTRO_NS, "name", false);
					root.appendChild(outputs);
					
					Element returnRoot = (Element) value.get()
							.getDocumentElement().cloneNode(true);
					document.adoptNode(returnRoot);
					NodeList nodes = returnRoot.getChildNodes();
					for (int i = 0; i < nodes.getLength(); i++) {
						outputs.appendChild(nodes.item(i));
					}
				}
			}

			if (convertToE4X)
				result = JSUtils.domToE4X(document, cx, jsScope);
		}

		LOGGER.info("[/Running] task: %s", factory.getId());

		return result;
	}

}
