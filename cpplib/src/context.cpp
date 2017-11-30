//
// Created by Benjamin Piwowarski on 08/12/2016.
//

#include <regex>
#include <iostream>
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


void Context::set(std::string const &ns, std::string const &key, std::string const &value) {
  _variables[ns + "." + key] = value;
}

std::map<std::string,std::string>::const_iterator Context::find(std::string const &key) const {
  size_t last_dot = key.rfind('.');
  // Position of component
  size_t name_pos = last_dot == std::string::npos ? 0 : last_dot + 1;
  std::string name = key.substr(name_pos);

  do {
    // Get the key
    std::string _key = last_dot == std::string::npos ? 
      name : key.substr(0, last_dot) + "." + name;

    auto it = _variables.find(_key);
    if (it != _variables.end()) {
      return it;
    }

    last_dot = key.rfind('.', last_dot - 1);
  } while (last_dot != std::string::npos);

  return _variables.end();
}

std::string Context::get(std::string const &key) const {
  auto it = find(key);
  return it != _variables.end() ? it->second : "";
}

bool Context::has(std::string const &key) const {
  return find(key) != _variables.end();
}


// --- Global methods

void set_workdir(Path const &path) {
  Context::current().workdir(path);
}


}
