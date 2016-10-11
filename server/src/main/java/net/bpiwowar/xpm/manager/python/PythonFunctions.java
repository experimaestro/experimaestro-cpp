package net.bpiwowar.xpm.manager.python;

import org.python.core.*;
import net.bpiwowar.xpm.exceptions.ValueMismatchException;
import net.bpiwowar.xpm.exceptions.XPMScriptRuntimeException;
import net.bpiwowar.xpm.manager.*;
import net.bpiwowar.xpm.manager.scripting.Expose;
import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.manager.scripting.ScriptContext;

import static net.bpiwowar.xpm.manager.python.PythonType.getPyClass;

/**
 * Python specific functions
 */
@Exposed
public class PythonFunctions {

    @Expose
    static public PyObject task(String id) {
        return new TaskAnnotation(id);
    }

    /**
     * Transforms a class into an experimaestro task
     */
    private static class TaskAnnotation extends PyObject {
        private final String id;

        public TaskAnnotation(String id) {
            this.id = id;
        }

        @Override
        public PyObject __call__(PyObject[] args, String[] keywords) {
            PyObject arg = args[0];
            if (args.length != 1) {
                throw new IllegalArgumentException("Task annotation expects one argument");
            }

            if (arg instanceof PyClass) {
                PyClass pyClass = (PyClass) arg;

                PyTuple bases__ = pyClass.__bases__;

                final PyObject[] elements = new PyObject[bases__.__len__() + 1];
                System.arraycopy(pyClass.__bases__.getArray(), 0, elements, 1, elements.length - 1);
                elements[0] = getPyClass(PythonTask.class);

                pyClass.__bases__ = new PyTuple(elements);
            } else if (arg instanceof PyType) {
                PyType pyType = (PyType) arg;
                PyObject[] bases = ((PyTuple)pyType.getBases()).getArray();
                final PyObject[] elements = new PyObject[bases.length + 1];
                for(int i = 0; i < elements.length-1; ++i) {
                    elements[i] = bases[i];
                }
                elements[elements.length-1] = getPyClass(PythonTask.class);
                pyType.setBases(new PyTuple(elements));
            } else {
                throw new IllegalArgumentException("Task annotation expects a class");
            }

            ScriptContext sc = ScriptContext.get();
            String version = null;
            String group = null;

            final TypeName qid = TypeName.parse(id, new PythonNamespaceContext());
            try {
                sc.getRepository().addFactory(new PythonTaskFactory(sc.getRepository(), qid, version, group, arg));
            } catch (ValueMismatchException e) {
                throw new XPMScriptRuntimeException(e);
            }

            return arg;
        }

    }

}
