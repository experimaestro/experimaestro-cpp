/** Abstract classes related to launching jobs.
 */
#ifndef EXPERIMAESTRO_LAUNCHERS_HPP
#define EXPERIMAESTRO_LAUNCHERS_HPP

#include <map>
#include <string>
#include <vector>
#include <memory>
#include <functional>

#include <xpm/common.hpp>

namespace xpm {

typedef std::map<std::string, std::string> Environment;


/** base class for redirect */
struct Redirect {};

/** Redirect to/from a file */
struct RedirectFile : public Redirect {
  std::string path;
};

/** Redirect to/from the null stream */
struct RedirectNull : public Redirect {
};

/** Redirect to a callback */
struct RedirectPipe : public Redirect {
  std::function<void(const char *bytes, size_t n)> function;
};

class Process {
public:
  /// Process
  ~Process();

  /// isRunning
  virtual void isRunning() = 0;

  /// Exit code 
  virtual int exitCode() = 0;

  /// Kill
  virtual void kill() = 0;

  /// Write to stdin
  virtual void writeStdin(std::string const & string) = 0;
};

/**
 * A process builder.
 * 
 * Paths are local to the running machine
 */
class ProcessBuilder {
public:
  ~ProcessBuilder();

  /// Sets the command
  void command(std::vector<std::string> const & command);
  
  /// Sets the envionment
  void environment(Environment const & environment);

  /// Sets the working directory
  void workingDirectory(std::string const & workingDirectory);

  /// Sets the standard input
  void stdin(ptr<Redirect> const & redirect);

  /// Sets the standard output
  void stdout(ptr<Redirect> const & redirect);

  /// Sets the standard error
  void stderr(ptr<Redirect> const & redirect);

  /// Starts
  virtual std::shared_ptr<Process> start() = 0;

protected:
  Environment _environment;
  std::vector<std::string> _command;
  std::string _workingDirectory;
  std::shared_ptr<Redirect> _stdin;
  std::shared_ptr<Redirect> _stdout;
  std::shared_ptr<Redirect> _stderr;
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