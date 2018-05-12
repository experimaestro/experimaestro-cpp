#ifndef EXPERIMAESTRO_CONNECTORS_HPP
#define EXPERIMAESTRO_CONNECTORS_HPP

#include <xpm/common.hpp>
#include <xpm/filesystem.hpp>

namespace xpm {

class ProcessBuilder;

enum struct FileType { UNEXISTING, FILE, DIRECTORY, PIPE, OTHER };

/** Access to a host and command line process */
class Connector NOSWIG(: public std::enable_shared_from_this<Connector>) {
public:
  virtual ~Connector();

  /** Returns a new process builder */
  virtual std::shared_ptr<ProcessBuilder> processBuilder() const = 0;

  /** Resolve a path so it is relative to the connector */
  virtual std::string resolve(Path const &path) const = 0;

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
  virtual void mkdirs(Path const &path, bool createParents = false,
                      bool errorExists = false) const = 0;

  /**
   * Returns file type
   * @throws ioexception If an error occurs
   */
  virtual FileType fileType(Path const &path) const = 0;

#ifndef SWIG
  /** Get an output stream */
  virtual std::unique_ptr<std::ostream> ostream(Path const &path) const = 0;

  /** Get an output stream */
  virtual std::unique_ptr<std::istream> istream(Path const &path) const = 0;
#endif
};

} // namespace xpm

#endif