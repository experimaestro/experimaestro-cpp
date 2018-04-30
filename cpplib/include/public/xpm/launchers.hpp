/** Abstract classes related to launching jobs.
 */
#ifndef EXPERIMAESTRO_LAUNCHERS_HPP
#define EXPERIMAESTRO_LAUNCHERS_HPP

#include <map>
#include <string>
#include <vector>
#include <memory>

namespace xpm {

typedef std::map<std::string, std::string> Environment;


/** Access to a host and command line process */
class Connector {
public:
  virtual  ~Connector();

  /** Returns a new process builder */
  virtual ProcessBuilder::Ptr processBuilder() = 0;

  typedef std::shared_ptr<Connector> Ptr;
};

/** Localhost connector */
class LocalConnector {};

/**
 * Base class for script builders
 */
class ScriptBuilder {};

/** Specifies a way to launch process.
 *
 * This can be directly or through a job manager like
 * OAR
 */
class Launcher {
public:
  Launcher(Connector::Ptr const &connector);
  virtual ~Launcher();
  virtual ProcessBuilder::Ptr processBuilder() = 0;

  inline Connector::Ptr connector() { return _connector; }
  inline Environment const & environment() { return _environment; }

private:
  /**
   * The notification URL
   */
  std::string _notificationURL;

  /**
   * The environment to set
   */
  Environment _environment;

  /**
   * The connecctor
   */
  Connector::Ptr _connector;
};

/** A direct launcher */
class DirectLauncher : public Launcher {
public:
  virtual ProcessBuilder::Ptr processBuilder() override;
};

} // namespace xpm

#endif