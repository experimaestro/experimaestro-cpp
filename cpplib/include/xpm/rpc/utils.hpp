//
// Created by Benjamin Piwowarski on 29/11/2016.
//

#ifndef XPM_UTILS_HPP
#define XPM_UTILS_HPP

#include <xpm/json.hpp>
#include <xpm/rpc/optional.hpp>

namespace xpm {
namespace rpc {
template<typename T>
struct RPCConverter;

typedef int64_t ObjectIdentifierType;

struct ObjectIdentifier {
  ObjectIdentifierType id;
  inline explicit ObjectIdentifier(ObjectIdentifierType id) : id(id) {
  }
};

class ServerObject {
 public:
  typedef nlohmann::json json;

 protected:
  static std::shared_ptr<ServerObject> OBJECTS;

  ServerObject();
  virtual ~ServerObject();

  virtual std::string const &__name__() const = 0;

  ObjectIdentifierType _identifier;
  void __set__(json const &params);
  json __call__(std::string const &name, json &params);
  static json __static_call__(std::string const &name, json const &params);
  explicit ServerObject(ObjectIdentifier o);

 public:
  inline ObjectIdentifierType identifier() const { return _identifier; };

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
  static inline nlohmann::json toJson(std::shared_ptr<T> const &x) {
    return x ? x->_identifier : -1;
  }
  static inline std::shared_ptr<T> toCPP(nlohmann::json const &x) {
    ObjectIdentifierType id = (ObjectIdentifierType) x;
    if (id >= 0) {
      return std::shared_ptr<T>(new T(ObjectIdentifier(id)));
    }
    return nullptr;
  }
};

template<typename T>
struct RPCConverter<optional<T>> {
static inline nlohmann::json toJson(optional<T> const &x) {
  return x ? nlohmann::json(*x) : nlohmann::json(nullptr);
}
static inline T toCPP(nlohmann::json const &x) {
  return x;
}
};

template<typename T>
struct RPCConverter<std::vector<T>> {
  static inline nlohmann::json toJson(std::vector<T> const &list) {
    nlohmann::json array = nlohmann::json::array();
    for (auto &el: list) {
      array.push_back(RPCConverter<T>::toJson(el));
    }
    return array;
  }
  static inline std::vector<T> toCPP(nlohmann::json const &x) {
    std::vector<T> vector;
    vector.reserve(x.size());
    for (nlohmann::json::const_iterator it = x.begin(); it != x.end(); ++it) {
      vector.push_back(RPCConverter<T>::toCPP(x));
    }
    return vector;
  }
};

}
}
#endif //XPM_UTILS_HPP
