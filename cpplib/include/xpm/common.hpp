//
// Created by Benjamin Piwowarski on 12/12/2016.
//

#ifndef PROJECT_COMMON_HPP
#define PROJECT_COMMON_HPP

#include <string>
#include <exception>

namespace xpm {

/** Base exception */
class exception : public std::exception {
  std::string _message;
 public:
  exception() {}
  exception(std::string const &message) : _message(message) {}

  virtual const char *what() const noexcept override {
    return _message.c_str();
  }
};

/** Thrown when trying to modify a sealed value */
class sealed_error : public exception {
 public:
  sealed_error();
};

/** Thrown when the argument is invalid */
class argument_error : public exception {
 public:
  argument_error(std::string const &message);
};

}

#endif //PROJECT_COMMON_HPP
