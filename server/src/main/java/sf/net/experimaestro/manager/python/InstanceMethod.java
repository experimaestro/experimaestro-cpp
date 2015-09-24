package sf.net.experimaestro.manager.python;

import org.python.core.*;
import sf.net.experimaestro.manager.scripting.MethodFunction;

import java.util.function.Function;

/**
 * Represents an instance method of a python class
 */
class InstanceMethod extends PyCode implements Traverseproc {
    private final Function<PyObject, Object> selfFactory;
    private MethodFunction function;

    public InstanceMethod(String name, MethodFunction function, Function<PyObject, Object> factory) {
        this.co_name = name;
        this.function = function;
        this.selfFactory = factory;
    }

    private PyObject call(PyObject self, PyObject[] args, String[] keywords) {
        return PythonMethod.call(selfFactory.apply(self), args, keywords, function);
    }

    public PyObject call(ThreadState state, PyFrame frame, PyObject closure) {
        throw new UnsupportedOperationException();
    }

    public PyObject call(ThreadState state, PyObject[] args, String[] keywords, PyObject globals, PyObject[] defaults, PyObject closure) {
        throw new UnsupportedOperationException();
    }

    public PyObject call(ThreadState state, PyObject self, PyObject[] args, String[] keywords, PyObject globals, PyObject[] defaults, PyObject closure) {
        throw new UnsupportedOperationException();
    }

    public PyObject call(ThreadState state, PyObject globals, PyObject[] defaults, PyObject closure) {
        throw new UnsupportedOperationException();
    }

    public PyObject call(ThreadState state, PyObject arg1, PyObject globals, PyObject[] defaults, PyObject closure) {
        throw new UnsupportedOperationException();
    }

    public PyObject call(ThreadState state, PyObject arg1, PyObject arg2, PyObject globals, PyObject[] defaults, PyObject closure) {
        return this.call(arg1, new PyObject[] { arg2 }, Py.NoKeywords);
    }

    public PyObject call(ThreadState state, PyObject arg1, PyObject arg2, PyObject arg3, PyObject globals, PyObject[] defaults, PyObject closure) {
        throw new UnsupportedOperationException();
    }

    public PyObject call(ThreadState state, PyObject arg1, PyObject arg2, PyObject arg3, PyObject arg4, PyObject globals, PyObject[] defaults, PyObject closure) {
        throw new UnsupportedOperationException();
    }

    public int traverse(Visitproc visit, Object arg) {
//            return this.function != null?visit.visit(this.function, arg):0;
        throw new UnsupportedOperationException();
    }

    public boolean refersDirectlyTo(PyObject ob) {
        throw new UnsupportedOperationException();
    }
}
