#ifndef EXPERIMAESTRO_CONNECTORS_HPP
#define EXPERIMAESTRO_CONNECTORS_HPP

#include <chrono>
#include <map>

#include <xpm/common.hpp>
#include <xpm/filesystem.hpp>

namespace xpm {

class ProcessBuilder;
class Connector;

enum struct FileType { UNEXISTING, FILE, DIRECTORY, PIPE, OTHER };

/**
 * Generic lock
 * 
 * Releases the lock when deleted (unless detached)
 */
class Lock {
protected:
  bool _detached;
public:
  Lock();
  virtual ~Lock();

  /// Set the detach state
  void detachState(bool state);

  /// Set the detach state
  inline bool detached() const { return _detached; };
};

/// A lock represented by the presence of a file
class FileLock : public Lock {
  std::shared_ptr<Connector> _connector;
  Path _path;
public:
  FileLock(std::shared_ptr<Connector> const & connector, Path const & path);
  virtual ~FileLock();
};


/** Access to a host and command line process */
class Connector : public std::enable_shared_from_this<Connector> {
  struct Mapping {
    std::string serverpath;
    std::string localpath;
  };

  std::map<std::string, std::vector<Mapping>> _mounts;
public:
  virtual ~Connector();

  /** Returns a new process builder */
  virtual std::shared_ptr<ProcessBuilder> processBuilder() const = 0;

  /** Resolve a path so it is relative to the connector */
  std::string resolve(Path const &path) const;

  /**
   * Resolve a path so it is relative to the other path on the connector
   * @param path The path to resolve
   * @param base The base path for relative
   */
  std::string resolve(Path const &path, Path const &base) const;

  /** Marks the file as executable (or not) */
  virtual void setExecutable(Path const &path, bool flag) const = 0;

  /**
   * Make directories
   * @param createParents if parents should be created should they not exist
   * @param errorExists throw an error if the directory exists
   * @throws ioexception If an error occurs
   */
  void mkdirs(Path const &path, bool createParents = false,
                      bool errorExists = false) const;

  /**
   * Make one directory
   * @throws ioexception If the directory exists or cannot be created
   */
  virtual void mkdir(Path const &path) const = 0;

  /**
   * Returns file type
   * @throws ioexception If an error occurs
   */
  virtual FileType fileType(Path const &path) const = 0;

  /**
   * Create a file
   */
  virtual void createFile(Path const &path, bool errorIfExists = false) const = 0;

  /**
   * Create a lock file
   */
  virtual std::unique_ptr<Lock> lock(Path const &path, std::chrono::seconds const &) const = 0;

  /**
   * Delete a file / directory
   */
  virtual void remove(Path const &path, bool recursive=false) const = 0;

  /** Get an output stream */
  virtual std::unique_ptr<std::ostream> ostream(Path const &path) const = 0;

  /** Get an output stream */
  virtual std::unique_ptr<std::istream> istream(Path const &path) const = 0;
};

} // namespace xpm

#endif 