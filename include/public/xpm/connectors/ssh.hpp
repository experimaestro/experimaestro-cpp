//
// Created by Benjamin Piwowarski on 19/01/2017.
//

#ifndef EXPERIMAESTRO_CONNECTORS_SSH_HPP
#define EXPERIMAESTRO_CONNECTORS_SSH_HPP

#include <xpm/common.hpp>
#include <xpm/connectors/connectors.hpp>

namespace xpm {
  class SSHSession;

  /// An SSH session
  class SSHConnector : public Connector {
    std::shared_ptr<SSHSession> _session;
  public:
    /// URI has the format [user@]hostname[:port]
    SSHConnector(std::string const &uri);

    /// Set identity key
    SSHConnector & addIdentity(std::string const & localpath);

    ~SSHConnector();
    virtual std::shared_ptr<ProcessBuilder> processBuilder() const override;
    virtual ptr<Process> getProcess(pid_t pid) const override;
    virtual void setExecutable(Path const & path, bool flag) const override;
    virtual void mkdir(Path const & path) const override;
    virtual FileType fileType(Path const & path) const override;
    
    virtual void createFile(Path const &path, bool errorIfExists) const override;
    virtual void remove(Path const &path, bool recursive=false) const override;
    virtual std::unique_ptr<Lock> lock(Path const &path, std::chrono::seconds const &) const override;

    std::unique_ptr<std::ostream> ostream(Path const & path) const override;
    std::unique_ptr<std::istream> istream(Path const & path) const override;

  };

}

#endif // EXPERIMAESTRO_XPMSSH_HPP
