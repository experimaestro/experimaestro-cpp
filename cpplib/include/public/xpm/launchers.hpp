/** Abstract classes related to launching jobs.
 */
#ifndef EXPERIMAESTRO_LAUNCHERS_HPP
#define EXPERIMAESTRO_LAUNCHERS_HPP

#include <map>
#include <string>
#include <vector>
#include <memory>

#include <xpm/common.hpp>

namespace xpm {

typedef std::map<std::string, std::string> Environment;

class ProcessBuilder {
public:
  ~ProcessBuilder();
  void environment(Environment const & environment);
  void command(std::vector<std::string> const & command);
  virtual void start() = 0;
private:
  Environment _environment;
  std::vector<std::string> _command;
};

/** Access to a host and command line process */
class Connector {
public:
  virtual  ~Connector();

  /** Returns a new process builder */
  virtual ptr<ProcessBuilder> processBuilder() = 0;
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
  Launcher(ptr<Connector> const &connector);
  virtual ~Launcher();
  virtual ptr<ProcessBuilder> processBuilder() = 0;

  inline ptr<Connector> connector() { return _connector; }
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
  ptr<Connector> _connector;
};

/** A direct launcher */
class DirectLauncher : public Launcher {
public:
  virtual ptr<ProcessBuilder> processBuilder() override;
};

} // namespace xpm

#endif