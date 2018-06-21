//
// Digest-related test on structured values
//

#include <xpm/xpm.hpp>
#include <xpm/register.hpp>
#include <gtest/gtest.h>
#include <xpm/cpp.hpp>

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

TEST(Digest, subkeys) {
  Register r;
  auto v0 = r.build(R"({ "a": 1})");
  auto v1 = r.build(R"({ "a": { "$value": 1, "z": "ignore" } })");
  EXPECT_EQ(v0->uniqueIdentifier(), v1->uniqueIdentifier());
}


TEST(Digest, ignore) {
  auto r = mkptr<CppRegister>();

  SimpleCppTypeBuilder("ignore", r)
    .argument<int>("a").required(true)
    .argument<int>("b").required(true).ignore(true);

  auto v0 = r->build(R"({ "a": 1, "$type": "ignore" })");
  auto v1 = r->build(R"({ "a": { "$value": 1 }, "b": { "$value": 2 },  "$type": "ignore"})");
  EXPECT_EQ(v0->uniqueIdentifier(), v1->uniqueIdentifier());
}


TEST(Digest, ignorePath) {
  auto r = mkptr<CppRegister>();

  auto v0 = r->build(R"({ "a": 1, "b": { "$type": "path", "value": "/a/path" } })");
  auto v1 = r->build(R"({ "a": 1, "b": { "$type": "path", "value": "/another/path" } })");
  EXPECT_EQ(v0->uniqueIdentifier(), v1->uniqueIdentifier());
}



TEST(Digest, default) {
  auto r = mkptr<CppRegister>();

  SimpleCppTypeBuilder("default", r)
    .argument<int>("a").required(true)
    .argument<int>("b").required(true).defaultValue(2);

  auto v0 = r->build(R"({ "a": 1, "$type": "default" })");
  auto v1 = r->build(R"({ "a": { "$value": 1 }, "b": { "$value": 2 }, "$type": "default" })");
  EXPECT_EQ(v0->uniqueIdentifier(), v1->uniqueIdentifier());
}
