//
// Created by Benjamin Piwowarski on 12/12/2016.
//

#ifndef PROJECT_PRIVATE_HPP
#define PROJECT_PRIVATE_HPP

#include <array>

#include <spdlog/spdlog.h>
#include <spdlog/fmt/ostr.h>
#include <openssl/sha.h>

#define DEFINE_LOGGER(name) namespace { auto LOGGER = ::xpm::logger(name); }

namespace xpm {
std::shared_ptr<spdlog::logger> logger(std::string const &name);

class TypeName;
class Type;

std::ostream &operator<<(std::ostream &os, const TypeName &c);
std::ostream &operator<<(std::ostream &os, const Type &c);

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

  inline std::array<unsigned char, SHA_DIGEST_LENGTH> get() {
    std::array<unsigned char, SHA_DIGEST_LENGTH> md;
    if (!SHA1_Final(md.data(), &context)) {
      throw std::runtime_error("Error while retrieving SHA-1");
    }
    return md;
  }
};


}

#endif //PROJECT_PRIVATE_HPP
