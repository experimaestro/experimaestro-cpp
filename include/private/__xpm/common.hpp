//
// Created by Benjamin Piwowarski on 12/12/2016.
//

#ifndef EXPERIMAESTRO__COMMON_HPP
#define EXPERIMAESTRO__COMMON_HPP

#include <array>

#include <spdlog/spdlog.h>
#include <spdlog/fmt/ostr.h>
#include <openssl/sha.h>

#include <xpm/common.hpp>

#define DEFINE_LOGGER(name) namespace { auto LOGGER = ::xpm::logger(name); }

namespace xpm {

std::shared_ptr<spdlog::logger> logger(std::string const &name);

class Parameters;
class Typename;
class Type;
class Path;

std::ostream &operator<<(std::ostream &os, const Typename &c);
std::ostream &operator<<(std::ostream &os, const Type &c);
std::ostream &operator<<(std::ostream &os, const Path &c);

struct Digest {
  SHA_CTX context;


  Digest() {
    if (!SHA1_Init(&context)) {
      throw std::runtime_error("Error while initializing SHA-1");
    }
  }

  template<typename T>
  void updateDigest(T const &value) {
    static_assert(std::is_pod<T>::value, "Expected a POD value");

    if (!SHA1_Update(&context, &value, sizeof(T))) {
      throw std::runtime_error("Error while computing SHA-1");
    }
  }

  inline void updateDigest(std::string const &value) {
    if (!SHA1_Update(&context, value.c_str(), value.size())) {
      throw std::runtime_error("Error while computing SHA-1");
    }
  }


  void updateDigest(Parameters const &value);

  inline std::array<unsigned char, SHA_DIGEST_LENGTH> get() {
    std::array<unsigned char, SHA_DIGEST_LENGTH> md;
    if (!SHA1_Final(md.data(), &context)) {
      throw std::runtime_error("Error while retrieving SHA-1");
    }
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
