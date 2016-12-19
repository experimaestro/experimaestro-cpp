%module(directors="1", jniclasspackage="xpm") experimaestro


#define SWIG_IGNORE %ignore
#define SWIG_REMOVE(x)
#define XPM_PIMPL(x) x
#define XPM_PIMPL_CHILD(name, parent) name: public parent

%pragma(java) jniclasspackage="xpm";

%{
#include <xpm/xpm.hpp>
#include <xpm/context.hpp>
#include <xpm/filesystem.hpp>
#include <xpm/commandline.hpp>
#include <xpm/logging.hpp>
%}


%include "std_string.i"

#ifdef SWIGJAVA
%include "java/common.i"
%nspace xpm::set_workdir;
#endif

// Support for intxx_t
%include "stdint.i"
// Support for standard C++ structures
#ifndef SWIGJAVA
%include "std_vector.i"
#endif

%include "std_map.i"
%include "std_shared_ptr.i"
// Handles exceptions
%include "exception.i"
// Handle attributes for languages supporting this (Python)
%include "attribute.i"

#ifdef SWIGPYTHON
// Implicit conversions
%implicitconv;
%implicitconv xpm::Path;
#endif


// Documentation
%include "documentation.i"

// Exception handling
#ifdef SWIGPYTHON
%exception {
   try {
      $action
   } catch (Swig::DirectorException &e) {
      SWIG_fail;
      SWIG_exception(SWIG_RuntimeError, e.what());
   } catch(std::exception &e) {
      SWIG_exception(SWIG_RuntimeError, e.what());
   }
}

%feature("director:except") {
    if ($error != NULL) {
        throw Swig::DirectorMethodException();
    }
}
#endif

// Imports
%import "ignores.i";
%import "xpm_rpc.i"

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
%attribute(xpm::Argument, Generator *, generator, generator, generator)
%ignore xpm::Argument::generator;

%attribute(xpm::Argument, std::string, help, help, help)
%ignore xpm::Argument::help;

/*%attribute(xpm::Argument, Type, type, type, type)*/
%ignore xpm::Argument::type;

#ifdef SWIGJAVA
%nspace xpm::Object;
%nspace xpm::Type;
%nspace xpm::Task;
%nspace xpm::Argument;
%nspace xpm::ObjectFactory;
%nspace xpm::Command;
%nspace xpm::Argument;
%nspace xpm::Context;
%nspace xpm::Generator;
%nspace xpm::Path;
%nspace xpm::Register;
%nspace xpm::Share;
%nspace xpm::PathGenerator;
%nspace xpm::Generator;
%nspace xpm::LogLevel;
%nspace xpm::TypeName;
%nspace xpm::Value;
%nspace xpm::ValueType;

%nspace xpm::AbstractCommandComponent;
%nspace xpm::CommandContext;
%nspace xpm::CommandContent;
%nspace xpm::CommandParameters;
%nspace xpm::CommandString;
%nspace xpm::CommandPath;
%nspace xpm::CommandLine;
#endif

%shared_ptr(xpm::Object)
%shared_ptr(xpm::Type)
%shared_ptr(xpm::Task)
%shared_ptr(xpm::Object)
%shared_ptr(xpm::Argument)
%shared_ptr(xpm::ObjectFactory)
%shared_ptr(xpm::Value)
%shared_ptr(xpm::Array)
%shared_ptr(xpm::Register)

// Object and object factory have virtual methods
%feature("director") xpm::ObjectFactory;
%feature("director") xpm::Object;
%feature("nodirector") xpm::Object::toJson;
%feature("nodirector") xpm::Object::equals;
%feature("nodirector") xpm::Object::digest;

%template(String2Object) std::map<std::string, std::shared_ptr<xpm::Object>>;

// Returns the wrapped python object rather than the director object
#ifdef SWIGPYTHON
%{
    namespace xpm { namespace python {
        PyObject * getRealObject(std::shared_ptr<xpm::Object> const &object, swig_type_info *swigtype) {
            if (object) {
                if (Swig::Director * d = SWIG_DIRECTOR_CAST(object.get())) {
                    Py_INCREF(d->swig_get_self());
                    return d->swig_get_self();
                } 
             
                std::shared_ptr<  xpm::Object > * smartresult = new std::shared_ptr<xpm::Object>(object);
                return SWIG_InternalNewPointerObj(SWIG_as_voidptr(smartresult), swigtype, SWIG_POINTER_OWN);
            }
            return SWIG_Py_Void();    
        }
    }}
%}

%typemap(out) std::shared_ptr<xpm::Object> { $result = xpm::python::getRealObject($1, $descriptor(std::shared_ptr<xpm::Object>*)); }
%typemap(directorin) std::shared_ptr<xpm::Object> const & { $input = xpm::python::getRealObject($1, $descriptor(std::shared_ptr<xpm::Object>*));  }

#endif


// Include file
%include <xpm/filesystem.hpp>
%include <xpm/commandline.hpp>
%include <xpm/context.hpp>
%include <xpm/xpm.hpp>
%include <xpm/logging.hpp>

// Optional
%extend xpm::optional {
    bool hasValue() const { return *$self; }
}

%template(set) xpm::Object::set<std::string>;
%template(set) xpm::Object::set<long>;
%template(set) xpm::Object::set<double>;
%template(set) xpm::Object::set<bool>;


// Template instanciation
%template(StringList) std::vector<std::string>;

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
