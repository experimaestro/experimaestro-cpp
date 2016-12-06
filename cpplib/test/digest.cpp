//
// Digest-related test on structured values
//

#include <xpm/xpm.h>
#include <gtest/gtest.h>

//using nlohmann::json;
using namespace xpm;

TEST(Digest, Same) {
  StructuredValue::Ptr v0 = StructuredValue::parse(R"({ "a":  1  })");
  StructuredValue::Ptr v1 = StructuredValue::parse(R"({ "a": 1 })");
  EXPECT_EQ(v0->uniqueIdentifier(), v1->uniqueIdentifier());
}

TEST(Digest, Different) {
  StructuredValue::Ptr v0 = StructuredValue::parse(R"({ "a": 1 })");
  StructuredValue::Ptr v1 = StructuredValue::parse(R"({ "a": 2 })");
  EXPECT_NE(v0->uniqueIdentifier(), v1->uniqueIdentifier());
}
TEST(Digest, IgnoredKey) {
  StructuredValue::Ptr v0 = StructuredValue::parse(R"({"$path": 1, "a": 1})");
  StructuredValue::Ptr v1 = StructuredValue::parse(R"({ "a": 1 })");
  EXPECT_EQ(v0->uniqueIdentifier(), v1->uniqueIdentifier());
}

TEST(Digest, subkeys) {
  StructuredValue::Ptr v0 = StructuredValue::parse(R"({ "a": 1})");
  StructuredValue::Ptr v1 = StructuredValue::parse(R"({ "a": { "$value": 2, "z": "ignore" } })");
  EXPECT_EQ(v0->uniqueIdentifier(), v1->uniqueIdentifier());
}
