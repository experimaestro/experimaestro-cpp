//
// Created by Benjamin Piwowarski on 30/11/2016.
//

#include <xpm/rpc/objects.hpp>
#include <xpm/rpc/client.hpp>
#include <xpm/register.hpp>
#include <xpm/xpm.hpp>
#include <xpm/logging.hpp>

using namespace xpm;

// Just to try...
int main(int ac, char const **av) {
  xpm::setLogLevel("xpm", LogLevel::DEBUG);

  Register xpmRegister;
  xpmRegister.loadYAML(Path(av[1]));
//  setenv("XPM_NOTIFICATION_URL", av[1], true);
//  progress(.1);
//  rpc::Client client(av[1], av[2], av[3]);
//
//  // Ping
//  std::cerr << "Ping: " << client.ping() << std::endl;
//
//  // RMI
//  rpc::SSHOptions options;
//  options.hostname("gate-ia.lip6.fr");
}
