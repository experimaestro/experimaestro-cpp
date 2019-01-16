//
// Created by Benjamin Piwowarski on 12/12/2016.
//

#ifndef EXPERIMAESTRO__COMMON_HPP
#define EXPERIMAESTRO__COMMON_HPP

#include <array>

#include <spdlog/spdlog.h>
#include <spdlog/fmt/ostr.h>
#include <Poco/SHA1Engine.h>
#include <xpm/common.hpp>

#define DEFINE_LOGGER(name) namespace { auto LOGGER = ::xpm::logger(name); }

namespace xpm {

std::shared_ptr<spdlog::logger> logger(std::string const &name);

class Value;
class Typename;
class Type;
class Path;

std::ostream &operator<<(std::ostream &os, const Typename &c);
std::ostream &operator<<(std::ostream &os, const Type &c);
std::ostream &operator<<(std::ostream &os, const Path &c);

struct Digest {
  Poco::SHA1Engine context;


  Digest() {
  }

  template<typename T>
  void updateDigest(T const &value) {
    static_assert(std::is_pod<T>::value, "Expected a POD value");
    context.update(&value, sizeof(T));
  }

  inline void updateDigest(std::string const &value) {
    context.update(value);
  }


  inline std::array<unsigned char, Poco::SHA1Engine::DIGEST_SIZE> get() {
    auto d = context.digest();
    if (d.size() != Poco::SHA1Engine::DIGEST_SIZE) {
      throw std::runtime_error("Error while retrieving SHA-1: digest size not maching real size");
    }
    
    std::array<unsigned char, Poco::SHA1Engine::DIGEST_SIZE> md;
    std::copy(d.begin(), d.end(), md.begin());
    return md;
  }
};

/// Allows to run commands when exiting a context  
class finally {
  std::function<void(void)> functor;
public:
  finally(const std::function<void(void)> &functor) : functor(functor) {}
  ~finally() {
    functor();
  }
};

}

#endif //PROJECT_PRIVATE_HPP
