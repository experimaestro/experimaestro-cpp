/**
 * This file contains helpers for c++ code
 */

#ifndef EXPERIMAESTRO_CPP_HPP
#define EXPERIMAESTRO_CPP_HPP

#include "xpm.hpp"

namespace xpm {

struct _ArgumentHolder {

};

template<typename T>
struct ArgumentHolder : public _ArgumentHolder {
  std::shared_ptr<Argument> argument;
  T value;

  ArgumentHolder() {}
  ArgumentHolder(const char *name) : argument(std::make_shared<Argument>(name)) {
  }

  ArgumentHolder required(bool _required) {
    argument->required(_required);
    return *this;
  }
};

struct TypeBuilder {
  std::shared_ptr<Type> type;
  TypeBuilder(std::string const &tn) : type(std::make_shared<Type>(TypeName(tn))) {}

  void add() {}

  template<typename T, typename ...Ts>
  void add(std::string const &name, ArgumentHolder<T> a, Ts... args) {
    a.argument->name(name);
    type->addArgument(a.argument);
    add(args...);
  }
};

#define XPMTYPE(name, ...) \
  TypeBuilder __register() { TypeBuilder tb(name); tb.add(__VA_ARGS__); return tb; };

/**
 * Add a type
 * @tparam T The type to analyze
 * @param r
 */
template<typename T>
void addType(Register &r) {
  T t;
  r.addType(t.__register().type);
}

struct CppObject : public Object {
  std::unordered_map<std::string, _ArgumentHolder *> _arguments;

  void setValue(std::string const &name, std::shared_ptr<Object> const &value) override {
  }
};
}


#endif //PROJECT_CPP_HPP
