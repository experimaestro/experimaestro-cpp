//
// Created by Benjamin Piwowarski on 08/12/2016.
//

#ifndef PROJECT_UTILS_HPP
#define PROJECT_UTILS_HPP

#include <memory>
namespace xpm {
template<typename T>
struct Reference;

template<typename T>
class Pimpl {
 protected:
  typedef std::shared_ptr <Reference<T>> ThisPtr;
  friend struct Reference<T>;

  template<typename... _Args>
  static inline ThisPtr make_this(_Args&& ...__args) {
    return std::make_shared<Reference<T>>(std::forward<_Args>(__args)...);
  }

  ThisPtr _this;


  template<typename... _Args>
  Pimpl(_Args&& ...__args) :
      _this(std::make_shared<Reference<T>>(std::forward<_Args>(__args)...)) {
  }

  Pimpl(ThisPtr const &ptr) : _this(ptr) {
  }

};

}
#endif //PROJECT_UTILS_HPP
