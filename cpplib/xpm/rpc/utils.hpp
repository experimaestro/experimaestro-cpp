//
// Created by Benjamin Piwowarski on 29/11/2016.
//

#ifndef PROJECT_UTILS_HPP
#define PROJECT_UTILS_HPP

#include <xpm/json.hpp>

namespace xpm {

class ServerObject {
  int64_t _identifier;
 public:
  typedef nlohmann::json json;

  json __call__(json const &params);
  static json __static_call__(json const &params);
};

template<typename T>
inline T cpp2rpc(T x) { return x; }

template<typename T>
T rpc2cpp(nlohmann::json const &t) { return t; }

}
#endif //PROJECT_UTILS_HPP
