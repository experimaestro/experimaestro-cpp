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
  int     status;
  return std::unique_ptr<char>(abi::__cxa_demangle(ti.name(), 0, 0, &status)).get();
}

template<typename T, typename U>
::testing::AssertionResult AssertType(const char* m_expr, U *ptr) {
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

// --- Type and tasks definitions
namespace xpm {

template<typename T>
struct CppType : public Object {
  typedef T Type;
  static RegisterType<T> __XPM_REGISTERTYPE;
  static std::unordered_map<std::string, _ArgumentHolder Type::*> _arguments;

  template<typename U>
  static Argument& argument(std::string const &name, U Type::* u) {
    static std::shared_ptr<Argument> arg = std::make_shared<Argument>();
    return *arg;
  }

  void setValue(std::string const &name, std::shared_ptr<Object> const &value) override {
  }
};

struct IntConverter {
};
int operator <<(IntConverter const &ic, Argument const &a) {
  return 0;
}
}

#define XPM_ARGUMENT(TYPENAME, VARNAME) \
  TYPENAME VARNAME; \
  static const int __ ## VARNAME = xpm::IntConverter() << XPM_TYPE::argument(#VARNAME, &XPM_TYPE::Type::VARNAME)

XPM_TYPE_CLASS(TypeA) {
  XPM_DECLARE_TYPE("TypeA", TypeA);

  XPM_ARGUMENT(std::string, name).required(true);
  XPM_ARGUMENT(int, x).required(true);
};

XPM_TYPE_CLASS(TypeB) {
  XPM_DECLARE_TYPE("TypeB");
  ArgumentHolder<TypeA> a;
  XPM_ARGUMENTS("a", a.required(true));
};


//XPM_SUBTYPE_CLASS TypeB1 : public TypeB {
//  ArgumentHolder<int> x;
//  XPM_ARGUMENTS("x", x.required(true));
//};
//XPM_CHILDTYPE(TypeB, "TypeB1", TypeB1);
//XPM_TASK("task", TypeB1);


// --- Tests

TEST(CppInterface, missingArgument) {
  auto o = CURRENT_REGISTER->build(R"({ "$type": "TypeB1" })");
  ASSERT_THROW(o->validate(), xpm::argument_error);
}


TEST(CppInterface, basic) {
  auto o = CURRENT_REGISTER->build(R"({ "$type": "TypeB1", "x": 1, "a": { "$type": "TypeA" } })");

//  EXPECT_PRED_FORMAT1(AssertType<TypeB1>, o.get());
//  auto & b1 = dynamic_cast<TypeB1&>(*o);
//
//  EXPECT_PRED_FORMAT1(AssertType<TypeA>, (*b1.a).get());
//
//  o->validate();
}

