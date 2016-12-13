//
// Created by Benjamin Piwowarski on 13/12/2016.
//

#include <xpm/xpm.hpp>
#include <gtest/gtest.h>

//using nlohmann::json;
using namespace xpm;

struct TestType {
  Type type;
  TestType() : type(TypeName("test")) {
    Argument a("a");
    a.defaultValue(Value(1l));
    type.addArgument(a);
  }
};

TEST(StructuredValue, defaultSet) {
  auto object = TestType().type.create();
  object->set("a", Value(1l));
  object->validate();

  auto const value = object->getValue();
  EXPECT_EQ(value["a"].value(), Value(1));
  EXPECT_TRUE(value["a"].isDefault());
}

TEST(StructuredValue, notDefault) {
  auto object = TestType().type.create();
  object->set("a", Value(2));
  object->validate();

  auto const value = object->getValue();
  EXPECT_EQ(value["a"].value(), Value(2));
  EXPECT_TRUE(!value["a"].isDefault());
}


TEST(StructuredValue, defaultNotSet) {
  auto object = TestType().type.create();
  object->validate();

  auto const value = object->getValue();
  EXPECT_EQ(value["a"].value(), Value(1));
  EXPECT_TRUE(value["a"].isDefault());
}
