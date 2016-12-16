%module(package="experimaestro") rpc

#ifdef SWIGJAVA
%pragma(java) jniclasspackage="xpm.rpc";
%nspace xpm::rpc::ObjectIdentifier;
%nspace xpm::rpc::ServerObject;
#endif

%{
    #include <xpm/rpc/utils.hpp>
    #include <xpm/rpc/objects.hpp>
%}

#ifdef SWIGJAVA
%include "java/common.i"
%nspace xpm::set_workdir;
#endif


// Support for intxx_t
%include "stdint.i"
%include "std_shared_ptr.i"
%include "std_string.i"

namespace xpm {
    namespace rpc {
}
}


%shared_ptr(xpm::rpc::ServerObject);

%include <xpm/rpc/utils.hpp>
%include <xpm/rpc/objects.hpp>
