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
  virtual std::shared_ptr<Process> start() = 0;

  std::string workingDirectory;
  Redirect stdin;
  Redirect stdout;
  Redirect stderr;
  Environment environment;
  std::vector<std::string> command;
};

enum struct FileType {
  UNEXISTING,
  FILE,
  DIRECTORY,
  PIPE,
  OTHER
};

/** Access to a host and command line process */
class Connector {
public:
  virtual  ~Connector();

  /** Returns a new process builder */
  virtual std::shared_ptr<ProcessBuilder> processBuilder() const = 0;

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

  /** 
   * Make directories 
   * @param createParents if parents should be created should they not exist
   * @param errorExists throw an error if the directory exists
   * @throws ioexception If an error occurs
   */
  virtual void mkdirs(Path const & path, bool createParents = false, bool errorExists = false) const = 0;

  /** 
   * Returns file type 
   * @throws ioexception If an error occurs
   */
  virtual FileType fileType(Path const & path) const = 0;

#ifndef SWIG
  /** Get an output stream */
  virtual std::unique_ptr<std::ostream> ostream(Path const & path) const = 0;

  /** Get an output stream */
  virtual std::unique_ptr<std::istream> istream(Path const & path) const = 0;
#endif
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