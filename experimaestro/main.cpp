//
// Created by Benjamin Piwowarski on 30/11/2016.
//

#include <xpm/logging.hpp>
#include <xpm/register.hpp>
#include <xpm/rpc/client.hpp>
#include <xpm/xpm.hpp>

#include <xpm/rpc/server.hpp>
#include <xpm/common.hpp>

#include <__xpm/CLI11.hpp>
#include <__xpm/common.hpp>

using namespace xpm;

DEFINE_LOGGER("main");

// Just to try...
int main(int argc, char const **argv) {
  xpm::setLogLevel("xpm", LogLevel::DEBUG);

  CLI::App app{"Experimaestro command line application"};
  app.require_subcommand(1);
  app.fallthrough(false);

  auto _generate =
      app.add_subcommand("server", "Start the experimaestro server");
  _generate->set_callback([&]() {  
    LOGGER->info("Starting server");
    rpc::Server::ensureStarted();
  });

  CLI11_PARSE(app, argc, argv);
}