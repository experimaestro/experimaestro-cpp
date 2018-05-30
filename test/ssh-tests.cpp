
#include <fstream>
#include <gtest/gtest.h>

#include <libssh/libssh.h>

#include <__xpm/common.hpp>
#include <xpm/connectors/local.hpp>
#include <xpm/connectors/ssh.hpp>

#include <config.h>

DEFINE_LOGGER("ssh-tests");

using namespace xpm;

class SshTest : public ::testing::Test {
protected:
  static ptr<SSHConnector> sshConnector;
  static const Path localpath;
  static const Path remotepath;

  static void SetUpTestCase() {
    try {
      ssh_set_log_level(SSH_LOG_FUNCTIONS);

      sshConnector = mkptr<SSHConnector>("testuser@localhost:2200");
      std::string rsaPath = XPM_TEST_USERDIR "/.ssh/id_rsa";
      sshConnector->addIdentity(rsaPath);

      // Remove everything in tree
      LocalConnector local;
      if (local.fileType(localpath) != FileType::UNEXISTING) {
        local.remove(localpath, true);
      }
      local.mkdir(localpath);
    } catch (...) {
      sshConnector = nullptr;
      throw;
    }
  }

  static void TearDownTestCase() {
    LocalConnector local;
    if (local.fileType(localpath) != FileType::UNEXISTING) {
      local.remove(localpath, true);
    }
  }

  virtual void SetUp() {
    ASSERT_TRUE(sshConnector) << "SSH setup was not successful";
  }
};

Path const SshTest::localpath(XPM_TEST_USERDIR "/ssh-test");
Path const SshTest::remotepath("/home/testuser/ssh-test");
ptr<SSHConnector> SshTest::sshConnector;

TEST_F(SshTest, OutputStream) {
  auto out = sshConnector->ostream(remotepath / "file");
  *out << "Hello world\n";
  out = nullptr;

  std::string s;
  std::ifstream ifin((localpath / "file").localpath());
  std::getline(ifin, s);
  ASSERT_EQ(s, "Hello world");
}

// TEST_F(SshTest, Lock) {
//   size_t x = 1;
//   auto lock = sshConnector->lock(remotepath / "lockfile.lock");
//   std::thread(
//       []() { auto lock = sshConnector->lock(remotepath / "lockfile.lock"); })
//       .detach();

//   x += 1;
// }

TEST_F(SshTest, Process) {
  auto builder = sshConnector->processBuilder();
  std::ostringstream ostr;
  builder->stdout = Redirect::pipe([&ostr](const char *bytes, size_t n) {
    ostr << std::string(bytes, n);
  });
  builder->command.push_back("/bin/echo");
  builder->command.push_back("-n");
  builder->command.push_back("hello");
  auto process = builder->start();

  EXPECT_EQ(process->exitCode(), 0);
  EXPECT_EQ(ostr.str(), "hello");
}

TEST_F(SshTest, ProcessStdin) {
  auto builder = sshConnector->processBuilder();
  std::ostringstream ostr;
  builder->stdout = Redirect::pipe([&ostr](const char *bytes, size_t n) {
    ostr << std::string(bytes, n);
  });
  builder->command.push_back("/bin/cat");
  auto process = builder->start();

  std::string hello = "hello";
  process->write((void*)hello.c_str(), hello.size());
  process->eof();
  EXPECT_EQ(process->exitCode(), 0);
  EXPECT_EQ(ostr.str(), hello);
}