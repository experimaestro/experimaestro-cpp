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
/*%feature("python:slot", "tp_getattro", functype = "binaryfunc") *::__getattr__;*/

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
%typemap(out) std::shared_ptr<xpm::Object> {
    if ($1) {
        if (Swig::Director * d = SWIG_DIRECTOR_CAST($1.get())) {
            Py_INCREF(d->swig_get_self());
            $result = d->swig_get_self();
        } else {
            std::shared_ptr<  xpm::Object > * smartresult = new std::shared_ptr<  xpm::Object >($1 SWIG_NO_NULL_DELETER_SWIG_BUILTIN_INIT);
            $result = SWIG_NewPointerObj(SWIG_as_voidptr(smartresult), $descriptor(std::shared_ptr< xpm::Object > *), SWIG_POINTER_OWN);
        }
    } else {
        $result = SWIG_Py_Void();
    }
}

%typemap(directorin) std::shared_ptr<xpm::Object> const & {
    if ($1) {
        if (Swig::Director * d = SWIG_DIRECTOR_CAST($1.get())) {
            Py_INCREF(d->swig_get_self());
            $input = d->swig_get_self();
        } else {
            std::shared_ptr<  xpm::Object > * smartresult = new std::shared_ptr<  xpm::Object >($1 SWIG_NO_NULL_DELETER_SWIG_BUILTIN_INIT);
            $input = SWIG_NewPointerObj(SWIG_as_voidptr(smartresult), $descriptor(std::shared_ptr< xpm::Object > *), SWIG_POINTER_OWN);
        }
    } else {
        $input = SWIG_Py_Void();
    }
}
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

    PyObject * __getattribute__(PyObject *name) {
      char const *key = (char*)PyUnicode_DATA(name); // FIXME : check!
      if (!key) {
         std::cerr << "Object is not string\n";
         return nullptr;
      }

         std::cerr << "In __getattr__ " << key << std::endl;
         if ($self->hasKey(key)) {
            std::shared_ptr<xpm::Object> value = $self->get(key);
            auto pvalue = new std::shared_ptr<xpm::Object>(value);
            PyObject *_value = SWIG_NewFunctionPtrObj(SWIG_as_voidptr(pvalue), $descriptor(std::shared_ptr< xpm::Object > *));
            return _value;
         }

      /*std::cerr << "Searching base class " << key << std::endl;*/
      /*std::cerr << "///" << PyObject_GenericGetAttr(SwigPyObject_TypeOnce(), "__getattribute__") << std::endl;*/
      auto pself = new std::shared_ptr<xpm::Object>($self);
      PyObject *_self = SWIG_NewFunctionPtrObj(SWIG_as_voidptr(pself), $descriptor(std::shared_ptr< xpm::Object > *));
      PyObject *_result = nullptr;

      auto mro = _self->ob_type->tp_mro;
      for(size_t i = 0, N = PyTuple_Size(mro); i < N; ++i) {
         PyTypeObject *base = (PyTypeObject*)PyTuple_GetItem(mro, i);
         PyObject *dict = base->tp_dict;
         auto item = PyDict_GetItem(dict, name);
         /*std::cerr << "[search] " << base->tp_name << std::endl;*/
         if (item) {
            std::cerr << "Yo, find in " << base->tp_name << " / " << item->ob_type->tp_name << std::endl;
            _result = item;
            std::cerr << PyInstanceMethod_Check(item) << std::endl;
            break;
         }
      }

      Py_DECREF(_self);
      if (_result) {
         Py_INCREF(_result);
         return _result;
      }

      PyErr_SetString(PyExc_AttributeError, (std::string("Could not find attribute ") + key).c_str());
      return nullptr;
    }
}
