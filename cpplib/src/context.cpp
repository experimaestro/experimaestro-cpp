//
// Created by Benjamin Piwowarski on 08/12/2016.
//

#include <xpm/context.hpp>

namespace xpm {

template<>
struct Reference<Context> {
  Path basepath;
};

Context Context::CURRENT_CONTEXT(std::make_shared<Reference<Context>>());

Context &Context::current() {
  return CURRENT_CONTEXT;
}

/// Set the current context
void Context::current(Context &&context) {
  CURRENT_CONTEXT = context;
}

Context::Context(ThisPtr const &ptr) : Pimpl(ptr) {
}

Path const Context::workdir() const {
  return _this->basepath;
}

void Context::workdir(Path const&path) {
  _this->basepath = path;
}


// --- Global methods

void set_workdir(Path const &path) {
  Context::current().workdir(path);
}


}