//
// Created by Benjamin Piwowarski on 30/11/2016.
//

#include <xpm/rpc/objects.hpp>
#include <xpm/rpc/client.hpp>

using namespace xpm;

// Just to try...
int main(int ac, char const **av) {
  rpc::Client client(av[1], av[2], av[3]);

  // Ping
  std::cerr << "Ping: " << client.ping() << std::endl;

  // RMI
  rpc::SSHOptions options;
  options.hostname("gate-ia.lip6.fr");
}