package sf.net.experimaestro.manager.python;

import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.util.PythonInterpreter;
import sf.net.experimaestro.utils.Output;

/**
 * The XPM object for python scripts
 *
 * @author B. Piwowarski
 */
public class XPM extends PyObject {


    static class Hello extends PyObject {

        @Override
        public PyObject __call__() {
            return new PyString("Hello");
        }

        @Override
        public PyObject __call__(PyObject[] args, String[] keywords) {
            return new PyString(String.format("Hello - %d args, keywords [%s]", args.length, Output.toString(",", keywords)));
        }
    }

    @Override
    public PyObject __findattr_ex__(String name) {
        switch (name) {
            case "hello":
                return new Hello();
            case "Task":
                return new Hello();
            default:
                return super.__findattr_ex__(name);
        }
    }

    @Override
    public PyObject __finditem__(String key) {
        return super.__finditem__(key);
    }



    /**
     * Prepare the environment for the python engine
     *
     * @param engine
     */
    public static void prepare(PythonInterpreter engine) {
        XPM xpm = new XPM();
        engine.set("xpm", xpm);
    }
}
