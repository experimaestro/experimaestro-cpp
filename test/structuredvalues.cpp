//
// Created by Benjamin Piwowarski on 13/12/2016.
//

#include <xpm/xpm.hpp>
#include <xpm/value.hpp>
#include <gtest/gtest.h>

//using nlohmann::json;
using namespace xpm;

struct TestType {
  std::shared_ptr<Type> type;
  TestType() : type(std::make_shared<Type>(TypeName("test"))) {
    auto a = std::make_shared<Argument>("a");
    a->defaultValue(mkptr<StructuredValue>(Value(1l)));
    type->addArgument(a);
  }

  ptr<StructuredValue> create() {
    auto ptr = mkptr<StructuredValue>();
    ptr->type(type);
    return ptr;
  }
};

TEST(StructuredValue, defaultSet) {
  auto object = TestType().create();
  object->set("a", mkptr<StructuredValue>(Value(1l)));
  object->validate();

  EXPECT_TRUE(object->get("a")->equals(Value(1)));
  EXPECT_TRUE(object->get("a")->isDefault());
}

TEST(StructuredValue, notDefault) {
  auto object = TestType().create();
  object->set("a", mkptr<StructuredValue>(Value(2)));
  object->validate();

  EXPECT_TRUE(object->get("a")->equals(Value(2)));
  EXPECT_TRUE(!object->get("a")->isDefault());
}


TEST(StructuredValue, defaultNotSet) {
  auto object = TestType().create();
  object->validate();

  EXPECT_TRUE(object->get("a")->equals(Value(1)));
  EXPECT_TRUE(object->get("a")->isDefault());
}
