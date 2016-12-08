//
// Created by Benjamin Piwowarski on 08/12/2016.
//

#ifndef PROJECT_CONTEXT_H
#define PROJECT_CONTEXT_H

#include <xpm/filesystem.h>

namespace xpm {

class _Context;

/**
 * Context when running tasks
 */
class Context {
  std::shared_ptr<_Context> _this;
 public:
  /// Get the current context
  static Context &current();

  /// Set the current context
  static void current(Context &&context);

  /// Get the basepath
  Path const basepath() const;

  /// Sets the base path
  void basepath(Path &&path);
};

}

#endif //PROJECT_CONTEXT_H
