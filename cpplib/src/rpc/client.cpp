//
// Created by Benjamin Piwowarski on 26/11/2016.
//

#include <functional>
#include <fstream>

#include <xpm/common.hpp>
#include <xpm/rpc/client.hpp>
#include <spdlog/tests/includes.h>

#include "../private.hpp"

namespace xpm {
namespace rpc {
using nlohmann::json;

namespace {
  auto LOGGER = logger("rpc");
  auto SERVER_LOGGER = logger("server");
}

Client *Client::DEFAULT_CLIENT = nullptr;

Configuration::Configuration(std::string const &path) {
  std::string _path = path;
  if (path.empty()) {
    _path = std::string(std::getenv("HOME")) + "/.experimaestro/settings.json";
  }
  LOGGER->info("Reading configuration {}", path);

  std::ifstream in(_path);
  if (!in) {
    throw exception("Cannot read configuration file " + _path);
  }

  json j = json::parse(in);

  defaultHost = j.count("default-host") > 0 ? j["default-host"] : "";

  json hosts = j["hosts"];
  for (json::iterator it = hosts.begin(); it != hosts.end(); ++it) {
    std::string hostid = it.key();
    json hostc = it.value();
    if (defaultHost == "") {
      defaultHost = hostid;
    }
    configurations[hostid] = {
        hostid,
        hostc["host"],
        (int)hostc["port"],
        hostc["username"],
        hostc["password"]
    };
  }

  if (hosts.empty()) {
    throw exception("No host defined in " + _path);
  }
}

HostConfiguration const& Configuration::defaultConfiguration() const {
  return configurations.find(defaultHost)->second;
}

Client::Client(const std::string &wsURL, const std::string &username, const std::string &password)
    : _client(wsURL, username, password, true) {
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
    Configuration configuration;
    const auto conf = configuration.defaultConfiguration();
    std::string uri = "ws://" + conf.host + ":" + std::to_string(conf.port) + "/web-socket";
    LOGGER->info("Connecting to default client {}", uri);

    static std::unique_ptr<Client> defaultClient(
      new Client(uri, conf.username, conf.password)
    );
    defaultClient->ping();
  }
  return *DEFAULT_CLIENT;
}

} // xpm ns
} // rpc ns
