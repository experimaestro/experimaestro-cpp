//
// Digest-related test on structured values
//

#include <xpm/xpm.hpp>
#include <xpm/register.hpp>
#include <gtest/gtest.h>

//using nlohmann::json;
using namespace xpm;

TEST(Digest, Same) {
  Register r;
  auto v0 = r.build(R"({  "a":  1  })");
  auto v1 = r.build(R"({ "a": 1 })");
  EXPECT_EQ(v0->uniqueIdentifier(), v1->uniqueIdentifier());
}

TEST(Digest, Different) {
  Register r;
  auto v0 = r.build(R"({ "a": 1 })");
  auto v1 = r.build(R"({ "a": 2 })");
  EXPECT_NE(v0->uniqueIdentifier(), v1->uniqueIdentifier());
}

TEST(Digest, IgnoredKey) {
  Register r;
  auto v0 = r.build(R"({"$path": 1, "a": 1})");
  auto v1 = r.build(R"({ "a": 1 })");
  EXPECT_EQ(v0->uniqueIdentifier(), v1->uniqueIdentifier());
}

TEST(Digest, subkeys) {
  Register r;
  auto v0 = r.build(R"({ "a": 1})");
  auto v1 = r.build(R"({ "a": { "$value": 1, "z": "ignore" } })");
  EXPECT_EQ(v0->uniqueIdentifier(), v1->uniqueIdentifier());
}

TEST(Digest, ignore) {
  Register r;
  auto v0 = r.build(R"({ "a": 1})");
  auto v1 = r.build(R"({ "a": { "$value": 1 }, "b": { "$value": 2, "$ignore": true }})");
  EXPECT_EQ(v0->uniqueIdentifier(), v1->uniqueIdentifier());
}

TEST(Digest, default) {
  Register r;
  auto v0 = r.build(R"({ "a": 1})");
  auto v1 = r.build(R"({ "a": { "$value": 1 }, "b": { "$value": 2, "$default": true }})");
  EXPECT_EQ(v0->uniqueIdentifier(), v1->uniqueIdentifier());
}
