/** Abstract classes related to launching jobs.
 */
#ifndef EXPERIMAESTRO_LAUNCHERS_HPP
#define EXPERIMAESTRO_LAUNCHERS_HPP

#include <iostream>
#include <map>
#include <string>
#include <vector>
#include <memory>
#include <functional>

#include <xpm/common.hpp>
#include <xpm/filesystem.hpp>

namespace xpm {

// Foward declarations
class ScriptBuilder;
class Connector;

// Type definitions
typedef std::map<std::string, std::string> Environment;


enum class Redirection {
  INHERIT, FILE, PIPE, NONE
};

typedef std::function<void(const char *bytes, size_t n)> PipeFunction;

/** Redirect specification */
struct Redirect {
  Redirection type;
  std::string path;
  PipeFunction function;

  static Redirect file(std::string const &path);
  static Redirect pipe(PipeFunction function);
  static Redirect none();
  static Redirect inherit();
  Redirect();
protected:
  Redirect(Redirection);
};


class Process {
public:
  /// Process
  virtual ~Process();

  /// isRunning
  virtual bool isRunning() = 0;

  /// Exit code 
  virtual int exitCode() = 0;

  /// Kill
  virtual void kill(bool force) = 0;

  /**
   * Write to standard input
   * @return The number of bytes written (or -1 if an error occurred)
   */
  virtual long write(void * s, long count) = 0;

  /**
   * Closes standard in
   */
  virtual void eof() = 0;
};

/**
 * A process builder.
 * 
 * Paths are local to the running machine
 */
class ProcessBuilder {
public:
  virtual ~ProcessBuilder();
  virtual std::shared_ptr<Process> start() = 0;

  std::string workingDirectory;

  /// Standard input redirection
  Redirect stdin;

  /// Standard output redirection
  Redirect stdout;

  /// Standard error redirection
  Redirect stderr;

  /// Should the process be detached
  bool detach;

  /// The environment
  Environment environment;

  /// The command to execute
  std::vector<std::string> command;
};


/** Specifies a way to launch process.
 *
 * This can be directly or through a job manager like
 * OAR
 */
class Launcher {
public:
  Launcher(std::shared_ptr<Connector> const &connector);
  virtual ~Launcher();
  virtual std::shared_ptr<ProcessBuilder> processBuilder() = 0;
  virtual std::shared_ptr<ScriptBuilder> scriptBuilder() = 0;

  inline std::shared_ptr<Connector> connector() { return _connector; }
  inline Environment const & environment() { return _environment; }

  /// Set the notification URL
  std::string const & notificationURL() const;
  void notificationURL(std::string const &);

  /// Get the default launcher
  static std::shared_ptr<Launcher> defaultLauncher();

private:
  static std::shared_ptr<Launcher> DEFAULT_LAUNCHER;

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
  std::shared_ptr<Connector> _connector;
};

/** A direct launcher */
class DirectLauncher : public Launcher {
public:
  DirectLauncher(std::shared_ptr<Connector> const &connector);
  virtual std::shared_ptr<ProcessBuilder> processBuilder() override;
  virtual std::shared_ptr<ScriptBuilder> scriptBuilder() override;
};

} // namespace xpm

#endif