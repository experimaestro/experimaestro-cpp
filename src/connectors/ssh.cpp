//
// Created by Benjamin Piwowarski on 19/01/2017.
//

#include <libssh/libssh.h>
#include <string>

#include <xpm/connectors/ssh.hpp>
#include <xpm/launchers/launchers.hpp>

namespace xpm {

class SSHSession {
  ssh_session session;
  bool connected;

public:
  SSHSession() {
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
      throw std::runtime_error("Error connecting" +
                               std::string(ssh_get_error(session)));
    }
    connected = true;
  }

  ~SSHSession() {
    if (connected)
      ssh_disconnect(session);
    ssh_free(session);
  }

  SSHSession &port(int port) {
    setOption(SSH_OPTIONS_PORT, &port);
    return *this;
  }

  SSHSession &host(std::string const &hostname) {
    setOption(SSH_OPTIONS_HOST, hostname.c_str());
    return *this;
  }

  SSHSession &username(std::string const &username) {
    setOption(SSH_OPTIONS_USER, username.c_str());
    return *this;
  }
};

// --- SSH process builder

class SSHProcess : public Process {
  /// isRunning
  virtual bool isRunning() override {}

  /// Exit code
  virtual int exitCode() override {}

  /// Kill
  virtual void kill(bool force) override {}

  /**
   * Write to stdin
   * @return true if the write succeeded, false otherwise
   */
  virtual bool writeStdin(std::string const &string) override {}
};

class SSHProcessBuilder : public ProcessBuilder {
  std::shared_ptr<SSHSession> _session;

public:
  SSHProcessBuilder(std::shared_ptr<SSHSession> const &session)
      : _session(session) {}

  std::shared_ptr<Process> start() {}
};

// --- SSH (connect)or

template<class T, class U>
std::shared_ptr<T> shared_this(U * ptr) {
  return std::dynamic_pointer_cast<T>(ptr->shared_from_this());
}
template<class T, class U>
std::shared_ptr<T> shared_this(U const * ptr) {
  return std::dynamic_pointer_cast<T>(const_cast<U*>(ptr)->shared_from_this());
}

SSHConnector::~SSHConnector() {}

std::shared_ptr<ProcessBuilder> SSHConnector::processBuilder() const {
  return std::make_shared<SSHProcessBuilder>(_session);
}

std::string SSHConnector::resolve(Path const &path) const {}

void SSHConnector::setExecutable(Path const &path, bool flag) const {}

void SSHConnector::mkdirs(Path const &path, bool createParents,
                                  bool errorExists) const {}

FileType SSHConnector::fileType(Path const &path) const {}

std::unique_ptr<std::ostream> SSHConnector::ostream(Path const &path) const {}

std::unique_ptr<std::istream> SSHConnector::istream(Path const &path) const {}

} // namespace xpm
