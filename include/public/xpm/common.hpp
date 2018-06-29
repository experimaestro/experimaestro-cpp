//
// Created by Benjamin Piwowarski on 12/12/2016.
//

#ifndef EXPERIMAESTRO_COMMON_HPP
#define EXPERIMAESTRO_COMMON_HPP

#include <memory>
#include <string>
#include <exception>
#include <iostream>
#include <vector>

#ifndef SWIG
  #define NOSWIG(...) __VA_ARGS__
#else
  #define NOSWIG(x) 
#endif

namespace xpm {

// SHA-1 digest lenght
static const int DIGEST_LENGTH = 20;

/// Template 
template <typename T> using ptr = std::shared_ptr<T>;

/// Short function name to create new shared pointer
template<class T, class... Args> 
inline std::shared_ptr<T> mkptr(Args&&... args) { return std::make_shared<T>(std::forward<Args>(args)...); }

/// Base class for 
class Outputable {
protected:
  /// Output using a stream
  virtual void output(std::ostream &out) const;
  friend inline std::ostream & operator<<(std::ostream & out, Outputable const & t) {
    t.output(out);
    return out;
  }
};

/** Output a shared ptr by trying to dereferencing it */
template<class T>
std::ostream & operator<<(std::ostream & out, std::shared_ptr<T> const &t) {
  if (t) return out << *t;
  return out << "[null]";
}




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


/** Thrown when a parameter value is invalid */
class parameter_error : public exception {
public:
  parameter_error(std::string const &message);
  parameter_error &addPath(std::string const &argument);
  virtual const char *what() const noexcept override;
private:
  mutable std::string _fullmessage;
  std::vector<std::string> _path;
};

/** Thrown when an argument cannot be converted to a given type */
class cast_error : public exception {
 public:
  cast_error(std::string const &message);
};

/** Thrown when an argument cannot be converted to a given type */
class assertion_error : public exception {
 public:
  assertion_error(std::string const &message);
};

/** Thrown when something has not been implemented */
class not_implemented_error : public exception {
 public:
  not_implemented_error(std::string const &method, std::string const &file, int line);
};

/** Thrown if an I/O error occurs */
class io_error : public exception {
 public:
  io_error(std::string const &message);
};



#define NOT_IMPLEMENTED() throw not_implemented_error(__func__, __FILE__, __LINE__)

}


#ifndef SWIG

#include <cxxabi.h>
template<typename T>
std::string demangle(T const & t) {
  int status;
  char * demangled = abi::__cxa_demangle(typeid(t).name(),0,0,&status);
  std::string r = demangled;
  free(demangled);
  return r;
}

template<typename T>
std::string demangle(std::shared_ptr<T> const & t) {
  if (t) {
    return "shared_ptr of " + demangle(*t);
  } else {
    return "nullptr";
  }
}


#endif

#endif //PROJECT_COMMON_HPP
