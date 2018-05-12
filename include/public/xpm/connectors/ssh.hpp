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
    ~SSHConnector();
    virtual std::shared_ptr<ProcessBuilder> processBuilder() const override;
    std::string resolve(Path const & path) const override;
    virtual void setExecutable(Path const & path, bool flag) const override;
    virtual void mkdirs(Path const & path, bool createParents = false, bool errorExists = false) const override;
    virtual FileType fileType(Path const & path) const override;

    std::unique_ptr<std::ostream> ostream(Path const & path) const override;
    std::unique_ptr<std::istream> istream(Path const & path) const override;

  };

}

#endif // EXPERIMAESTRO_XPMSSH_HPP
