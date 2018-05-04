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
   *  Write to stdin
   * @return true if the write succeeded, false otherwise
   */
  virtual bool writeStdin(std::string const & string) = 0;
};

/**
 * A process builder.
 * 
 * Paths are local to the running machine
 */
class ProcessBuilder {
public:
  virtual ~ProcessBuilder();
  virtual ptr<Process> start() = 0;

  std::string workingDirectory;
  Redirect stdin;
  Redirect stdout;
  Redirect stderr;
  Environment environment;
  std::vector<std::string> command;
};



/** Access to a host and command line process */
class Connector {
public:
  virtual  ~Connector();

  /** Returns a new process builder */
  virtual ptr<ProcessBuilder> processBuilder() const = 0;

  /** Resolve a path so it is relative to the connector */
  virtual std::string resolve(Path const & path) const = 0;

  /** 
   * Resolve a path so it is relative to the other path on the connector 
   * @param path The path to resolve
   * @param base The base path for relative 
  */
  std::string resolve(Path const & path, Path const & base) const;

  /** Marks the file as executable (or not) */
  virtual void setExecutable(Path const & path, bool flag) const = 0;

  /** Get an output stream */
  virtual std::unique_ptr<std::ostream> ostream(Path const & path) const = 0;

  /** Get an output stream */
  virtual std::unique_ptr<std::istream> istream(Path const & path) const = 0;
};


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
  virtual ptr<ScriptBuilder> scriptBuilder() = 0;

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