//
// Created by Benjamin Piwowarski on 26/11/2016.
//

#include <functional>
#include <fstream>

#include <xpm/common.hpp>
#include <xpm/rpc/client.hpp>
#include <xpm/rpc/configuration.hpp>

#include <__xpm/common.hpp>

namespace xpm::rpc {
using nlohmann::json;

namespace {
  auto LOGGER = logger("rpc");
  auto SERVER_LOGGER = logger("server");
}

Client *Client::DEFAULT_CLIENT = nullptr;

Client::Client(const std::string &host, int port, const std::string &username, const std::string &password)
    : _client(host, port, username, password, true) {
  _client.setHandler(std::bind(&Client::handler, this, std::placeholders::_1));
  DEFAULT_CLIENT = this;
}

Client::~Client() {
  if (DEFAULT_CLIENT == this) DEFAULT_CLIENT = nullptr;
}

JsonMessage Client::call(std::string const &name, json const &params) {
  return _client.request(name, params);
}

bool Client::ping() {
  auto response = _client.request("ping", json::object());
  if (response.error()) {
    std::cerr << "Error while requesting " << response.errorMessage() << std::endl;
  }
  return !response.error();
}

void Client::handler(nlohmann::json const &message) {
  if (message["result"].count("type") && message["result"]["type"] == "log") {
    std::string logLevel = message["result"]["level"];
    std::string logMessage = message["result"]["message"];

    if (logLevel == "DEBUG") {
      SERVER_LOGGER->debug("{}", logMessage);
    } else if (logLevel == "INFO") {
      SERVER_LOGGER->info("{}", logMessage);
    } else if (logLevel == "WARN") {
      SERVER_LOGGER->warn("{}", logMessage);
    } else if (logLevel == "ERROR") {
      SERVER_LOGGER->error("{}", logMessage);
    } else {
      SERVER_LOGGER->error("RPC message with unknown level {}: {}", logLevel, logMessage);
    }
  } else {
    LOGGER->warn("Unhandled notification: {}", message.dump());
  }
}

Client &Client::defaultClient() {
  if (DEFAULT_CLIENT == nullptr) {
    // Try to connect
    ConfigurationParameters configuration;
    const auto conf = configuration.serverConfiguration();
    LOGGER->info("Connecting to default client {}:{}", conf.host, conf.port);

    static std::unique_ptr<Client> defaultClient(
      new Client(conf.host, conf.port, "hello", "world") // conf.username, conf.password)
    );
    defaultClient->ping();
  }
  return *DEFAULT_CLIENT;
}

} // xpm::rpc ns
