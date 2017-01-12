//
// Created by Benjamin Piwowarski on 12/01/2017.
//

#include <string>
#include <xpm/xpm.hpp>
#include <gtest/gtest.h>

namespace xpm {

struct None {};
template<typename T>
struct ArgumentHolder {
  std::shared_ptr<Argument> argument;

  ArgumentHolder() {}
  ArgumentHolder(const char *name) : argument(std::make_shared<Argument>(name)) {
  }

  ArgumentHolder required(bool _required) {
    argument->required(_required);
    return *this;
  }

  ArgumentHolder(ArgumentHolder<None> const &other) : argument(std::move(other.argument)) {}
};

ArgumentHolder<None> operator "" _arg(const char *name, size_t len) {
  return ArgumentHolder<None>(name);
}

struct TypeBuilder {
  std::shared_ptr<Type> type;
  TypeBuilder(std::string const &tn) : type(std::make_shared<Type>(TypeName(tn))) {}

  void add() {}

  template<typename T, typename ...Ts>
  void add(std::string const &name, ArgumentHolder<T> a, Ts... args) {
    a.argument->name(name);
    type->addArgument(a.argument);
    add(args...); //std::forward<Ts>(args)...);
  }
};

#define XPMTYPE(name, ...) \
  TypeBuilder __register() { TypeBuilder tb(name); tb.add(__VA_ARGS__); return tb; }; \
  static const std::shared_ptr<Type> TYPE = nullptr;

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
}

using namespace xpm;

struct TypeA : public Object {
  ArgumentHolder<std::string> name;
  ArgumentHolder<int> x;

  XPMTYPE("TypeA", "name", name.required(true), "x", x);

  TypeA() {
  }
};

struct TypeB : public Object {
  ArgumentHolder<TypeA> b;
  XPMTYPE("TypeB", "b", b.required(true));
};

TEST(CppInterface, basic) {
  Register r;
  auto o = r.build(R"({ "type": "TypeB" })");
}