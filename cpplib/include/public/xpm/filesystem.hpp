/**
 * This header contains methods and classes related to the file system access.
 * They are linked to the Experimaestro server
 */

#ifndef XPM_FILESYSTEM_H
#define XPM_FILESYSTEM_H

#include <memory>
#include <string>
#include <vector>
#include <functional>
#include "utils.hpp"

namespace xpm {

/**
 * Represents a file system access point
 */
class Share : public Pimpl<Share> {
 public:
  Share(std::string const &host, std::string const &name);
};

/**
 * Represents a path
 */
class Path : public Pimpl<Path> {
 public:
  Path();

  /// Constructs a path from a string representation
  Path(std::string const &pathstring);

  /// Constructs a path from a triplet (share, node, path)
  Path(std::string const &share, std::string const &node, std::string const &path);

#ifndef SWIG
  Path(Path const &parent, std::initializer_list<std::string> const &relative);
#endif

  /// Construct relatively to a path
  Path(Path const &parent, std::vector<std::string> const &relative);

  /// Returns the parent path
  Path parent() const;

  /// Returns a string representation of the path (that can be parsed)
  std::string toString() const;

  /** Local path.
   * Returns a path on the current host
   * @throws std::logic_error If the path is on a shared host
   */
  std::string localpath() const;

  /**
   * Is local
   */
   bool isLocal() const;

  /**
   * Get file content
   */
   std::string getContent() const;

   /**
    * Equality
    */
  friend bool operator==(Path const & lhs, Path const & rhs);

  template<typename OStream>
  friend OStream& operator<<(OStream& os, const Path & path) {
    return os << path.toString();
  }
};

  bool operator==(Path const & lhs, Path const & rhs);

}

#ifndef SWIG
namespace std {
  template<>
  struct hash<xpm::Path> {
    size_t operator()(const xpm::Path & path) const {
      return hash<std::string>()(path.toString());
    }
  };
}
#endif

#endif //PROJECT_FILESYSTEM_H
