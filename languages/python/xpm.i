// Python slots
// See https://docs.python.org/3/c-api/typeobj.html

%include "collection.i"


// Pythonic renames and mappings

%feature("python:slot", "tp_str",functype = "reprfunc") *::toString;
%feature("python:slot", "tp_repr", functype = "reprfunc") *::toString;
%feature("python:slot", "tp_hash", functype = "hashfunc") *::hash;
%feature("python:slot", "tp_getattro", functype = "binaryfunc") *::__getattro__;

%feature("python:slot", "sq_length", functype = "lenfunc") *::size;

%rename(append) *::push_back;
%rename(equals) *::operator==;


// Attributes
%attribute(xpm::Argument, bool, required, required, required);
%ignore xpm::Argument::required;

// Attributes
%attribute(xpm::Argument, bool, ignored, ignored, ignored);
%ignore xpm::Argument::ignored;

/*%attributeval(xpm::Argument, std::shared_ptr<xpm::Object>, Object, defaultValue, defaultValue)
%ignore xpm::Argument::defaultValue;
*/

/*%feature("naturalvar", 0) std::shared_ptr<xpm::Generator>;
attributeval(xpm::Argument, xpm::Generator, generator, generator, generator)
%ignore xpm::Argument::generator;
*/

%attribute(xpm::Argument, std::string, help, help, help)
%ignore xpm::Argument::help;

/*%attribute(xpm::Argument, Type, type, type, type)*/
/*%ignore xpm::Argument::type;*/

%extend xpm::ArrayValue { 
    %COLLECTION(std::shared_ptr<xpm::Value>) 
};

// Get the real object for a shared_ptr of an object
%typemap(out) std::shared_ptr<xpm::Object> {
    // Retrieving Python object (and not the director)
    if ($1) {
        // This is a Director object
        if (Swig::Director * d = SWIG_DIRECTOR_CAST($1.get())) {
            Py_INCREF(d->swig_get_self());
            return d->swig_get_self();
        }

        std::shared_ptr< xpm::Object > * smartresult = new std::shared_ptr<xpm::Object>($1);
        $result = SWIG_InternalNewPointerObj(SWIG_as_voidptr(smartresult), $descriptor(std::shared_ptr<xpm::Object>), SWIG_POINTER_OWN);
    } else {
        // Returns None
        $result = SWIG_Py_Void();
    } 
}

// Get the real object for a shared_ptr of a value
%typemap(out) std::shared_ptr<xpm::Value> {
    if (!$1) {
        $result = SWIG_Py_Void();
    } else if (auto mp = std::dynamic_pointer_cast<xpm::MapValue>($1)) {
        auto smartresult = new std::shared_ptr<xpm::MapValue>(mp);
        $result = SWIG_InternalNewPointerObj(SWIG_as_voidptr(smartresult), $descriptor(std::shared_ptr<xpm::MapValue> &), SWIG_POINTER_OWN);
    } else if (auto ap = std::dynamic_pointer_cast<xpm::ArrayValue>($1)) {
        auto smartresult = new std::shared_ptr<xpm::ArrayValue>(ap);
        $result = SWIG_InternalNewPointerObj(SWIG_as_voidptr(smartresult), $descriptor(std::shared_ptr<xpm::MapParameters> &), SWIG_POINTER_OWN);
    } else if (auto sp = std::dynamic_pointer_cast<xpm::ScalarValue>($1)) {
        auto smartresult = new std::shared_ptr<xpm::ScalarValue>(sp);
        $result = SWIG_InternalNewPointerObj(SWIG_as_voidptr(smartresult), $descriptor(std::shared_ptr<xpm::ScalarValue> &), SWIG_POINTER_OWN);
    } else {
        throw xpm::assertion_error("Could not recognize value type...");
    }
      
}


// Handles properly a smart pointer
%typemap(directorin) std::shared_ptr< xpm::Value > const & {
    // Handles null smart pointer
    if (!$1) {
        $input = SWIG_Py_Void();
    } else {
        std::shared_ptr< xpm::Value > * ptr = new std::shared_ptr< xpm::Value >($1);
        $input = SWIG_NewPointerObj(%as_voidptr(ptr), $descriptor, SWIG_POINTER_OWN);
    }
}

%extend xpm::Typename {
    PyObject * __getattro__(PyObject *name) {
        if (!PyUnicode_Check(name)) {
            PyErr_SetString(PyExc_AttributeError, "Attribute name is not a string");
            return SWIG_Py_Void();
        }

        Py_ssize_t stringsize;
        char *_key = (char*)PyUnicode_AsUTF8AndSize(name, &stringsize);
        std::string key(_key, stringsize);

        auto ptr = new xpm::Typename((*$self)(key));
        return SWIG_InternalNewPointerObj(%as_voidptr(ptr), $descriptor(xpm::Typename*), SWIG_POINTER_OWN);
    }
}
