//
// Created by Benjamin Piwowarski on 26/11/2016.
//
#include <functional>

#include <include/xpm/rpc/client.hpp>
#include <fstream>

namespace xpm {
namespace rpc {
using nlohmann::json;

Client *Client::DEFAULT_CLIENT = nullptr;

Configuration::Configuration(std::string const &path) {
  std::cerr << "Reading default configuration...\n";
  std::string _path = path;
  if (path.empty()) {
    _path = std::string(std::getenv("HOME")) + "/.experimaestro/settings.json";
  }
  std::ifstream in(_path);

  json j = json::parse(in);
  defaultHost = j["default-host"];

  json hosts = j["hosts"];
  for (json::iterator it = hosts.begin(); it != hosts.end(); ++it) {
    std::string hostid = it.key();
    json hostc = it.value();
    configurations[hostid] = {
        hostid,
        hostc["host"],
        (int)hostc["port"],
        hostc["username"],
        hostc["password"]
    };
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
  std::cerr << "[notification] " << message.dump() << std::endl;
}

Client &Client::defaultClient() {
  if (DEFAULT_CLIENT == nullptr) {
    // Try to connect
    Configuration configuration;
    const auto conf = configuration.defaultConfiguration();
    std::string uri = "ws://" + conf.host + ":" + std::to_string(conf.port) + "/web-socket";
    std::cerr << "Connecting to default client " << uri << "..." << std::endl;

    static std::unique_ptr<Client> defaultClient(
      new Client(uri, conf.username, conf.password)
    );
    defaultClient->ping();
  }
  return *DEFAULT_CLIENT;
}

} // xpm ns
} // rpc ns
