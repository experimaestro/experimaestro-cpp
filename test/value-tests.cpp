//
// Created by Benjamin Piwowarski on 13/12/2016.
//

#include <gtest/gtest.h>

#include <xpm/xpm.hpp>
#include <xpm/type.hpp>
#include <xpm/workspace.hpp>
#include <xpm/cpp.hpp>

//using nlohmann::json;
using namespace xpm;

struct TestType {
  std::shared_ptr<Type> type;
  TestType() : type(mkptr<Type>(Typename("test"))) {
    auto a = mkptr<Argument>("a");
    a->defaultValue(mkptr<ScalarValue>(1l));
    type->addArgument(a);
  }

  ptr<MapValue> create() {
    auto ptr = mkptr<MapValue>();
    ptr->type(type);
    return ptr;
  }
};



TEST(Value, scalarParse) {
  auto r = mkptr<CppRegister>();
  EXPECT_THROW(r->build(R"({ "$value": 5, "shouldnotbehere": 2 })"), xpm::argument_error);
}


TEST(Value, defaultSet) {
  auto object = TestType().create();
  object->set("a", mkptr<ScalarValue>(1l));
  object->validate();

  EXPECT_TRUE(object->get("a")->equals(ScalarValue(1l)));
  EXPECT_TRUE(object->get("a")->isDefault());
}

TEST(Value, notDefault) {
  auto object = TestType().create();
  object->set("a", mkptr<ScalarValue>(2l));
  object->validate();

  EXPECT_TRUE(object->get("a")->equals(ScalarValue(2l)));
  EXPECT_TRUE(!object->get("a")->isDefault());
}


TEST(Value, defaultNotSet) {
  auto object = TestType().create();
  object->validate();
  Workspace ws;
  GeneratorContext context(ws);
  object->generate(context);

  EXPECT_TRUE(object->get("a")->equals(ScalarValue(1l)));
  EXPECT_TRUE(object->get("a")->isDefault());
}


TEST(Value, Immutable) {
  auto v = mkptr<MapValue>();
  auto v1 = mkptr<MapValue>();
  
  v1->set("b", mkptr<ScalarValue>(1l));
  v->set("a", v1);
  v1->set("b", mkptr<ScalarValue>(2l));
  
  auto vb = v->get("a")->asMap()->get("b")->asScalar()->asInteger();
  auto v1b = v1->get("b")->asScalar()->asInteger();
  EXPECT_NE(vb, v1b);
}