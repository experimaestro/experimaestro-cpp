//
// Created by Benjamin Piwowarski on 19/01/2017.
//

#include <string>
#include <sstream>
#include <regex>

#include <libssh/callbacks.h>
#include <libssh/libssh.h>
#include <libssh/sftp.h>

#include <spdlog/fmt/fmt.h>
#include <sys/stat.h>

#include <__xpm/common.hpp>
#include <__xpm/scriptbuilder.hpp>
#include <xpm/connectors/ssh.hpp>
#include <xpm/launchers/launchers.hpp>

DEFINE_LOGGER("ssh");

namespace xpm {

struct SFTPSession {
  ssh_session session;
  sftp_session sftp;
  SFTPSession(SFTPSession const &) = delete;
  ~SFTPSession() { sftp_free(sftp); }

  SFTPSession(ssh_session session) : session(session) {
    sftp = sftp_new(session);
    if (sftp == NULL) {
      throw io_error(fmt::format("Error allocating SFTP session: {}",
                                 ssh_get_error(session)));
    }

    auto rc = sftp_init(sftp);
    if (rc != SSH_OK) {
      sftp_free(sftp);
      throw io_error(fmt::format("Error allocating SFTP session: {}",
                                 sftp_get_error(sftp)));
    }
  }

  void chmod(std::string const &path, mode_t mode) {
    auto rc = sftp_chmod(sftp, path.c_str(), mode);
    if (rc != SSH_OK) {
      throw io_error(
          fmt::format("Can't chmod {}: {}", path, ssh_get_error(session)));
    }
  }

  void mkdir(std::string const &path) {
    auto rc = sftp_mkdir(sftp, path.c_str(), S_IRWXU);
    if (rc != SSH_OK) {
      throw io_error(fmt::format("Can't create directory {}: {}", path,
                                 ssh_get_error(session)));
    }
  }
};


class SSHSession {
  ssh_session session;
  bool connected;

public:

  /// SSH session mutex
  std::mutex mutex;

  SSHSession() {
    static bool initialized = false;
    if (!initialized) {
      initialized = true;
      ssh_threads_set_callbacks(ssh_threads_get_pthread());
      ssh_init();
    }

    session = ssh_new();
    if (session == NULL) {
      throw io_error("Could not initialize SSH session");
    }
  }

  operator ssh_session() { 
    // Ensure we are connected
    connect();
    return session; 
  }

  void setOption(enum ssh_options_e option, void const *x) {
    ::ssh_options_set(session, option, x);
  }

  void connect() {
    if (!connected) {
      auto rc = ssh_connect(session);
      if (rc != SSH_OK) {
        throw io_error("Error connecting" + std::string(ssh_get_error(session)));
      }

      rc = ssh_is_server_known(session);
      if (rc != SSH_OK) {
        ssh_disconnect(session);
        throw io_error("Error connecting" + std::string(ssh_get_error(session)));
      }

      rc = ssh_userauth_publickey_auto(session, NULL, NULL);
      if(rc != SSH_AUTH_SUCCESS) {
        ssh_disconnect(session);
        throw io_error("Error connecting" + std::string(ssh_get_error(session)));
      }
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

class SSHChannel {
  ssh_channel channel;
public:
  ptr<SSHSession> session;

  operator ssh_channel() { return channel; }

  SSHChannel(ptr<SSHSession> const & session) : session(session) {
    std::lock_guard<std::mutex> _lock(session->mutex);
    
    channel = ssh_channel_new(*session);
    if (channel == NULL) {
      throw io_error(fmt::format("Cannot create the channel: {}", ssh_get_error(*session)));
    }

    auto rc = ssh_channel_open_session(channel);
    if (rc != SSH_OK) {
      ssh_channel_free(channel);
      throw io_error(fmt::format("Cannot open the channel: {}", ssh_get_error(*session)));
    }
 
    // rc = ssh_channel_request_shell(channel);
    // if (rc != SSH_OK) {
    //   this->~SSHChannel();
    //   throw io_error(fmt::format("Cannot create request a shell: {}", ssh_get_error(session)));
    // }
    LOGGER->info("Opened channel {}", (void*)channel);

  }

  void exec(std::string const & command) {
    LOGGER->info("Request exec on channel {}", (void*)channel);

    auto rc = ssh_channel_request_exec(channel, command.c_str());
    if (rc != SSH_OK) {
      throw io_error(fmt::format("Cannot create request exec: {}", ssh_get_error(*session)));
    }
  }

  ~SSHChannel() {
    LOGGER->info("Closing channel {}", (void*)channel);
    ssh_channel_close(channel);
    ssh_channel_free(channel);
  }
};


// --- SSH process builder

class SSHProcess : public Process {
  ptr<SSHChannel> channel;
  std::thread stdout_thread, stderr_thread;

public:
  SSHProcess(ProcessBuilder &builder, ptr<SSHChannel> const & channel) : channel(channel) {
    auto readloop = [this](PipeFunction f, int is_stderr) {
      std::array<char, 256> buffer;
      //ssize_t nbytes;
      while (true) {
        std::lock_guard<std::mutex> _lock(this->channel->session->mutex);
        auto nbytes = ssh_channel_read_timeout(*this->channel, buffer.data(), buffer.size(), is_stderr, 100);
        if (nbytes == SSH_AGAIN) continue;
        if (nbytes == 0) break;
        f(buffer.data(), static_cast<size_t>(nbytes));
      }
    };

    if (builder.stdout.type == Redirection::PIPE) {
      stdout_thread = std::thread([readloop, &builder]() { readloop(builder.stdout.function, 0); });
    }
    if (builder.stderr.type == Redirection::PIPE) {
      stderr_thread = std::thread([readloop, &builder]() { readloop(builder.stderr.function, 1); });
    } 
  }

  ~SSHProcess() {
    kill(true);
    wait();
  }
  
  virtual long write(void * s, long count) override {
    if (channel) {
      std::lock_guard<std::mutex> _lock(this->channel->session->mutex);
      return ssh_channel_write(*channel, s, count);
    }
    return -1;
  }

  virtual void eof() override {
    if (channel) {
      std::lock_guard<std::mutex> _lock(this->channel->session->mutex);
      LOGGER->info("Sending EOF for channel", (void*)*channel);
      if (ssh_channel_is_open(*channel)) {
        ssh_channel_send_eof(*channel);
      }
    }
  }

  /// isRunning
  virtual bool isRunning() override { 
    NOT_IMPLEMENTED(); 
  }

  /// Wait for stderr/stdout to finish
  void wait() {
    if (stdout_thread.joinable())
      stdout_thread.join();
    if (stderr_thread.joinable())
      stderr_thread.join();
  }

  /// Exit code
  virtual int exitCode() override { 
    eof();
    wait();

    std::lock_guard<std::mutex> _lock(this->channel->session->mutex);
    auto code = ssh_channel_get_exit_status(*channel);
    LOGGER->info("Exit code is {}", code);
    return code;
  }

  /// Kill
  virtual void kill(bool force) override { 
    channel = nullptr;
  }



};



// ---- Process builder

class SSHProcessBuilder : public ProcessBuilder {
  std::shared_ptr<SSHSession> _session;

public:
  SSHProcessBuilder(std::shared_ptr<SSHSession> const &session)
      : _session(session) {}

  void redirect(std::ostringstream & oss, std::string const &s, Redirect const & r) {
    switch(r.type) {
      case Redirection::PIPE:
      case Redirection::INHERIT:
        break;
      case Redirection::FILE:
        oss << s << " " << ShScriptBuilder::protect_quoted(r.path);
        break;
      case Redirection::NONE:
        oss << s << " /dev/null";
        break;
    }
  }

  std::shared_ptr<Process> start() { 
    auto channel = mkptr<SSHChannel>(_session);
    std::ostringstream oss;

    // Set the environment (do not use SSH since they are filtered)
    for (auto & x : environment) {
        oss << "export " << x.first << "=\""
          << ShScriptBuilder::protect_quoted(x.second)
          << "\"; ";
    }

    for(auto const & c: command) {
      oss << "\"" << ShScriptBuilder::protect_quoted(c) << "\" ";
    }

    redirect(oss, "<", stdin);
    redirect(oss, ">", stdout);
    redirect(oss, "2>", stderr);

    channel->exec(oss.str());
    auto process = std::make_shared<SSHProcess>(*this, channel);

    return process;
  }

};

// --- SSH connector

template <class T, class U> std::shared_ptr<T> shared_this(U *ptr) {
  return std::dynamic_pointer_cast<T>(ptr->shared_from_this());
}
template <class T, class U> std::shared_ptr<T> shared_this(U const *ptr) {
  return std::dynamic_pointer_cast<T>(const_cast<U *>(ptr)->shared_from_this());
}





// ---- Streams


template <class cT, class traits = std::char_traits<cT>>
class sftpstreambuf : public std::basic_streambuf<cT, traits> {
  std::array<typename traits::char_type, 1024> buffer;
  sftp_file file;
  size_t offset = 0;
  ptr<SSHSession> session;
  SFTPSession sftp;

  bool flush() {
    auto size = sizeof(typename traits::char_type) * offset;
    auto wsize = sftp_write(file, (void *)buffer.data(), size);
    offset = 0;
    LOGGER->debug("Wrote {} bytes to file ({})", wsize, size);
    if (wsize < 0) {
      throw io_error(fmt::format("Could not write in file: {}",
                                 ssh_get_error(sftp.session)));
    }
    return wsize == size;
  }

public:
  sftpstreambuf(ptr<SSHSession> const &session, const std::string &path, int access_type,
                mode_t mode) : session(session), sftp(*session) {
    file = sftp_open(sftp.sftp, path.c_str(), access_type, mode);
    if (file == NULL) {
      throw io_error(fmt::format("Can't open file for writing: {}",
                                 ssh_get_error(sftp.session)));
    }
  }

  ~sftpstreambuf() {
    flush();
    sftp_close(file);
  }

  typename traits::int_type overflow(typename traits::int_type c) {
    if (!traits::eq_int_type(c, traits::eof())) {
      if (offset == buffer.max_size()) {
        if (!flush())
          return traits::eof();
      }
      buffer[offset++] = c;
    }
    return traits::not_eof(c); // indicate success
  }
};

template <class cT, class traits = std::char_traits<cT>>
class osftpstream : public std::basic_ostream<cT, traits> {
public:
  osftpstream(ptr<SSHSession> const & session, const std::string &path, int access_type,
              mode_t mode)
      : std::basic_ios<cT, traits>(), std::basic_ostream<cT, traits>(0),
        m_sbuf(session, path, access_type, mode) {
    this->init(&m_sbuf);
  }

private:
  sftpstreambuf<cT, traits> m_sbuf;
};

// --- SSH Connector



SSHConnector::SSHConnector(std::string const &s) :
  _session(mkptr<SSHSession>()) {
    static const std::regex RE_SSHURI(R"((?:(\w+)@)?(\w+)(?::(\d+))?)");
    std::cmatch m;
    if (!std::regex_match (s.c_str(), m, RE_SSHURI)) {
      throw argument_error("Cannot parse SSH URI " + s);
    }

    if (m[1].matched) {
      _session->username(m[1].str());
    }
    _session->host(m[2].str());
    if (m[3].matched) {
      int port = std::atoi(m[3].str().c_str());
      _session->port(port);
    }
}

SSHConnector::~SSHConnector() {}

SSHConnector & SSHConnector::addIdentity(std::string const & localpath) {
  _session->setOption(SSH_OPTIONS_ADD_IDENTITY, localpath.c_str());
  return *this;
}

std::unique_ptr<Lock> SSHConnector::lock(Path const &path) const {
  NOT_IMPLEMENTED();  
}

std::shared_ptr<ProcessBuilder> SSHConnector::processBuilder() const {
  return std::make_shared<SSHProcessBuilder>(_session);
}

void SSHConnector::setExecutable(Path const &path, bool flag) const {
  std::string localpath = resolve(path);
  _session->connect();
  SFTPSession sftp(*_session);
  sftp.chmod(localpath, S_IRWXU);
}

void SSHConnector::mkdir(Path const &path) const {
  std::string localpath = resolve(path);
  SFTPSession sftp(*_session);
  sftp.mkdir(localpath);
}

FileType SSHConnector::fileType(Path const &path) const { NOT_IMPLEMENTED(); }


std::unique_ptr<std::ostream> SSHConnector::ostream(Path const &path) const {
  return std::unique_ptr<std::ostream>(new osftpstream<char>(_session, resolve(path), O_CREAT | O_WRONLY | O_TRUNC, S_IRUSR | S_IWUSR));
}

std::unique_ptr<std::istream> SSHConnector::istream(Path const &path) const {
  NOT_IMPLEMENTED();
}

void SSHConnector::createFile(Path const &path, bool errorIfExists) const {
  auto out = std::unique_ptr<std::ostream>(new osftpstream<char>(_session, resolve(path), O_CREAT, S_IRUSR | S_IWUSR));
}

void SSHConnector::remove(Path const &path, bool recursive) const {
  std::string localpath = resolve(path);
  SFTPSession sftp(*_session);

  NOT_IMPLEMENTED();
}


} // namespace xpm
