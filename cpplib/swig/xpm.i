%module(directors="1", jniclasspackage="xpm") experimaestro


#define SWIG_IGNORE %ignore
#define SWIG_REMOVE(x)
#define XPM_PIMPL(x) x
#define XPM_PIMPL_CHILD(name, parent) name: public parent

%pragma(java) jniclasspackage="xpm";

%{
#include <xpm/xpm.hpp>
#include <xpm/common.hpp>
#include <xpm/context.hpp>
#include <xpm/filesystem.hpp>
#include <xpm/commandline.hpp>
#include <xpm/value.hpp>
#include <xpm/register.hpp>
#include <xpm/logging.hpp>
#undef SWIG_PYTHON_DIRECTOR_VTABLE
%}



#ifdef SWIGPYTHON
// Implicit conversions
%implicitconv;
%implicitconv xpm::Path;
%implicit(xpm::TypeName, std::string);
#endif

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



// Documentation
%include "documentation.i"


// Imports
%import "ignores.i";
%import "xpm_rpc.i"

%shared_ptr(xpm::Object)
%shared_ptr(xpm::Type)
%shared_ptr(xpm::SimpleType)
%shared_ptr(xpm::Task)
%shared_ptr(xpm::StructuredValue)
%shared_ptr(xpm::Argument)
%shared_ptr(xpm::Register)
%shared_ptr(xpm::Generator)
%shared_ptr(xpm::PathGenerator)

#ifdef SWIGPYTHON
%include "python/xpm.i"
#endif

#ifdef SWIGJAVA
%include "java/xpm.i"
#endif

// Object and object factory have virtual methods
%feature("director") xpm::Register;
%feature("director") xpm::Object;

// Include file
%include <xpm/filesystem.hpp>
%include <xpm/commandline.hpp>
%include <xpm/context.hpp>
%include <xpm/value.hpp>
%include <xpm/xpm.hpp>
%include <xpm/task.hpp>
%include <xpm/register.hpp>
%include <xpm/logging.hpp>

// Template instanciation
%template(StringList) std::vector<std::string>;



%exception {
    try {
        $action
    }
    catch (const std::exception & e)
    {
        SWIG_exception(SWIG_RuntimeError, (std::string("C++ std::exception: ") + e.what()).c_str());
    }
    catch (const xpm::exception & e)
    {
        SWIG_exception(SWIG_RuntimeError, (std::string("C++ xpm::exception: ") + e.what()).c_str());
    }
    catch (...)
    {
        SWIG_exception(SWIG_UnknownError, "C++ anonymous exception");
    }
}
