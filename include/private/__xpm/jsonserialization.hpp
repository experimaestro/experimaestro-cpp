//
// Created by Benjamin Piwowarski on 15/01/2017.
//

#ifndef PROJECT_JSONSERIALIZATION_HPP
#define PROJECT_JSONSERIALIZATION_HPP

#include <string>

namespace xpm {

class JsonSerializer {
  bool writer;
 public:
  JsonSerializer &object();

  template<typename T>
  JsonSerializer &operator()(std::string const &name, T &t) {
    if (writer) {

    } else {

    }

    return *this;
  }
};


}

#endif //PROJECT_JSONSERIALIZATION_HPP
