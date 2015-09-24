package sf.net.experimaestro.manager.python;

import org.python.core.*;
import sf.net.experimaestro.exceptions.ValueMismatchException;
import sf.net.experimaestro.exceptions.XPMScriptRuntimeException;
import sf.net.experimaestro.manager.*;
import sf.net.experimaestro.manager.scripting.Expose;
import sf.net.experimaestro.manager.scripting.Exposed;
import sf.net.experimaestro.manager.scripting.ScriptContext;

import static sf.net.experimaestro.manager.python.PythonType.getPyClass;

/**
 * Python specific functions
 */
@Exposed
public class PythonFunctions  {

    @Expose
    static public PyObject task(String id) {
        return new TaskAnnotation(id);
    }

    /**
     * Transforms a class into a task
     */
    private static class TaskAnnotation extends PyObject {
        private final String id;

        public TaskAnnotation(String id) {
            this.id = id;
        }

        @Override
        public PyObject __call__(PyObject[] args, String[] keywords) {
            if (args.length != 1 && !(args[0] instanceof PyClass)) {
                throw new IllegalArgumentException("Task annotation expects a class");
            }

            PyClass pyClass = (PyClass) args[0];

            final PyObject[] elements = new PyObject[pyClass.__bases__.__len__() + 1];
            System.arraycopy(pyClass.__bases__.getArray(), 0, elements, 1, elements.length - 1);
            elements[0] = getPyClass(PythonTask.class);

            pyClass.__bases__ = new PyTuple(elements);

            ScriptContext sc = ScriptContext.get();
            String version = null;
            String group = null;

            final QName qid = QName.parse(id, new PythonNamespaceContext());
            try {
                sc.getRepository().addFactory(new PythonTaskFactory(sc.getRepository(), qid, version, group, pyClass));
            } catch (ValueMismatchException e) {
                throw new XPMScriptRuntimeException(e);
            }

            return pyClass;
        }

    }

}
