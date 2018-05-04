//
// Created by Benjamin Piwowarski on 08/12/2016.
//

#include <regex>
#include <iostream>
#include <xpm/context.hpp>

namespace xpm {


ptr<Context> Context::CURRENT_CONTEXT(std::make_shared<Context>());

Context::Context() {}

ptr<Context> const & Context::current() {
  return CURRENT_CONTEXT;
}

/// Set the current context
void Context::current(ptr<Context> const & context) {
  CURRENT_CONTEXT = context;
}

Path const Context::workdir() const {
  return basepath;
}

void Context::workdir(Path const&path) {
  basepath = path;
}

void Context::set(std::string const &key, std::string const &value) {
  _variables[key] = value;
}


void Context::set(std::string const &ns, std::string const &key, std::string const &value) {
  _variables[ns + "." + key] = value;
}

std::map<std::string,std::string>::const_iterator Context::find(std::string const &key) const {
  size_t last_dot = key.rfind('.');
  std::string name = key.substr(last_dot == std::string::npos ? 0 : last_dot + 1);

  for (;last_dot != std::string::npos; last_dot = key.rfind('.', last_dot - 1)) {
    // Get the key
    std::string _key = last_dot == std::string::npos ? 
        name : key.substr(0, last_dot) + "." + name;

    auto it = _variables.find(_key);
    if (it != _variables.end()) {
      return it;
    }
  }

  // Search for name
  return _variables.find(name);
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
  Context::current()->workdir(path);
}


}
