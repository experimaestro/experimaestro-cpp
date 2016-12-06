//
// Created by Benjamin Piwowarski on 06/12/2016.
//

//#include <xpm/json.hpp>
#include <xpm/xpm.h>

#include <gtest/gtest.h>

//using nlohmann::json;
using namespace xpm;

TEST(Digest, Same) {
  StructuredValue
      v0(R"({ "a":  01  })"),
      v1(R"({ "a": 1 }))");
  EXPECT_EQ(v0.uniqueIdentifier(), v1.uniqueIdentifier());
}

TEST(Digest, Different) {
  StructuredValue
      v0(R"({ "a": 1 })"),
      v1(R"({ "a": 2 }))");
  EXPECT_NE(v0.uniqueIdentifier(), v1.uniqueIdentifier());
}
TEST(Digest, IgnoredKey) {
  StructuredValue
      v0(R"({"$path": 1, "a": 1})"),
      v1(R"({ "a": 1 }))");
  EXPECT_EQ(v0.uniqueIdentifier(), v1.uniqueIdentifier());
}


TEST(Digest, subkeys) {
  StructuredValue
      v0(R"({ "a": 1})"),
      v1(R"({ "a": { "$value": 2, "z": "ignore" } }))");
  EXPECT_EQ(v0.uniqueIdentifier(), v1.uniqueIdentifier());
}
