%module(directors="1") experimaestro


%{
#include <xpm/xpm.h>
#include <xpm/rpc/objects.hpp>

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


// Implicit conversions
%implicitconv;

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
%import "ignores.i"

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

// FIXME: hack to get access to the real object -> we should use typemap or patch...
%{
PyObject *swigGetSelf(xpm::Object const *p) {
    if (Swig::Director const *d = dynamic_cast<Swig::Director const *>(p)) {
        return d->swig_get_self();
    }
    return nullptr;
}
%}
%extend xpm::Object {
    PyObject *self() const {
      return swigGetSelf($self);
    } 
};

%shared_ptr(xpm::Object)
%shared_ptr(xpm::ObjectFactory)

// Object and object factory have virtual methods
%feature("director") xpm::ObjectFactory;
%feature("director") xpm::Object;

%template(String2StructuredValue) std::map<std::string, xpm::StructuredValue>;


// Include file
%include <xpm/xpm.h>
%include <xpm/Context.hpp>
%include <xpm/rpc/utils.hpp>
%include <xpm/rpc/objects.hpp>

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

