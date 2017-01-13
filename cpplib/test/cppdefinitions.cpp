//
// Created by Benjamin Piwowarski on 12/01/2017.
//

#include <string>
#include <xpm/xpm.hpp>
#include <gtest/gtest.h>
#include <xpm/cpp.hpp>

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
