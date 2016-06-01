package net.bpiwowar.xpm.manager.js;

/*
 * This file is part of experimaestro.
 * Copyright (c) 2014 B. Piwowarski <benjamin@bpiwowar.net>
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

import net.bpiwowar.xpm.exceptions.ValueMismatchException;
import net.bpiwowar.xpm.exceptions.XPMRhinoException;
import net.bpiwowar.xpm.exceptions.XPMRuntimeException;
import net.bpiwowar.xpm.manager.*;
import net.bpiwowar.xpm.manager.json.Json;
import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.utils.JSUtils;
import net.bpiwowar.xpm.utils.log.Logger;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import static java.lang.String.format;
import static net.bpiwowar.xpm.exceptions.XPMRuntimeException.SHOULD_NOT_BE_HERE;

/**
 * A task factory defined by a javascript object
 */
@Exposed
public class JavaScriptTaskFactory extends TaskFactory {
    final static private Logger LOGGER = Logger.getLogger();

    /**
     * The underlying object
     */
    protected NativeObject jsObject;

    /**
     * The inputs
     */
    protected Map<String, Input> inputs;

    /**
     * The scope
     */
    Scriptable jsScope;

    /**
     * The outputs
     */
    private Type output;


    /**
     * Creates a new task information from a javascript object
     *
     * @param scope    The scope
     * @param jsObject The object
     */
    public JavaScriptTaskFactory(TypeName qname, Scriptable scope, NativeObject jsObject,
                                 Repository repository) throws ValueMismatchException {
        super(repository, qname, JSUtils.get(scope,
                "version", jsObject, "1.0"), null);
        this.jsScope = scope;
        this.jsObject = jsObject;

        final JSNamespaceContext jsNamespaceContext = new JSNamespaceContext(scope);
        java.util.function.Function<String, String> prefixes = x -> jsNamespaceContext.getNamespaceURI(x);

        // --- Look up the module
//        Module module = Module.getModule(repository,
//                JSUtils.get(jsScope, "module", jsObject, null));
//        if (module != null)
//            setModule(module);

        // --- Get the task inputs
        inputs = new TreeMap<>();
        setInputs(scope, jsObject, "inputs");
        setInputs(scope, jsObject, "parameters");


        // --- Get the task outputs
        TypeName outTypeName = getQName(scope, jsObject, "output", true);
        if (outTypeName != null) {
            output = new Type(outTypeName);
        }


        init();

    }

    static TypeName getQName(Scriptable scope, NativeObject jsObject, String key, boolean allowNull) {
        Object o = JSUtils.get(scope, key, jsObject, allowNull);
        if (o == null)
            return null;

        if (o instanceof TypeName)
            return (TypeName) o;
        else if (o instanceof String) {
            return TypeName.parse(o.toString(), new JSNamespaceContext(scope));
        }

        throw new XPMRhinoException("Cannot transform type %s into QName", o.getClass());
    }

    static public Set<String> getFields(Scriptable object, String... keys) {
        Set<String> selected = new HashSet<>();
        for (String key : keys) {
            if (object.has(key, object))
                selected.add(key);

        }
        return selected;
    }

    private void setInputs(Scriptable scope, NativeObject jsObject, String name) throws ValueMismatchException {
        Object input = JSUtils.get(scope, name, jsObject, true);

        if (input == null)
            return;

        if (input instanceof NativeObject) {
            setJSInputs(scope, (NativeObject) input);
        } else
            throw new XPMRuntimeException("Cannot handle inputs of type %s", input.getClass());
    }

    /**
     * Set inputs from JSON data
     *
     * @param scope   The current JS scope
     * @param jsInput The JS input object
     */
    private void setJSInputs(final Scriptable scope, final NativeObject jsInput) throws ValueMismatchException {
        final JSNamespaceContext jsNamespaceContext = new JSNamespaceContext(scope);
        java.util.function.Function<String, String> prefixes = x -> jsNamespaceContext.getNamespaceURI(x);

        final Object[] ids = jsInput.getIds();
        for (Object _id : ids) {
            String id = JSUtils.toString(_id);

            Object o = jsInput.get(id);
            if (!(o instanceof Scriptable))
                throw new IllegalArgumentException(format("%s element is not an object", _id));

            final Scriptable definition = (Scriptable) o;

            Set<String> fields = getFields(definition, "value", "alternative", "json", "task", "array");
            String type;
            if (fields.size() == 1) {
                type = fields.iterator().next();
            } else if (fields.size() == 2 && fields.contains("xml") && fields.contains("task")) {
                type = "task";
            } else {
                throw new ValueMismatchException("Cannot create task factory: expected value, alternative, xml, array or " +
                        "task values in input definition [got " + definition.getIds() + "]");
            }

            boolean sequence = JSUtils.toBoolean(scope, definition, "sequence");
            boolean optional = JSUtils.toBoolean(scope, definition, "optional");


            Input input;
            final TypeName inputType = TypeName.parse(JSUtils.toString(definition.get(type, jsObject)), null, prefixes);

            switch (type) {
                case "array":
                    final ArrayType arrayType = new ArrayType(new ValueType(inputType));
                    input = new JsonInput(arrayType);
                    break;

                case "value":
                    final ValueType valueType = inputType == null ? null : new ValueType(inputType);
                    input = new JsonInput(valueType);
                    break;

                case "json":
                    input = new JsonInput(new Type(inputType));
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

            if (definition.has("copy", definition)) {
                final Object copyTo = definition.get("copy", definition);
                if (copyTo instanceof Boolean) {
                    if ((Boolean) copyTo)
                        input.setCopyTo(id);
                } else {
                    input.setCopyTo(JSUtils.toString(copyTo));
                }
            } else {
                // Copy parameters by default
                input.setCopyTo(id);
            }

            // Set groups
            final Object _groups = definition.get("groups", definition);
            if (JSUtils.isDefined(_groups)) {
                String[] groups;
                if (_groups instanceof List) {
                    final Stream<String> __groups = ((List) _groups).stream().map(JSUtils::toString);
                    groups = __groups.toArray(n -> new String[n]);
                } else if (_groups instanceof String) {
                    groups = new String[]{_groups.toString()};
                } else {
                    throw new ValueMismatchException("groups should be an array of strings, or a single value");
                }
                input.setGroups(groups);
            }


            // Set required/optional flag
            input.setOptional(optional);

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


    @Override
    public Task create() {
        // Get the "createSSHAgentIdentityRepository" constructor
        Object function = JSUtils.get(jsScope, "create", jsObject, null);

        // If we don't have one, then it might be a "direct" task, i.e.
        // not implying any object creation
        if (!(function instanceof Function)) {
            // Case of a configuration object
            function = JSUtils.get(jsScope, "run", jsObject, null);
            if (function != null && !(function instanceof Function))
                throw new RuntimeException(
                        "Could not find the create or run converter.");

            JSDirectTask jsDirectTask = new JSDirectTask(this, jsScope, jsObject, (Function) function, output);
            jsDirectTask.init();
            return jsDirectTask;
        }

        // Call it
        Context jsContext = Context.getCurrentContext();
        Function f = (Function) function;
        Object result = f.call(jsContext, jsScope, jsScope, new Object[]{});
        LOGGER.info("Created a new experiment: %s (%s)", result,
                result.getClass());
        TaskJavascript jsTask = new TaskJavascript(this, jsContext, jsScope,
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