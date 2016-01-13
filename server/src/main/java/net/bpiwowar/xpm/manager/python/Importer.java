package sf.net.experimaestro.manager.python;

import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PySystemState;

public class Importer extends PyObject {
    public static final String JAVA_IMPORT_PATH_ENTRY = "__classpath__";

    public Importer() {
    }

    public PyObject __call__(PyObject[] args, String[] keywords) {
        if(args[0].toString().endsWith("__classpath__")) {
            return this;
        } else {
            throw Py.ImportError("unable to handle");
        }
    }

    public PyObject find_module(String name) {
        return this.find_module(name, Py.None);
    }

    public PyObject find_module(String name, PyObject path) {
        Py.writeDebug("import", "trying " + name + " in packagemanager for path " + path);
        PyObject ret = PySystemState.packageManager.lookupName(name.intern());
        if(ret != null) {
            Py.writeComment("import", "\'" + name + "\' as java package");
            return this;
        } else {
            return Py.None;
        }
    }

    public PyObject load_module(String name) {
        return PySystemState.packageManager.lookupName(name.intern());
    }

    public String toString() {
        return this.getType().toString();
    }
}
