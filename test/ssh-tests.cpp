#include <gtest/gtest.h>

#include <libssh/libssh.h>

#include <xpm/connectors/ssh.hpp>
#include <__xpm/common.hpp>

#include <config.h>

DEFINE_LOGGER("ssh-tests");

using namespace xpm;
#define str(s) #s

TEST(SSH, Basic) {
    // ssh_set_log_level(SSH_LOG_FUNCTIONS);
    SSHConnector ssh("testuser@localhost:2200");
    std::string rsaPath = XPM_TEST_SOURCEDIR  "/docker/userdir/.ssh/id_rsa";
    LOGGER->info("Adding identify {}", rsaPath);
    ssh.addIdentity(rsaPath);
    auto out = ssh.ostream(Path("/home/testuser/file"));
    *out << "Hello world\n";
}

