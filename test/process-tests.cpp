
#include "date.h"

#include <config.h>
#include <__xpm/common.hpp>
#include <xpm/connectors/local.hpp>
#include <xpm/workspace.hpp>
#include <gtest/gtest.h>


using namespace xpm;

DEFINE_LOGGER("xpm.tests.process");

namespace {
 const std::string EXITSCRIPT_PATH = XPM_TEST_SOURCEDIR "/scripts/exitscript";
} 

TEST(Process, done) {

  auto builder = LocalConnector().processBuilder();
  builder->command.push_back(EXITSCRIPT_PATH);
  builder->command.push_back("0");
  auto process = builder->start();
  auto code = process->exitCode();
  EXPECT_EQ(code, 0);
}

TEST(Process, error) {
  auto builder = LocalConnector().processBuilder();
  builder->command.push_back(EXITSCRIPT_PATH);
  builder->command.push_back("1");
  auto process = builder->start();
  auto code = process->exitCode();
  EXPECT_NE(code, 0);
}