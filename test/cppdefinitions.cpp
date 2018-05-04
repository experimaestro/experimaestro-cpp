//
// Created by Benjamin Piwowarski on 12/01/2017.
//

#include <string>
#include <typeindex>

#include <xpm/xpm.hpp>
#include <gtest/gtest.h>
#include <xpm/common.hpp>
#include <xpm/cpp.hpp>

#include <cxxabi.h>
std::string demangle(std::type_info const &ti) {
  int status;
  return std::unique_ptr<char>(abi::__cxa_demangle(ti.name(), 0, 0, &status)).get();
}

template<typename T, typename U>
::testing::AssertionResult AssertType(const char *m_expr, U *ptr) {
  if (ptr == nullptr) {
    return ::testing::AssertionFailure()
        << "Expected type of object to be [" << demangle(typeid(T)) << "], but got a nullptr";
  }

  if (typeid(*ptr) == typeid(T))
    return ::testing::AssertionSuccess();

  return ::testing::AssertionFailure()
      << "Type of object is not [" << demangle(typeid(T)) << "] but [" << demangle(typeid(*ptr)) << "]";
}

using namespace xpm;


struct TypeA : public CppObject<TypeA> {
  std::string name;
  int x = 0;
  Path path;
};

XPM_TYPE("TypeA", TypeA)
    .argument("name", &TypeA::name).required(true)
    .argument("x", &TypeA::x).defaultValue(1)
    .argument("path", &TypeA::path).generator(std::make_shared<PathGenerator>());

struct TypeB : public CppObject<TypeB> {
  std::shared_ptr<TypeA> a;
};
XPM_TYPE("TypeB", TypeB)
  .argument("a", &TypeB::a);

struct TypeB1 : public CppObject<TypeB1, TypeB> {
  long x;


};
XPM_SUBTYPE("TypeB1", TypeB1, TypeB)
  .argument("x", &TypeB1::x);


XPM_SIMPLETASK("task.b1", TypeB1);

// --- Tests

TEST(CppInterface, missingArgument) {
  auto o = currentRegister()->build(R"({ "$type": "TypeA" })");
  ASSERT_THROW(o->validate(true), xpm::argument_error);
}

TEST(CppInterface, basic) {
  auto o = currentRegister()->build(
      R"({ "$type": "TypeA", "name": "a name", "x": 1 })"
  );
  o->validate(true);

  TypeA &a = dynamic_cast<TypeA&>(*o);
  ASSERT_EQ(a.name, "a name");
  ASSERT_EQ(a.x, 1);
}

TEST(CppInterface, composed) {
  auto o = currentRegister()->build(R"({ "$type": "TypeB1", "x": 1, "a": { "$type": "TypeA", "x": 1 } })");

  EXPECT_PRED_FORMAT1(AssertType<TypeB1>, o.get());
  auto & b1 = dynamic_cast<TypeB1&>(*o);
  EXPECT_PRED_FORMAT1(AssertType<TypeA>, b1.a.get());

  ASSERT_EQ(b1.a->x, 1);

  o->validate(true);
}

