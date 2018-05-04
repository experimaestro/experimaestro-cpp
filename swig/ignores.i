/*%module sys

%include "std_shared_ptr.i";

%define PIMPL(name)
class name;
%template(Pimpl ## name) Pimpl<name>;
%enddef

%define SHAREDFROMTHIS(name)
class xpm::name;
%shared_ptr(std::enable_shared_from_this<xpm::name>);
%template(SharedFromThis ## name) std::enable_shared_from_this<xpm::name>;
%enddef


namespace std {
    class exception {};
    template<typename T> class enable_shared_from_this {};
}

SHAREDFROMTHIS(Object);

namespace xpm {
    template<typename T> class Pimpl {};
    PIMPL(Share);
    PIMPL(Path);
    PIMPL(Context);
}
*/