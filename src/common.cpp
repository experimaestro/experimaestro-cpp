//
// Created by Benjamin Piwowarski on 13/01/2017.
//

#include <__xpm/common.hpp>
#include <xpm/xpm.hpp>

namespace xpm {


sealed_error::sealed_error() : exception("Object is sealed: cannot modify") {}
argument_error::argument_error(const std::string &message) : exception(message) {}
cast_error::cast_error(const std::string &message) : exception(message) {}
assertion_error::assertion_error(const std::string &message) : exception(message) {}
io_error::io_error(const std::string &message) : exception(message) {}
illegal_argument_error::illegal_argument_error(const std::string &message) : exception(message) {}
exited_error::exited_error() {}
lock_error::lock_error(const std::string &message) : exception(message) {}
not_implemented_error::not_implemented_error(const std::string &message,
                                             const std::string &file, int line) : exception(
    "Not implemented: " + message + ", file " + file + ":" + std::to_string(line)) {}


parameter_error::parameter_error(std::string const &message) : exception(message) {
}

parameter_error &parameter_error::addPath(std::string const &argument) {
  _path.push_back(argument);
  return *this;
}

const char *parameter_error::what() const noexcept {
  _fullmessage = std::string("Error with parameter ");

  bool first = true;
  for (auto it = _path.rbegin(); it != _path.rend(); ++it) 
    if (first){
      _fullmessage += *it;
      first = false;
    } else {
      _fullmessage += "." + *it;      
    }

  _fullmessage += ": ";
  _fullmessage += exception::what(); 
  return _fullmessage.c_str();
}


}
