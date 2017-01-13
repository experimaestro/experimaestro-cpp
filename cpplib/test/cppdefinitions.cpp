//
// Created by Benjamin Piwowarski on 12/01/2017.
//

#include <string>
#include <xpm/xpm.hpp>
#include <gtest/gtest.h>

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

using namespace xpm;

struct TypeA : public CppObject {
  ArgumentHolder<std::string> name;
  ArgumentHolder<int> x;

  XPMTYPE("TypeA", "name", name.required(true), "x", x);

  TypeA() {
  }

};

struct TypeB : public CppObject {
  ArgumentHolder<TypeA> a;
  XPMTYPE("TypeB", "a", a.required(true));
};

struct TypeB1 : public TypeB {
  ArgumentHolder<int> x;
  XPMTYPE("TypeB", "x", x.required(true));
};

TEST(CppInterface, basic) {
  Register r;
  auto o = r.build(R"({ "type": "TypeB1" })");

  o->validate();

}
