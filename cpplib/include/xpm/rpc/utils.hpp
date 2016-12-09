//
// Created by Benjamin Piwowarski on 29/11/2016.
//

#ifndef XPM_UTILS_HPP
#define XPM_UTILS_HPP

#include <xpm/json.hpp>

namespace xpm {
namespace rpc {
template<typename T>
struct RPCConverter;

class ServerObject {
 public:
  typedef nlohmann::json json;

 protected:
  static std::shared_ptr<ServerObject> OBJECTS;

  ServerObject();
  virtual ~ServerObject();

  virtual std::string const &__name__() const = 0;

  int64_t _identifier;
  void __set__(json const &params);
  json __call__(std::string const &name, json &params);
  static json __static_call__(std::string const &name, json const &params);

 public:
  template<typename T> friend
  struct RPCConverter;
};

template<typename T>
struct RPCConverter {
  static inline nlohmann::json toJson(T x) { return x; }
  static inline T toCPP(nlohmann::json const &x) { return x; }
};

template<typename T>
struct RPCConverter<std::shared_ptr<T>> {
  static inline nlohmann::json toJson(std::shared_ptr<T> const &x) { return x->_identifier; }
  static inline std::shared_ptr<T> toCPP(nlohmann::json const &x) {
    return nullptr;
  }
};

}
}
#endif //XPM_UTILS_HPP
