/**
 * This header contains methods and classes related to the file system access.
 * They are linked to the Experimaestro server
 */

#ifndef XPM_FILESYSTEM_H
#define XPM_FILESYSTEM_H

#include <memory>
#include <string>
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
  Path(std::string const &pathstring);
  Path();

  Path(std::string const &share, std::string const &node, std::string const &path);

  /// Construct relatively to a path
  Path(Path const &parent, std::string &relativePath);

  /// Returns the parent path
  Path parent() const;

  /// Returns a string representation of the path (that can be parsed)
  std::string toString() const;
};

}

#endif //PROJECT_FILESYSTEM_H
