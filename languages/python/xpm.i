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

%extend xpm::Parameters { 
    %COLLECTION(std::shared_ptr<xpm::Parameters>) 

};

%{
/** 
 * Returns the wrapped python object rather than the director object.
 * This is useful since an XPM object might be subclassed
*/

   namespace xpm { namespace python {

      PyObject * getRealObject(std::shared_ptr<xpm::Object> const &object) {
         if (object) {
            // This is a Director object
            if (Swig::Director * d = SWIG_DIRECTOR_CAST(object.get())) {
               Py_INCREF(d->swig_get_self());
               return d->swig_get_self();
            }

            std::shared_ptr< xpm::Object > * smartresult = new std::shared_ptr<xpm::Object>(object);
            return SWIG_InternalNewPointerObj(SWIG_as_voidptr(smartresult), SWIGTYPE_p_std__shared_ptrT_xpm__Object_t, SWIG_POINTER_OWN);
         }


         // Returns None
         return SWIG_Py_Void();
      }
      
   }} // Ends xpm::python
%}

// Get the real object for a shared_ptr of an object
%typemap(out) std::shared_ptr<xpm::Object> {
    // Retrieving Python object (and not the director)
    $result = xpm::python::getRealObject($1);
}

// Handles properly a smart pointer
%typemap(directorin) std::shared_ptr< xpm::Parameters > const & {
    // Handles null smart pointer
    if (!$1) {
        $input = SWIG_Py_Void();
    } else {
        std::shared_ptr< xpm::Parameters > * ptr = new std::shared_ptr< xpm::Parameters >($1);
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



// TODO: GARBAGE SECTION....

#ifndef SWIG



%{
    #include <xpm/common.hpp>


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
        auto _self = xpm::python::getRealObject($self->shared_from_this());

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
         return xpm::python::getRealObject($self->get(key));
      }

      // std::cerr << "Could not find attribute " << key << "\n";
      PyErr_SetString(PyExc_AttributeError, (std::string("Could not find attribute ") + key).c_str());
      return nullptr;
    }

    PyObject *call() {
        // If we have a value, just return it
        if (auto valuePtr = dynamic_cast<xpm::Value*>($self)) {
            switch(valuePtr->scalarType()) {
                case xpm::ValueType::BOOLEAN:
                    if (valuePtr->asBoolean()) { Py_RETURN_TRUE; } else { Py_RETURN_FALSE; }

                case xpm::ValueType::NONE:
                    return SWIG_Py_Void();

                case xpm::ValueType::PATH: {
                    auto pathPtr = new xpm::Path($self->asPath());
                    return SWIG_InternalNewPointerObj(pathPtr, $descriptor(xpm::Path*), SWIG_POINTER_OWN );
                }

                case xpm::ValueType::STRING:
                    return PyUnicode_FromString($self->asString().c_str());

                case xpm::ValueType::INTEGER:
                    return PyLong_FromLong($self->asInteger());

                case xpm::ValueType::REAL:
                    return PyFloat_FromDouble($self->asReal());
            }
        }

        // Otherwise return void
        return SWIG_Py_Void();
    }
}

%}

#endif