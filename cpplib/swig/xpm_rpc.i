%module(package="experimaestro") rpc

%{
    #include <xpm/rpc/utils.hpp>
    #include <xpm/rpc/objects.hpp>
%}

namespace xpm {
    namespace rpc {
}
}

%include <xpm/rpc/utils.hpp>
%include <xpm/rpc/objects.hpp>
