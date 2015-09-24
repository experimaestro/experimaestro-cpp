package sf.net.experimaestro.manager.python;

import org.python.core.PyBoolean;
import org.python.core.PyClass;
import org.python.core.PyDictionary;
import sf.net.experimaestro.exceptions.ValueMismatchException;
import sf.net.experimaestro.manager.*;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.manager.scripting.Exposed;
import sf.net.experimaestro.utils.JSUtils;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Stream;

/**
 *
 */
@Exposed
class PythonTaskFactory extends TaskFactory {
    final PyClass pyClass;
    private Map<String, Input> inputs = new HashMap<>();
    private Type output;

    static public Set<String> getFields(PyDictionary object, String... keys) {
        Set<String> selected = new HashSet<>();
        for (String key : keys) {
            if (object.containsKey(key))
                selected.add(key);

        }
        return selected;
    }

    public PythonTaskFactory(Repository repository, QName id, String version, String group, PyClass pyClass) throws ValueMismatchException {
        super(repository, id, version, group);
        this.pyClass = pyClass;

        final PythonNamespaceContext pythonNamespaceContext = new PythonNamespaceContext();

        // Analyze class
        final PyDictionary pyInputs = (PyDictionary) pyClass.__getattr__("inputs").__getitem__(0);
        for (Object _keyvalue : pyInputs.entrySet()) {
            Entry keyvalue = (Entry) _keyvalue;
            String inputId = (String) keyvalue.getKey();
            PyDictionary definition = (PyDictionary) keyvalue.getValue();

            Set<String> fields = getFields(definition, "value", "alternative", "json", "task", "array");
            String type;
            if (fields.size() == 1) {
                type = fields.iterator().next();
            } else {
                throw new ValueMismatchException("Cannot create task factory: expected value, json, array or " +
                        "task values in input definition [got " + definition + "]");
            }

            boolean sequence = toBoolean(definition, "sequence");
            boolean optional = toBoolean(definition, "optional");


            Input input;
            final QName inputType = QName.parse(toString(definition.get(type)), pythonNamespaceContext);

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
                            new Type(QName.parse(toString(definition.get("type")), pythonNamespaceContext))
                            : factory.getOutput();

                    input = new TaskInput(factory, xmlType);
                    break;

                default:
                    throw new AssertionError();
            }

            // Case of sequence
            if (sequence)
                input = new ArrayInput(input.getType());

            // Case of default
            if (definition.containsKey("default")) {
                Json document = PythonUtils.toJSON(definition.get("default"));
                input.setDefaultValue(document);
            }

            if (definition.containsKey("copy")) {
                final Object copyTo = definition.get("copy");
                if (copyTo instanceof Boolean) {
                    if ((Boolean) copyTo)
                        input.setCopyTo(inputId);
                } else {
                    input.setCopyTo(JSUtils.toString(copyTo));
                }
            } else {
                // Copy parameters by default
                input.setCopyTo(inputId);
            }

            // Set groups
            final Object _groups = definition.get("groups");
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

            // Merge
            boolean merge = toBoolean(definition, "merge");
            if (merge) {
                input.setUnnamed(true);
            }

            // Store in the inputs
            inputs.put(inputId, input);

        }

    }

    private String toString(Object o) {
        return (String) o;
    }

    private boolean toBoolean(PyDictionary definition, String key) {
        if (!definition.containsKey(key)) {
            return false;
        }
        return ((PyBoolean) definition.get(key)).getValue() != 0;
    }

    @Override
    public Map<String, Input> getInputs() {
        return inputs;
    }

    @Override
    public Type getOutput() {
        return output;
    }

    @Override
    public Task create() {
        final PythonTask pythonTask = new PythonTask(this);
        pythonTask.init();
        return pythonTask;
    }
}
