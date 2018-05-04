//
// Created by Benjamin Piwowarski on 08/12/2016.
//

#ifndef PROJECT_CONTEXT_H
#define PROJECT_CONTEXT_H

#include <map>
#include <xpm/filesystem.hpp>
#include <xpm/common.hpp>

namespace xpm {

/**
 * Context when running tasks
 */
class Context {
public:
  Context(Context const &) = delete;
  Context();

  static ptr<Context> CURRENT_CONTEXT;
  std::map<std::string, std::string> _variables;
  Path basepath;

  /// Get an iterator for a key
#ifndef SWIG
  decltype(_variables)::const_iterator find(std::string const &key) const;
#endif
 public:
  /// Get the current context
  static ptr<Context> const &current();

  /// Set the current context
  static void current(ptr<Context> const & context);

  /// Get the basepath
  Path const workdir() const;

  /// Sets the base path
  void workdir(Path const &path);

  /// Sets a variable
  void set(std::string const &key, std::string const &value);

  /// Sets a variable with a ns
  void set(std::string const &ns, std::string const &key, std::string const &value);

  /// Gets a variable given a fully qualified name
  std::string get(std::string const &key) const;

  /// Checks if the variable exists
  bool has(std::string const &key) const;
};

/** Sets the working directory for the current context
 *
 * @param path The path to the new working directory
 */
void set_workdir(Path const &path);

}

#endif //PROJECT_CONTEXT_H
