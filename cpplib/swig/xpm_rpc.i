%module(package="experimaestro") rpc

#ifdef SWIGJAVA
%pragma(java) jniclasspackage="xpm.rpc";
%nspace xpm::rpc::ObjectIdentifier;
%nspace xpm::rpc::ServerObject;
#endif

%{
    #include <stdexcept>
    #include <xpm/rpc/optional.hpp>
    #include <xpm/rpc/utils.hpp>
    #include <xpm/rpc/objects.hpp>
%}

#ifdef SWIGJAVA
%include "java/common.i"
%nspace xpm::set_workdir;
#endif

#ifdef SWIGPYTHON
%implicitconv
%implicit(xpm::rpc::optional<bool>, bool);
#endif

// Support for intxx_t
%include "stdint.i"
%include "exception.i"
%include "std_shared_ptr.i"
%include "std_string.i"

namespace xpm {
    namespace rpc {
}
}

%shared_ptr(xpm::rpc::ServerObject);

%include <xpm/rpc/optional.hpp>
%include <xpm/rpc/utils.hpp>
%include <xpm/rpc/objects.hpp>

// Optional
%extend xpm::rpc::optional {
    bool hasValue() const { return *$self; }
}
%template(BoolOptional) xpm::rpc::optional<bool>;
%template(IntOptional) xpm::rpc::optional<int>;
%template(UIntOptional) xpm::rpc::optional<unsigned int>;
%template(StringOptional) xpm::rpc::optional<std::string>;
