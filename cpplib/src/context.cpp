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

void Context::set(std::string const &key, std::string const &value) {
  _variables[key] = value;
}

std::string Context::get(std::string const &key) const {
  auto it = _variables.find(key);
  if (it != _variables.end()) {
    return it->second;
  }

  return "";
}
bool Context::has(std::string const &key) const {
  return _variables.find(key) != _variables.end();
}


// --- Global methods

void set_workdir(Path const &path) {
  Context::current().workdir(path);
}


}