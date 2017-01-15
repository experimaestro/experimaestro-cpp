//
// Created by Benjamin Piwowarski on 15/01/2017.
//

#include "jsonserialization.hpp"

namespace xpm {

struct A {
  std::string s;
  int x;

  void serialize(JsonSerializer &js) {
    js.object()
        ("s", s)
        ("x", x);
  }
};

JsonSerializer &JsonSerializer::object() {
  return *this;
}

}
