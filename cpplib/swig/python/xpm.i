// Python slots
// See https://docs.python.org/3/c-api/typeobj.html
%feature("python:slot", "tp_str",functype = "reprfunc") *::toString;
%feature("python:slot", "tp_repr", functype = "reprfunc") *::toString;
%feature("python:slot", "tp_call", functype = "ternarycallfunc") *::call;
%feature("python:slot", "tp_hash", functype = "hashfunc") *::hash;
/*%feature("python:slot", "mp_subscript", functype = "binaryfunc") *::__getitem__;*/
/*%feature("python:slot", "mp_ass_subscript", functype = "objobjargproc") *::__getitem__;*/
%feature("python:slot", "tp_getattro", functype = "binaryfunc") *::__getattro__;

// Attributes
%attribute(xpm::Argument, bool, required, required, required);
%ignore xpm::Argument::required;

/*%attributeval(xpm::Argument, std::shared_ptr<xpm::Object>, Object, defaultValue, defaultValue)
%ignore xpm::Argument::defaultValue;
*/
%attributeval(xpm::Argument, std::shared_ptr<xpm::Generator>, generator, generator, generator)
%ignore xpm::Argument::generator;

%attribute(xpm::Argument, std::string, help, help, help)
%ignore xpm::Argument::help;

/*%attribute(xpm::Argument, Type, type, type, type)*/
/*%ignore xpm::Argument::type;*/


// Returns the wrapped python object rather than the director object
%{
    namespace xpm { namespace python {
        PyObject * getRealObject(std::shared_ptr<xpm::Object> const &object, swig_type_info *swigtype) {
            if (object) {
                if (Swig::Director * d = SWIG_DIRECTOR_CAST(object.get())) {
                    Py_INCREF(d->swig_get_self());
                    PyObject *pyObject = d->swig_get_self();
                    return d->swig_get_self();
                }

                std::shared_ptr<  xpm::Object > * smartresult = new std::shared_ptr<xpm::Object>(object);
                return SWIG_InternalNewPointerObj(SWIG_as_voidptr(smartresult), swigtype, SWIG_POINTER_OWN);
            }
            return SWIG_Py_Void();
        }
    }}
%}

%typemap(out) std::shared_ptr<xpm::Object> {
    // OUT-OBJECT
    $result = xpm::python::getRealObject($1, $descriptor(std::shared_ptr<xpm::Object>*));
}

%typemap(directorin) std::shared_ptr<xpm::Object> const & {
    $input = xpm::python::getRealObject($1, $descriptor(std::shared_ptr<xpm::Object>*));
}

%extend xpm::Object {
    /*void __setitem__(std::string const & key, std::shared_ptr<xpm::Object> const &value) {
        (*($self))[key] = value;
    }
    void __setitem__(std::string const & key, std::map<std::string, std::shared_ptr<xpm::Object>> &value) {
        (*($self))[key] = std::make_shared<xpm::Object>(value);
    }
    void __setitem__(std::string const & key, Value const &value) {
        (*($self))[key] = std::make_shared<xpm::Object>(value);
    }*/

    PyObject * __getattro__(PyObject *name) {
        auto _self = xpm::python::getRealObject($self->shared_from_this(), $descriptor(std::shared_ptr<xpm::Object>*));

        PyObject *object = PyObject_GenericGetAttr(_self, name);
        if (object) {
            return object;
        }

        if (!PyUnicode_Check(name)) {
            PyErr_SetString(PyExc_AttributeError, "Attribute name is not a string");
            return nullptr;
        }

        Py_ssize_t stringsize;
        char *_key = (char*)PyUnicode_AsUTF8AndSize(name, &stringsize);
        std::string key(_key, stringsize);

      if ($self->hasKey(key)) {
          PyErr_Clear();
         return xpm::python::getRealObject($self->get(key), $descriptor(std::shared_ptr<xpm::Object>*));
      }

      std::cerr << "Could not find attribute\n";
      PyErr_SetString(PyExc_AttributeError, (std::string("Could not find attribute ") + key).c_str());
      return nullptr;
    }
}
