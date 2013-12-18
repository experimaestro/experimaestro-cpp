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
import org.mozilla.javascript.NativeFunction;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.XPMHelper;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.exceptions.ValueMismatchException;
import sf.net.experimaestro.exceptions.XPMRhinoException;
import sf.net.experimaestro.manager.AlternativeInput;
import sf.net.experimaestro.manager.AlternativeType;
import sf.net.experimaestro.manager.ArrayInput;
import sf.net.experimaestro.manager.DotName;
import sf.net.experimaestro.manager.Input;
import sf.net.experimaestro.manager.JsonInput;
import sf.net.experimaestro.manager.Module;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.manager.Repository;
import sf.net.experimaestro.manager.Task;
import sf.net.experimaestro.manager.TaskContext;
import sf.net.experimaestro.manager.TaskFactory;
import sf.net.experimaestro.manager.TaskInput;
import sf.net.experimaestro.manager.Type;
import sf.net.experimaestro.manager.ValueType;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.utils.JSNamespaceContext;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.String2String;
import sf.net.experimaestro.utils.log.Logger;

import javax.xml.xpath.XPathExpressionException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static java.lang.String.format;
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
        return JSTaskWrapper.wrap(task.runPlan(plan, false, new JSScriptRunner(this), new TaskContext()));
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
            inputs = new TreeMap<>();
            setInputs(scope, jsObject, "inputs");
            setInputs(scope, jsObject, "parameters");


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

        private void setInputs(Scriptable scope, NativeObject jsObject, String name) throws ValueMismatchException {
            Object input = JSUtils.get(scope, name, jsObject, true);

            if (input == null)
                return;

            if (input instanceof NativeObject) {
                setJSInputs(scope, (NativeObject) input);
            } else
                throw new ExperimaestroRuntimeException("Cannot handle inputs of type %s", input.getClass());
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
        private void setJSInputs(final Scriptable scope, final NativeObject jsInput) throws ValueMismatchException {
            String2String prefixes = new JSNamespaceBinder(scope);

            final Object[] ids = jsInput.getIds();
            for (Object _id : ids) {
                String id = JSUtils.toString(_id);

                Object o = jsInput.get(id);
                if (!(o instanceof Scriptable))
                    throw new IllegalArgumentException(format("%s element is not an object", _id));

                final Scriptable definition = (Scriptable) o;

                Set<String> fields = getFields(definition, "value", "alternative", "json", "xml", "task");
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
                        input = new JsonInput(valueType);
                        break;

                    case "xml":
                        xpm.getRootLogger().warn("xml is *strongly* deprecated: use json [%s]", id);

                    case "json":
                        input = new JsonInput(new Type(inputType));
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
                    Json document = JSUtils.toJSON(jsInput, definition.get("default", definition));
                    input.setDefaultValue(document);
                }

                // Set required/optional flag
                input.setOptional(optional);

                // Merge
                boolean merge = JSUtils.toBoolean(scope, definition, "merge");
                if (merge)
                    input.setUnnamed(true);

                // Process connections
                if (definition.has("connect", definition)) {
                    NativeObject connect = (NativeObject) definition.get("connect", definition);
                    for (Map.Entry<Object, Object> connection : connect.entrySet()) {
                        DotName to = DotName.parse(connection.getKey().toString());
                        Object value = connection.getValue();
                        if (value instanceof NativeFunction) {
                            NativeFunction f = (NativeFunction) value;
                            String[] names = XPMHelper.getParamNames(f);
                            input.addConnection(new JSConnection(new DotName(id, to), scope, f, names));
                        } else
                            throw new IllegalArgumentException("Cannot handle object of type " + value.getClass());
                    }
                }

                // Store in the inputs
                inputs.put(id, input);
            }
        }


        @Override
        public String getDocumentation() {
            final Object object = JSUtils.get(jsScope, "description", jsObject, null);
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

                JSDirectTask jsDirectTask = new JSDirectTask(xpm, this, jsScope,
                        jsObject, (Function) function, output);
                jsDirectTask.init();
                return jsDirectTask;
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
