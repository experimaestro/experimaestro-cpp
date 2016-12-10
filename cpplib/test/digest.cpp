//
// Digest-related test on structured values
//

#include <xpm/xpm.hpp>
#include <gtest/gtest.h>

//using nlohmann::json;
using namespace xpm;

TEST(Digest, Same) {
  StructuredValue v0 = StructuredValue::parse(R"({  "a":  1  })");
  StructuredValue v1 = StructuredValue::parse(R"({ "a": 1 })");
  EXPECT_EQ(v0.uniqueIdentifier(), v1.uniqueIdentifier());
}

TEST(Digest, Different) {
  StructuredValue v0 = StructuredValue::parse(R"({ "a": 1 })");
  StructuredValue v1 = StructuredValue::parse(R"({ "a": 2 })");
  EXPECT_NE(v0.uniqueIdentifier(), v1.uniqueIdentifier());
}

TEST(Digest, IgnoredKey) {
  StructuredValue v0 = StructuredValue::parse(R"({"$path": 1, "a": 1})");
  StructuredValue v1 = StructuredValue::parse(R"({ "a": 1 })");
  EXPECT_EQ(v0.uniqueIdentifier(), v1.uniqueIdentifier());
}

TEST(Digest, subkeys) {
  StructuredValue v0 = StructuredValue::parse(R"({ "a": 1})");
  StructuredValue v1 = StructuredValue::parse(R"({ "a": { "$value": 1, "z": "ignore" } })");
  EXPECT_EQ(v0.uniqueIdentifier(), v1.uniqueIdentifier());
}
