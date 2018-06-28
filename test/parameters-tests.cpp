//
// Created by Benjamin Piwowarski on 13/12/2016.
//

#include <xpm/xpm.hpp>
#include <xpm/type.hpp>
#include <xpm/workspace.hpp>
#include <gtest/gtest.h>

//using nlohmann::json;
using namespace xpm;

struct TestType {
  std::shared_ptr<Type> type;
  TestType() : type(std::make_shared<Type>(Typename("test"))) {
    auto a = std::make_shared<Argument>("a");
    a->defaultValue(mkptr<ScalarParameters>(1l));
    type->addArgument(a);
  }

  ptr<MapParameters> create() {
    auto ptr = mkptr<MapParameters>();
    ptr->type(type);
    return ptr;
  }
};

TEST(Parameters, defaultSet) {
  auto object = TestType().create();
  object->set("a", mkptr<ScalarParameters>(1l));
  object->validate();

  EXPECT_TRUE(object->get("a")->equals(ScalarParameters(1)));
  EXPECT_TRUE(object->get("a")->isDefault());
}

TEST(Parameters, notDefault) {
  auto object = TestType().create();
  object->set("a", mkptr<ScalarParameters>(2));
  object->validate();

  EXPECT_TRUE(object->get("a")->equals(ScalarParameters(2)));
  EXPECT_TRUE(!object->get("a")->isDefault());
}


TEST(Parameters, defaultNotSet) {
  auto object = TestType().create();
  object->validate();
  Workspace ws;
  GeneratorContext context(ws);
  object->generate(context);

  EXPECT_TRUE(object->get("a")->equals(ScalarParameters(1)));
  EXPECT_TRUE(object->get("a")->isDefault());
}
