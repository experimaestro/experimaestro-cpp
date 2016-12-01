%module(directors="1") experimaestro

%{
#include <xpm/xpm.h>
%}

// Useful imports
%include "std_string.i"
%include "exception.i"
%include "std_shared_ptr.i"

// Handle attributes for languages supporting this (Python)
%include <attribute.i>

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
%ignore std::exception;
%ignore std::enable_shared_from_this<Object>;

// Python slots
// See https://docs.python.org/3/c-api/typeobj.html
%feature("python:slot", "tp_str",functype = "reprfunc") *::toString;
%feature("python:slot", "tp_repr", functype = "reprfunc") *::toString;
%feature("python:slot", "tp_call", functype = "ternarycallfunc") *::call;
%feature("python:slot", "tp_hash", functype = "hashfunc") *::hash;

// Attributes

%attribute(xpm::Argument, bool, required, required, required);
%ignore xpm::Argument::required;

%attributeval(xpm::Argument, xpm::Value, defaultValue, defaultValue, defaultValue)
%ignore xpm::Argument::defaultValue;

%attribute(xpm::Argument, std::string, help, help, help)
%ignore xpm::Argument::help;

%attribute(xpm::Argument, std::shared_ptr<Type>, type, type, type)
%ignore xpm::Argument::type;

%shared_ptr(xpm::Namespace)
%shared_ptr(xpm::StructuredValue)
%shared_ptr(xpm::Object)
%shared_ptr(xpm::ObjectHolder)

%shared_ptr(xpm::Argument)
%shared_ptr(xpm::Type)
%shared_ptr(xpm::Task)

%feature("director") xpm::Register;
%feature("director") xpm::Object;
%feature("director") xpm::Type;

// Include file
%include <xpm/xpm.h>

%template(set) xpm::Object::set<std::string>;
%template(set) xpm::Object::set<long>;
%template(set) xpm::Object::set<double>;
%template(set) xpm::Object::set<std::shared_ptr<xpm::Object>>;

/*%extend xpm::Object {
    std::shared_ptr<Object> __getitem__(std::string const & key) {
      return (*($self))[key];
    }
}
*/
/*%pythoncode "swig/python/xpm.swig.py"*/
