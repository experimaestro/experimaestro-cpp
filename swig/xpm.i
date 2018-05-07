%module(directors="1", jniclasspackage="xpm") experimaestro


#define SWIG_IGNORE %ignore
#define NOSWIG(...)
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
#include <xpm/launchers.hpp>
#include <xpm/workspace.hpp>
#include <SQLiteCpp/SQLiteCpp.h>

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

%shared_ptr(xpm::Object)
%shared_ptr(xpm::Type)
%shared_ptr(xpm::SimpleType)
%shared_ptr(xpm::Task)
%shared_ptr(xpm::StructuredValue)
%shared_ptr(xpm::Argument)
%shared_ptr(xpm::Register)
%shared_ptr(xpm::Generator)
%shared_ptr(xpm::PathGenerator)
%shared_ptr(xpm::Context)

%shared_ptr(xpm::Dependency)
%shared_ptr(xpm::Resource)
%shared_ptr(xpm::Token)
%shared_ptr(xpm::CounterToken)
%shared_ptr(xpm::Job)
%shared_ptr(xpm::CommandLineJob)
%shared_ptr(xpm::Workspace)

%shared_ptr(xpm::AbstractCommandComponent)
%shared_ptr(xpm::AbstractCommand)
%shared_ptr(xpm::CommandPart)
%shared_ptr(xpm::CommandLine)
%shared_ptr(xpm::Command)
%shared_ptr(xpm::CommandPath)
%shared_ptr(xpm::CommandContent)
%shared_ptr(xpm::CommandPathReference)
%shared_ptr(xpm::CommandParameters)
%shared_ptr(xpm::CommandString)


// Object and object factory have virtual methods that have to be overridden
%feature("director") xpm::Register;
%feature("director") xpm::Object;

#ifdef SWIGPYTHON
%include "python/xpm.i"
#endif

#ifdef SWIGJAVA
%include "java/xpm.i"
#endif

namespace xpm {
    template <typename T> using ptr = std::shared_ptr<T>;
}

// Include file
%include <xpm/filesystem.hpp>
%include <xpm/launchers.hpp>
%include <xpm/commandline.hpp>
%include <xpm/workspace.hpp>
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
