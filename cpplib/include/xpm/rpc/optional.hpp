//
// Created by Benjamin Piwowarski on 20/01/2017.
//

#ifndef EXPERIMAESTRO_OPTIONAL_HPP
#define EXPERIMAESTRO_OPTIONAL_HPP

namespace xpm {
namespace rpc {
/// Valeur optionnelle
template<typename T>
class optional {
  bool _set;
  T t;
 public:
  inline optional() : _set(false) {}

  inline optional(T const &t) : _set(true), t(t) {}

  ~optional() {
  }

  operator optional<T>() const {
    return *this ? optional<T>(t) : optional<T>();
  }

  T &operator*() {
    if (!_set) throw std::runtime_error("Optional value is not set");
    return t;
  }

  T const &operator*() const {
    if (!_set) throw std::runtime_error("Optional value is not set");
    return t;
  }

  T *operator->() {
    if (!_set) throw std::runtime_error("Optional value is not set");
    return &t;
  }

  T const *operator->() const {
    if (!_set) throw std::runtime_error("Optional value is not set");
    return &t;
  }

  operator bool() const {
    return _set;
  }
};
}
}
#endif //EXPERIMAESTRO_OPTIONAL_HPP
