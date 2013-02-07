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

package sf.net.experimaestro.manager.js;

import org.apache.commons.lang.NotImplementedException;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.manager.*;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.String2String;
import sf.net.experimaestro.utils.XMLUtils;
import sf.net.experimaestro.utils.log.Logger;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import static java.lang.String.format;
import static sf.net.experimaestro.exceptions.ExperimaestroRuntimeException.SHOULD_NOT_BE_HERE;

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
    private Type output;


    public JSTaskFactory(Scriptable scope, NativeObject jsObject,
                         Repository repository) {
        this(getQName(scope, jsObject, "id", false), scope, jsObject, repository);
    }

    /**
     * Creates a new task information from a javascript object
     *
     * @param scope    The scope
     * @param jsObject The object
     */
    public JSTaskFactory(QName qname, Scriptable scope, NativeObject jsObject,
                         Repository repository) {
        super(repository, qname, JSUtils.get(scope,
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
        inputs = new TreeMap<>();

        if (JSUtils.isXML(input)) {
            Node dom = (Node) JSUtils.toDOM(null, input);
            setInputs(repository, dom);
        } else if (input instanceof NativeObject) {
            setInputs(scope, (NativeObject) input);
        } else
            throw new ExperimaestroRuntimeException("Cannot handle inputs of type %s", inputs.getClass());


        // --- Get the task outputs
        QName outQName = getQName(scope, jsObject, "output", true);
        if (outQName != null)
            output = new Type(outQName);


        // --- Are we an alternative?

        Boolean altType = JSUtils.get(jsScope, "alternative", jsObject, null);
        if (altType != null && altType) {
            if (output == null)
                throw new ExperimaestroRuntimeException("No output has been defined for an alternative");

            Type type = repository.getType(outQName);
            if (type == null || !(type instanceof AlternativeType))
                throw new ExperimaestroRuntimeException(
                        "Type %s is not an alternative", outQName == null ? "null"
                        : outQName.toString());

            ((AlternativeType) type).add(id, this);
            return;
        }

        init();

    }

    static public String onlyOne(Scriptable object, String... keys) {
        String selected = null;
        for (String key : keys) {
            if (object.has(key, object))
                if (selected == null)
                    selected = key;
                else
                    throw new ExperimaestroRuntimeException("Object has at least two conflicting properties: %s and %s",
                            selected, key);
        }
        if (selected == null)
            throw new ExperimaestroRuntimeException("Expected at least one property in %s", Arrays.toString(keys));
        return selected;
    }

    private void setInputs(final Scriptable scope, final NativeObject jsInput) {
        String2String prefixes = new JSNamespaceBinder(scope);

        final Object[] ids = jsInput.getIds();
        for (Object _id : ids) {
            String id = JSUtils.toString(_id);

            final Scriptable definition = (Scriptable) jsInput.get(id);

            String type = onlyOne(definition, "value", "alternative", "xml", "task");

            Input input;
            final QName inputType = QName.parse(JSUtils.toString(definition.get(type, jsObject)), null, prefixes);

            switch(type) {
                case "value":
                    final ValueType valueType = inputType == null ? null : new ValueType(inputType);
                    input = new XMLInput(valueType);
                    break;

                case "xml":
                case "alternative":
                    throw new NotImplementedException();

                case "task":
                    throw new NotImplementedException();
                default:
                    throw SHOULD_NOT_BE_HERE;
            }

            inputs.put(id, input);
        }
    }

    /**
     * Set inputs from XML
     *
     * @param repository
     * @param dom
     */
    private void setInputs(Repository repository, Node dom) {
        NodeList list = dom.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node item = list.item(i);
            if (item.getNodeType() != Node.ELEMENT_NODE)
                continue;
            Element el = (Element) item;
            addInput(el, repository);

        }

        // --- XPMProcess connections
        for (int i = 0; i < list.getLength(); i++) {
            Node item = list.item(i);
            if (item.getNodeType() != Node.ELEMENT_NODE)
                continue;
            Element el = (Element) item;
            final String inputToId = el.getAttribute("id");
            final Input inputTo = inputs.get(inputToId);
            if (inputTo == null)
                throw new AssertionError();

            // Add this to the list of inputs
            for (Element connect : XMLUtils.childIterator(el, new QName(
                    Manager.EXPERIMAESTRO_NS, "connect"))) {
                final String fromAtt = connect.getAttribute("from");
                final DotName to = new DotName(inputToId, DotName.parse(connect
                        .getAttribute("to")));

                final Connection connection;
                if (fromAtt.equals("")) {
                    // Complex case
                    final NodeList xqueryList = connect.getElementsByTagNameNS(Manager.EXPERIMAESTRO_NS, "xquery");
                    if (xqueryList.getLength() == 0)
                        throw new ExperimaestroRuntimeException("Cannot find a xp:xquery element within connect");
                    if (xqueryList.getLength() > 1)
                        throw new ExperimaestroRuntimeException("Too many xp:xquery element within connect");

                    String query = xqueryList.item(0).getTextContent();
                    XQueryConnection _connection = new XQueryConnection(to, query);

                    for (Element from : XMLUtils.elements(connect.getElementsByTagNameNS(Manager.EXPERIMAESTRO_NS, "from"))) {
                        final String ref = ensureAttribute(from, "ref", "no @ref attribute");
                        final String var = ensureAttribute(from, "var", "no @var attribute");
                        _connection.bind(var, DotName.parse(ref));
                    }
                    connection = _connection;
                } else {
                    final DotName from = DotName
                            .parse(fromAtt);
                    final String path = ensureAttribute(connect, "path", "Attribute path has to be defined (and not at the same time)");
                    LOGGER.info("Found connection between [%s in %s] and [%s]",
                            path, from, to);


                    if (inputs.get(from.get(0)) == null)
                        throw new ExperimaestroRuntimeException(
                                "Could not find input [%s] in [%s]", from.get(0),
                                this.id);

                    // TODO: maybe check there is no overloading?
                    connection = new SimpleConnection(from, path, to);
                }
                connection.addNamespaces(connect);
                inputTo.addConnection(connection);

            }
        }
    }

    private String ensureAttribute(Element connect, String name, String message) {
        final String path = connect.getAttribute(name);
        if ("".equals(path))
            throw new ExperimaestroRuntimeException(message);
        return path;
    }

    /**
     * Add an input for this task
     *
     * @param el         The XML element to process
     * @param repository The repository where the task is stored
     */
    private void addInput(Element el, Repository repository) {
        // Get the id
        String id = el.getAttribute("id");
        if (id == null)
            throw new RuntimeException(
                    format("Input without id in %s", this.id));


        // By default, the namespace is unspecified
        String namespace = null;
        final int colon = id.indexOf(":");
        if (colon >= 0) {
            namespace = id.substring(0, colon);
            id = id.substring(colon + 1);
        }


        LOGGER.debug("New input [%s] for task [%s]", id, this.id);

        // --- Get the type

        final Input input;
        QName inputType = getTypeQName(el, el.getAttribute("type"));
        QName inputRef = getTypeQName(el, el.getAttribute("ref"));

        switch (el.getTagName()) {
            case "xml":
            case "alternative": { // An XML document
                Type type = inputType != null ? repository.getType(inputType) : null;
                if (type == null && inputType != null) type = new Type(inputType);

                if (type != null && type instanceof AlternativeType) {
                    LOGGER.debug(
                            "Detected an alternative type configuration for input [%s] of type [%s]",
                            id, inputType);
                    input = new AlternativeInput((AlternativeType) type);
                } else {
                    // Simple type
                    if (el.getTagName().equals("alternative"))
                        throw new ExperimaestroRuntimeException("Type %s is not an alternative", inputType);
                    input = new XMLInput(type);
                }

                break;
            }

            case "array": {
                final Iterator<Element> it = XMLUtils.childElements(el).iterator();
                if (!it.hasNext())
                    throw new ExperimaestroRuntimeException("array element should have only one child");
                final Element child = it.next();
                if (it.hasNext())
                    throw new ExperimaestroRuntimeException("array element should have only one child");

                input = new ArrayInput();
                el.getFirstChild();
                break;
            }

            case "task": { // A task
                if (inputRef == null)
                    throw new ExperimaestroRuntimeException("There was no @ref attribute for the task input with id [%s]",
                            el.getAttribute("id"));
                TaskFactory factory = repository.getFactory(inputRef);
                if (factory == null)
                    throw new ExperimaestroRuntimeException(
                            "Could not find task factory [%s] for input [%s]", inputRef, id);

                // The type of this input is either specified (inputType)
                // or it is set to the declared output of the task
                Type type = inputType == null ? factory.getOutput() : new Type(inputType);
                input = new TaskInput(factory, type);
                break;
            }

            case "value": { // Simple XPM value
                final ValueType valueType = inputType == null ? null : new ValueType(inputType);
                input = new XMLInput(valueType);
                break;
            }

            default:
                throw new IllegalArgumentException("Cannot handle input of type " + el.getTagName());
        }


        input.setNamespace(namespace);

        // Should we merge all the parameters?
        if ("true".equals(el.getAttribute("merge")))
            input.setUnnamed(true);

        inputs.put(id, input);

        // Set the optional flag
        String optional = el.getAttribute("optional");
        boolean isOptional = optional != null && optional.equals("true");
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
            final Document defaultValue = Task.wrapValue(namespace, id, el.getAttribute("default"));
            input.setDefaultValue(defaultValue);
            LOGGER.debug("Default value[" + el.getAttribute("default") + "]: " + XMLUtils.toString(defaultValue));

        } else {
            Node child = XMLUtils.getChild(el, new QName(
                    Manager.EXPERIMAESTRO_NS, "default"));
            if (child != null) {
                Document document = XMLUtils.newDocument();
                child = child.cloneNode(true);
                document.adoptNode(child);
                final Iterator<Element> elements = XMLUtils.elements(child.getChildNodes()).iterator();
                if (!elements.hasNext()) {
                    document = Task.wrapValue(namespace, id, child.getTextContent());
                } else {
                    document.appendChild(elements.next());
                    if (elements.hasNext())
                        throw new ExperimaestroRuntimeException("Default value should be either atomic or one element");
                }
                input.setDefaultValue(document);
                LOGGER.debug("Default value: " + XMLUtils.toString(document));
            }
        }

    }

    private QName getTypeQName(Element el, String typeAtt) {
        return !typeAtt.equals("") ? QName.parse(typeAtt, el,
                Manager.PREDEFINED_PREFIXES) : null;
    }

    private static QName getQName(Scriptable scope, NativeObject jsObject, String key, boolean allowNull) {
        return (QName) JSUtils.get(scope, key, jsObject, allowNull);
    }

    @Override
    public String getDocumentation() {
        final Object object = JSUtils.get(jsScope, "description", jsObject,
                null);
        if (object != null)
            return object.toString();
        return "";
    }


    public Object run(NativeObject object) {
        return new JSPlan(this, object).run(object.getParentScope());
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
        Object result = f.call(jsContext, jsScope, jsScope, new Object[]{});
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
    public Type getOutput() {
        return output;
    }

}
