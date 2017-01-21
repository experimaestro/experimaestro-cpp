//
// Created by Benjamin Piwowarski on 19/01/2017.
//

#include <string>
#include <libssh/libssh.h>
#include "xpmssh.hpp"

namespace xpm {

template<>
struct Reference<ssh::Session> {
  ssh_session session;
  bool connected = false;

  Reference() {
    session = ssh_new();
    if (session == NULL) {
      throw std::runtime_error("Could not initialize SSH session");
    }

  }

  void setOption(enum ssh_options_e option, void const *x) {
    ::ssh_options_set(session, option, x);
  }

  void connect() {
    auto rc = ssh_connect(session);
    if (rc != SSH_OK) {
      throw std::runtime_error("Error connecting" + std::string(ssh_get_error(session)));
    }
    connected = true;
  }

  ~Reference() {
    if (connected) ssh_disconnect(session);
    ssh_free(session);
  }
};

ssh::Session &ssh::Session::port(int port) {
  self().setOption(SSH_OPTIONS_PORT, &port);
  return *this;
}

ssh::Session &ssh::Session::host(std::string const &hostname) {
  self().setOption(SSH_OPTIONS_HOST, hostname.c_str());
  return *this;
}

ssh::Session &ssh::Session::username(std::string const &username) {
  self().setOption(SSH_OPTIONS_USER, username.c_str());
  return *this;
}

void ssh::Session::connect() {
  self().connect();
}
}

