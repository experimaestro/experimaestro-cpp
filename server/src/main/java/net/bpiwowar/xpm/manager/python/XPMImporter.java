package sf.net.experimaestro.manager.python;

import org.python.core.PyObject;

/**
 * The XPM specific importer
 */
public class XPMImporter extends PyObject {
    _Importer _importer = new _Importer();

    @Override
    public PyObject __findattr_ex__(String name) {
        if (name.equals("find_module")) {
            return _importer;
        }
        return super.__findattr_ex__(name);
    }

    private class _Importer extends PyObject {
        public PyObject __call__(PyObject[] args, String[] keywords) {
            return new PyObject() {
                @Override
                public PyObject __findattr_ex__(String name) {
                    if (name.equals("load_module")) {
                        return new PyObject() {
                            public PyObject __call__(PyObject[] args, String[] keywords) {
                                return new PyObject() {

                                };
                            }
                        };
                    }
                    return super.__findattr_ex__(name);
                }

            };
        }
    }
}
