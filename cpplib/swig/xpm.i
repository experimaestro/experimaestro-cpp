%module(directors="1") experimaestro

#define SWIG_IGNORE %ignore
#define SWIG_REMOVE(x)
#define XPM_PIMPL(x) x
#define XPM_PIMPL_CHILD(name, parent) name: public parent

%{
#include <xpm/xpm.hpp>
#include <xpm/context.hpp>
#include <xpm/filesystem.hpp>
#include <xpm/commandline.hpp>
// #include <xpm/rpc/objects.hpp>
/*using xpm::Path;*/
%}

// Support for standard C++ structures
%include "std_string.i"
%include "std_vector.i"
%include "std_map.i"
%include "std_shared_ptr.i"
// Handles exceptions
%include "exception.i"
// Handle attributes for languages supporting this (Python)
%include "attribute.i"

%import "xpm_rpc.i"

// Implicit conversions
%implicitconv;
%implicitconv xpm::Path;

// Documentation
%include "documentation.i"

// Exception handling
%exception {
   try {
      $action
   } catch (Swig::DirectorException &e) {
      SWIG_fail;
   } catch(std::exception &e) {
      SWIG_exception(SWIG_RuntimeError, e.what());
   }
}

%feature("director:except") {
    if ($error != NULL) {
        throw Swig::DirectorMethodException();
    }
}


// Ignores
%import "ignores.i";

// Python slots
// See https://docs.python.org/3/c-api/typeobj.html
%feature("python:slot", "tp_str",functype = "reprfunc") *::toString;
%feature("python:slot", "tp_repr", functype = "reprfunc") *::toString;
%feature("python:slot", "tp_call", functype = "ternarycallfunc") *::call;
%feature("python:slot", "tp_hash", functype = "hashfunc") *::hash;
%feature("python:slot", "mp_subscript", functype = "binaryfunc") *::__getitem__;
%feature("python:slot", "mp_ass_subscript", functype = "objobjargproc") *::__setitem__;

// Attributes
%attribute(xpm::Argument, bool, required, required, required);
%ignore xpm::Argument::required;

%attributeval(xpm::Argument, xpm::Value, defaultValue, defaultValue, defaultValue)
%ignore xpm::Argument::defaultValue;

%attribute(xpm::Argument, Generator *, generator, generator, generator)
%ignore xpm::Argument::generator;

%attribute(xpm::Argument, std::string, help, help, help)
%ignore xpm::Argument::help;

/*%attribute(xpm::Argument, Type, type, type, type)*/
%ignore xpm::Argument::type;
%ignore xpm::StructuredValue::operator[];

%shared_ptr(xpm::Object)
%shared_ptr(xpm::Type)
%shared_ptr(xpm::StructuredValue)
%shared_ptr(xpm::Argument)
%shared_ptr(xpm::ObjectFactory)

// Object and object factory have virtual methods
%feature("director") xpm::ObjectFactory;
%feature("director") xpm::Object;

%template(String2StructuredValue) std::map<std::string, xpm::StructuredValue>;

// Returns the wrapped python object rather than the director object
#ifdef SWIGPYTHON
%typemap(out) std::shared_ptr<xpm::Object> {
    if ($1) {
        if (Swig::Director * d = SWIG_DIRECTOR_CAST($1.get())) {
            Py_INCREF(d->swig_get_self());
            $result = d->swig_get_self();
        } else {
            std::shared_ptr<  xpm::Object > * smartresult = new std::shared_ptr<  xpm::Object >(result SWIG_NO_NULL_DELETER_SWIG_BUILTIN_INIT);
            $result = SWIG_NewPointerObj(SWIG_as_voidptr(smartresult), $descriptor(std::shared_ptr< xpm::Object > *), SWIG_POINTER_OWN);
        }
    } else {
        $result = SWIG_Py_Void();
    }
}
#endif


// Include file
%include <xpm/filesystem.hpp>
%include <xpm/commandline.hpp>
%include <xpm/context.hpp>
%include <xpm/xpm.hpp>
/*%include <xpm/rpc/utils.hpp>*/
/*%include <xpm/rpc/objects.hpp>*/

// Optional
%extend xpm::optional {
    bool hasValue() const { return *$self; }
}

%template(ConstTaskOptional) xpm::optional<xpm::Task const>;
%template(ConstTypeOptional) xpm::optional<xpm::Type const>;
%template(TaskOptional) xpm::optional<xpm::Task>;
%template(TypeOptional) xpm::optional<xpm::Type>;

%template(set) xpm::Object::set<std::string>;
%template(set) xpm::Object::set<long>;
%template(set) xpm::Object::set<double>;
%template(set) xpm::Object::set<std::shared_ptr<xpm::Object>>;

%template(StringList) std::vector<std::string>;

%extend xpm::StructuredValue {
    void __setitem__(std::string const & key, xpm::StructuredValue const &value) {
        (*($self))[key] = value;
    }
    void __setitem__(std::string const & key, std::map<std::string, xpm::StructuredValue> &value) {
        (*($self))[key] = value;
    }
    void __setitem__(std::string const & key, Value const &value) {
        (*($self))[key] = value;
    }
    StructuredValue __getitem__(std::string const & key) {
      return (*($self))[key];
    }
}


