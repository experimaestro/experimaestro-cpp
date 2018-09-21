//
// Created by Benjamin Piwowarski on 30/11/2016.
//

#include <xpm/logging.hpp>
#include <xpm/register.hpp>
#include <xpm/rpc/client.hpp>
#include <xpm/xpm.hpp>

#include <xpm/rpc/server.hpp>
#include <xpm/rpc/client.hpp>
#include <xpm/common.hpp>

#include <__xpm/CLI11.hpp>
#include <__xpm/common.hpp>

using namespace xpm;

DEFINE_LOGGER("main");

// Just to try...
int main(int argc, char const **argv) {
  CLI::App app{"Experimaestro command line application"};
  app.require_subcommand(1);
  app.fallthrough(false);

  app.add_flag("--verbose", [](size_t) {
    xpm::setLogLevel("xpm", LogLevel::DEBUG);
    xpm::setLogLevel("rpc", LogLevel::DEBUG);
  }, "Be more verbose");

  auto _generate =
      app.add_subcommand("server", "Start the experimaestro server");
  int _generate_locked = 0;
  _generate->add_flag("--locked", _generate_locked, "Use this flag when the lock has been already taken [internal]");
  _generate->set_callback([&]() {  
    LOGGER->info("Starting server");
    rpc::Server::start(_generate_locked > 0);
  });

  auto _client = app.add_subcommand("client", "Runs an experimaestro client (debugging)");
  _client->set_callback([&]() {  
    LOGGER->info("Starting client");
    try {
      auto & client = rpc::Client::defaultClient();
      LOGGER->info("Client started");
      client.call("ping", {});
    } catch(std::exception &e) {
      LOGGER->error("Got an exception: {}", e.what());
    }
  });



  CLI11_PARSE(app, argc, argv);
}