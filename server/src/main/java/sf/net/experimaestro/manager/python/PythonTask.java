package sf.net.experimaestro.manager.python;

import org.mozilla.javascript.Undefined;
import org.python.core.*;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.manager.*;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.manager.json.JsonObject;
import sf.net.experimaestro.manager.json.JsonString;
import sf.net.experimaestro.manager.scripting.*;
import sf.net.experimaestro.scheduler.Resource;
import sf.net.experimaestro.utils.log.Logger;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Function;

/**
 * A python task
 */
@Exposed
public class PythonTask extends Task {
    final static private Logger LOGGER = Logger.getLogger();

    private final PyObject pyObject;


//    static PyClass PYTHONTASKCLASS;
//
//    static {
//        ClassDescription cd = ClassDescription.analyzeClass(TaskMethods.class);
//        final PyStringMap dict = new PyStringMap();
//        final Function<PyObject, Object> java_object__ = x -> ((PythonObject) x.__getattr__("__java_object__")).object;
//
//        for (Map.Entry<Object, ArrayList<Method>> objectArrayListEntry : cd.getMethods().entrySet()) {
//            final Object key = objectArrayListEntry.getKey();
//            if (key instanceof String) {
//                final String name = (String) key;
//                final MethodFunction unique_directory = new MethodFunction(name);
//                unique_directory.add(objectArrayListEntry.getValue());
//                final PyFrame frame = Py.getThreadState().frame;
//                final PyFunction pyFunction = new PyFunction(frame.f_globals, null, new InstanceMethod(name, unique_directory, java_object__), null, null);
//                dict.__setitem__(name, pyFunction);
//            }
//        }
//
//
//        PYTHONTASKCLASS = (PyClass) PyClass.classobj___new__(new PyString("Task"), new PyTuple(), dict);
//    }


    public PythonTask(PythonTaskFactory factory) {
        super(factory);

        pyObject = factory.pyClass.__call__();
        pyObject.__setattr__("__java_object__", PythonRunner.wrap(this));
    }

    @Override
    public Json doRun(ScriptContext taskContext) {

        LOGGER.debug("[Running] task: %s", factory.getId());

        // Get the inputs
        JsonObject resultObject = new JsonObject();
        Type outputType = getFactory().getOutput();

        final PyObject runFunction = pyObject.__findattr__("run");

        // Handles the type
        if (outputType != null) {
            // If the output is a generic object, modify the value
            resultObject.put(Constants.XP_TYPE.toString(), new JsonString(outputType.toString()));
        }

        // Copy the requested outputs
        for (Map.Entry<String, Input> namedInput : getInputs().entrySet()) {
            final String copyTo = namedInput.getValue().getCopyTo();
            if (copyTo != null || runFunction == null) {
                String key = namedInput.getKey();
                Value value = values.get(key);
                resultObject.put(copyTo == null ? key : copyTo, value.get());
            }
        }


        if (runFunction != null) {
            // We have a run function
            JsonObject jsoninput = new JsonObject();
            for (Map.Entry<String, Value> entry : values.entrySet()) {
                Json input = entry.getValue().get();
                jsoninput.put(entry.getKey(), input);
            }

            // Switch to our context
            try (ScriptContext.Swap ignored = taskContext.swap()) {
                final Object returned = runFunction.__call__(new PyObject[]{
                        PythonRunner.wrap(jsoninput), PythonRunner.wrap(resultObject)
                });

                LOGGER.debug("Returned %s", returned);
                if (returned == Undefined.instance || returned == null) {
                    throw new XPMRuntimeException(
                            "Undefined returned by the function run of task [%s]",
                            factory.getId());
                }
                LOGGER.debug("[/Running] task: %s", factory.getId());

                return PythonUtils.toJSON(returned);
            }
        }


        // Simplify the output if needed
        if (outputType == null && values.size() == 1) {
            LOGGER.debug("[/Running] task: %s", factory.getId());
            return values.values().iterator().next().get();
        }

        LOGGER.debug("[/Running] task: %s", factory.getId());
        return resultObject;

    }


    @Expose(context = true, optionalsAtStart = true, optional = 2)
    public Path unique_directory(LanguageContext cx, Path basedir, String prefix, Object json) throws IOException, NoSuchAlgorithmException {
        return uniquePath(cx, basedir, prefix, json, true);
    }

    @Expose(context = true)
    public Path unique_directory(LanguageContext cx, Resource resource, String prefix, Object json) throws IOException, NoSuchAlgorithmException {
        return uniquePath(cx, resource.file().getParent(), prefix, json, true);
    }

    @Expose(context = true, optionalsAtStart = true, optional = 2)
    public Path unique_file(LanguageContext cx, Path basedir, String prefix, Object json) throws IOException, NoSuchAlgorithmException {
        return uniquePath(cx, basedir, prefix, json, false);
    }

    @Expose(context = true)
    public Path unique_file(LanguageContext cx, Resource resource, String prefix, Object json) throws IOException, NoSuchAlgorithmException {
        return uniquePath(cx, resource.file().getParent(), prefix, json, false);
    }

    private Path uniquePath(LanguageContext cx, Path basedir, String prefix, Object json, boolean directory) throws IOException, NoSuchAlgorithmException {
        QName taskId = PythonTask.this.getFactory().getId();
        if (prefix == null) {
            prefix = taskId.getLocalPart();
        }
        return Manager.uniquePath(basedir, prefix, taskId, cx.toJSON(json), directory);
    }
}
