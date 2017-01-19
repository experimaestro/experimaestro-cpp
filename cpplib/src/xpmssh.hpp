//
// Created by Benjamin Piwowarski on 19/01/2017.
//

#ifndef EXPERIMAESTRO_XPMSSH_HPP
#define EXPERIMAESTRO_XPMSSH_HPP

#include <xpm/utils.hpp>

namespace xpm {
namespace ssh {
  /// An SSH session
  class Session : public Pimpl<Session> {
    Session &port(int);
    Session &host(std::string const &hostname);
    Session &username(std::string const &username);

    void connect();
  };
}
}

#endif // EXPERIMAESTRO_XPMSSH_HPP
