//
// Created by Benjamin Piwowarski on 08/12/2016.
//

#ifndef PROJECT_CONTEXT_H
#define PROJECT_CONTEXT_H

#include <xpm/filesystem.hpp>

namespace xpm {

/**
 * Context when running tasks
 */
class Context : public Pimpl<Context> {
  Context(ThisPtr const &);
  static Context CURRENT_CONTEXT;
 public:
  /// Get the current context
  static Context &current();

  /// Set the current context
  static void current(Context &&context);

  /// Get the basepath
  Path const workdir() const;

  /// Sets the base path
  void workdir(Path const &path);
};

/** Sets the working directory for the current context
 *
 * @param path The path to the new working directory
 */
void set_workdir(Path const &path);

}

#endif //PROJECT_CONTEXT_H
