//
// Created by Benjamin Piwowarski on 08/12/2016.
//

#include <xpm/Context.h>

namespace xpm {

class _Context {
  Path basepath;
};

namespace {
  Context CURRENT_CONTEXT(new _Context());
}

Context &Context::current() {

}

/// Set the current context
void Context::current(Context &&context) {
  CURRENT_CONTEXT = context;
}

/// Get the basepath
Path const basepath() const {
  return _this->basepath;
}

  /// Sets the base path
void basepath(Path &&path) {
  _this->basepath = path;
}


}