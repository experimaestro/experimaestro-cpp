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
import sf.net.experimaestro.exceptions.XPMRhinoException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.exceptions.ValueMismatchException;
import sf.net.experimaestro.manager.AlternativeInput;
import sf.net.experimaestro.manager.AlternativeType;
import sf.net.experimaestro.manager.ArrayInput;
import sf.net.experimaestro.manager.Connection;
import sf.net.experimaestro.manager.DotName;
import sf.net.experimaestro.manager.Input;
import sf.net.experimaestro.manager.Manager;
import sf.net.experimaestro.manager.Module;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.manager.Repository;
import sf.net.experimaestro.manager.SimpleConnection;
import sf.net.experimaestro.manager.Task;
import sf.net.experimaestro.manager.TaskFactory;
import sf.net.experimaestro.manager.TaskInput;
import sf.net.experimaestro.manager.Type;
import sf.net.experimaestro.manager.ValueType;
import sf.net.experimaestro.manager.XMLInput;
import sf.net.experimaestro.manager.XQueryConnection;
import sf.net.experimaestro.utils.JSNamespaceContext;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.String2String;
import sf.net.experimaestro.utils.XMLUtils;
import sf.net.experimaestro.utils.log.Logger;

import javax.xml.xpath.XPathExpressionException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static sf.net.experimaestro.exceptions.ExperimaestroRuntimeException.SHOULD_NOT_BE_HERE;

/**
 * A task factory as defined by a JavaScript object
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class JSTaskFactory extends JSBaseObject {

    final static private Logger LOGGER = Logger.getLogger();
    FactoryImpl factory;

    public JSTaskFactory(Scriptable scope, NativeObject jsObject,
                         Repository repository) throws ValueMismatchException {
        this(getQName(scope, jsObject, "id", false), scope, jsObject, repository);
    }

    public JSTaskFactory(QName qname, Scriptable scope, NativeObject jsObject,
                         Repository repository) throws ValueMismatchException {
        factory = new FactoryImpl(qname, scope, jsObject, repository);
    }

    public JSTaskFactory(FactoryImpl factory) {
        this.factory = factory;
    }


    private static QName getQName(Scriptable scope, NativeObject jsObject, String key, boolean allowNull) {
        Object o = JSUtils.get(scope, key, jsObject, allowNull);
        if (o == null)
            return null;

        if (o instanceof QName)
            return (QName) o;
        else if (o instanceof String) {
            return QName.parse(o.toString(), new JSNamespaceContext(scope));
        }

        throw new XPMRhinoException("Cannot transform type %s into QName", o.getClass());
    }

    @JSFunction("create")
    public JSTaskWrapper create() {
        return new JSTaskWrapper(factory.create());
    }

    @JSFunction(value = "run", scope = true)
    public Object run(Context context, Scriptable scope, NativeObject object) throws XPathExpressionException {
        return plan(context, scope, object).run(context, scope);
    }


    @JSHelp("Creates a plan from this task")
    @JSFunction(value = "plan", scope = true)
    public JSPlan plan(Context cx, Scriptable scope, NativeObject object) throws XPathExpressionException {
        return new JSPlan(scope, factory, object);
    }

    @JSHelp("Creates a plan from this task")
    @JSFunction(value = "plan")
    public JSPlan plan() {
        return new JSPlan(factory);
    }

    @JSFunction("run_plan")
    public List<Object> runPlan(String plan) throws Exception {
        Task task = factory.create();
        return JSTaskWrapper.wrap(task.runPlan(plan, false, new JSScriptRunner(this), false));
    }


    static public class FactoryImpl extends TaskFactory {
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

        /**
         * Our XPM object
         */
        private final XPMObject xpm;


        /**
         * Creates a new task information from a javascript object
         *
         * @param scope    The scope
         * @param jsObject The object
         */
        public FactoryImpl(QName qname, Scriptable scope, NativeObject jsObject,
                           Repository repository) throws ValueMismatchException {
            super(repository, qname, JSUtils.get(scope,
                    "version", jsObject, "1.0"), null);
            this.jsScope = scope;
            this.jsObject = jsObject;
            this.xpm = XPMObject.getXPMObject(scope);

            // --- Look up the module
            Module module = JSModule.getModule(repository,
                    JSUtils.get(jsScope, "module", (NativeObject) jsObject, null));
            if (module != null)
                setModule(module);

            // --- Get the task inputs
            Object input = JSUtils.get(scope, "inputs", jsObject);
            inputs = new TreeMap<>();

            if (JSUtils.isXML(input)) {
                Document dom = JSUtils.toDocument(null, input, new QName(Manager.EXPERIMAESTRO_NS, "inputs"));
                setInputs(repository, dom.getDocumentElement());
            } else if (input instanceof NativeObject) {
                setInputs(scope, (NativeObject) input);
            } else
                throw new ExperimaestroRuntimeException("Cannot handle inputs of type %s", inputs.getClass());


            // --- Get the task outputs
            QName outQName = getQName(scope, jsObject, "output", true);
            if (outQName != null)
                output = new Type(outQName);


            // --- Are we an alternative?

            Object altObject = JSUtils.get(jsScope, "alternative", jsObject, null);
            if (altObject != null) {
                QName altId;

                if (altObject instanceof Boolean) {
                    if (output == null)
                        throw new ExperimaestroRuntimeException("No output has been defined for an alternative");
                    altId = output.getId();
                } else if (altObject instanceof QName) {
                    altId = (QName) altObject;
                } else
                    throw new NotImplementedException("Cannot handle alternative of type " + altObject.getClass());


                Type type = repository.getType(altId);
                if (type == null || !(type instanceof AlternativeType))
                    throw new ExperimaestroRuntimeException(
                            "Type %s is not an alternative", outQName == null ? "null"
                            : outQName.toString());

                ((AlternativeType) type).add(id, this);
                return;
            }

            init();

        }

        static public Set<String> getFields(Scriptable object, String... keys) {
            Set<String> selected = new HashSet<>();
            for (String key : keys) {
                if (object.has(key, object))
                    selected.add(key);

            }
            return selected;
        }

        /**
         * Set inputs from JSON data
         *
         * @param scope   The current JS scope
         * @param jsInput The JS input object
         */
        private void setInputs(final Scriptable scope, final NativeObject jsInput) throws ValueMismatchException {
            String2String prefixes = new JSNamespaceBinder(scope);

            final Object[] ids = jsInput.getIds();
            for (Object _id : ids) {
                String id = JSUtils.toString(_id);

                final Scriptable definition = (Scriptable) jsInput.get(id);

                Set<String> fields = getFields(definition, "value", "alternative", "xml", "task");
                String type;
                if (fields.size() == 1) {
                    type = fields.iterator().next();
                } else if (fields.size() == 2 && fields.contains("xml") && fields.contains("task")) {
                    type = "task";
                } else
                    throw new ValueMismatchException("Cannot create task factory: expected value, alternative, xml, or" +
                            "task values in input definition");


                boolean sequence = JSUtils.toBoolean(scope, definition, "sequence");
                boolean optional = JSUtils.toBoolean(scope, definition, "optional");

                Input input;
                final QName inputType = QName.parse(JSUtils.toString(definition.get(type, jsObject)), null, prefixes);

                switch (type) {
                    case "value":
                        final ValueType valueType = inputType == null ? null : new ValueType(inputType);
                        input = new XMLInput(valueType);
                        break;

                    case "xml":
                        input = new XMLInput(new Type(inputType));
                        break;

                    case "alternative":
                        Type altType = getRepository().getType(inputType);
                        if (altType == null || !(altType instanceof AlternativeType))
                            throw new IllegalArgumentException("Type " + inputType + " is not an alternative");
                        input = new AlternativeInput((AlternativeType) altType);
                        break;

                    case "task":
                        TaskFactory factory = getRepository().getFactory(inputType);
                        if (factory == null)
                            throw new ValueMismatchException("Could not find task factory [%s] for input [%s]",
                                    inputType, id);

                        // The type of this input is either specified (inputType)
                        // or it is set to the declared output of the task
                        Type xmlType = fields.contains("type") ?
                                new Type(QName.parse(JSUtils.toString(definition.get("type", jsObject)), null, prefixes))
                                : factory.getOutput();

                        input = new TaskInput(factory, xmlType);
                        break;

                    default:
                        throw SHOULD_NOT_BE_HERE;
                }

                // Case of sequence
                if (sequence)
                    input = new ArrayInput(input.getType());

                // Case of default
                if (definition.has("default", definition)) {
                    Document document = JSUtils.toDocument(jsInput, definition.get("default", definition));
                    input.setDefaultValue(document);
                }

                // Set required/optional flag
                input.setOptional(optional);

                // Process connections
                if (definition.has("connect", definition)) {
                    NativeObject connect = (NativeObject) definition.get("connect", definition);
                    for (Map.Entry<Object, Object> connection : connect.entrySet()) {
                        DotName to = DotName.parse(connection.getKey().toString());
                        String query = JSUtils.toString(connection.getValue());
                        input.addConnection(new XQueryConnection(to, query));
                    }
                }

                // Store in the inputs
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
                        final String path = ensureAttribute(connect, "path", "Attribute path has to be defined");
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
                        String.format("Input without id in %s", this.id));


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
                final Document defaultValue = ValueType.wrapString(namespace, id, el.getAttribute("default"),
                        input.getType() != null ? input.getType().qname() : null);
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
                        document = ValueType.wrapString(namespace, id, child.getTextContent(), input.getType().qname());
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


        @Override
        public String getDocumentation() {
            final Object object = JSUtils.get(jsScope, "description", jsObject,
                    null);
            if (object != null)
                return object.toString();
            return "";
        }


        public Object run(Scriptable scope, NativeObject object) throws XPathExpressionException {
            return new JSPlan(scope, this, object).run(Context.getCurrentContext(), object.getParentScope());
        }


        @Override
        public JSAbstractTask create() {
            // Get the "create" method
            Object function = JSUtils.get(jsScope, "create", jsObject, null);

            // If we don't have one, then it might be a "direct" task, i.e.
            // not implying any object creation
            if (!(function instanceof Function)) {
                // Case of a configuration object
                function = JSUtils.get(jsScope, "run", jsObject, null);
                if (function != null && !(function instanceof Function))
                    throw new RuntimeException(
                            "Could not find the create or run converter.");

                JSDirectTask jdDirectTask = new JSDirectTask(xpm, this, jsScope,
                        jsObject, (Function) function, output);
                jdDirectTask.init();
                return jdDirectTask;
            }

            // Call it
            Context jsContext = Context.getCurrentContext();
            Function f = (Function) function;
            Object result = f.call(jsContext, jsScope, jsScope, new Object[]{});
            LOGGER.info("Created a new experiment: %s (%s)", result,
                    result.getClass());
            JSTask jsTask = new JSTask(this, jsContext, jsScope,
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

}
