%module(package="experimaestro") rpc

%{
    #include <xpm/rpc/utils.hpp>
    #include <xpm/rpc/objects.hpp>
%}

%include "std_shared_ptr.i"
%include "std_string.i"

namespace xpm {
    namespace rpc {
}
}

%shared_ptr(xpm::rpc::ServerObject);

%include <xpm/rpc/utils.hpp>
%include <xpm/rpc/objects.hpp>
