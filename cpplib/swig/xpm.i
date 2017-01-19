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
#include <xpm/register.hpp>
#include <xpm/logging.hpp>
#include <xpm/value.hpp>
#undef SWIG_PYTHON_DIRECTOR_VTABLE
%}


%include "std_string.i"

#ifdef SWIGJAVA
%include "java/common.i"
#endif

#ifdef SWIGPYTHON
%include "python/common.i"
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
%implicitconv xpm::TypeName;
#endif


// Documentation
%include "documentation.i"


// Imports
%import "ignores.i";
%import "xpm_rpc.i"

%shared_ptr(xpm::Object)
%shared_ptr(xpm::Type)
%shared_ptr(xpm::Task)
%shared_ptr(xpm::Object)
%shared_ptr(xpm::Argument)
%shared_ptr(xpm::ObjectFactory)
%shared_ptr(xpm::Value)
%shared_ptr(xpm::Array)
%shared_ptr(xpm::Register)

#ifdef SWIGPYTHON
%include "python/xpm.i"
#endif

#ifdef SWIGJAVA
%include "java/xpm.i"
#endif


// Object and object factory have virtual methods
%feature("director") xpm::ObjectFactory;
%feature("director") xpm::Object;
%feature("nodirector") xpm::Object::toJson;
%feature("nodirector") xpm::Object::equals;
%feature("nodirector") xpm::Object::digest;

%template(String2Object) std::map<std::string, std::shared_ptr<xpm::Object>>;

// Include file
%include <xpm/filesystem.hpp>
%include <xpm/commandline.hpp>
%include <xpm/context.hpp>
%include <xpm/xpm.hpp>
%include <xpm/value.hpp>
%include <xpm/register.hpp>
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
