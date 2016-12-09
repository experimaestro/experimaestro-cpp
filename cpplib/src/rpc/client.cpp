//
// Created by Benjamin Piwowarski on 26/11/2016.
//
#include <functional>

#include <include/xpm/rpc/client.hpp>

namespace xpm {
namespace rpc {
using nlohmann::json;

Client *Client::DEFAULT_CLIENT = nullptr;

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
    throw std::runtime_error("Default RPC client is not defined");
  }
  return *DEFAULT_CLIENT;
}

} // xpm ns
} // rpc ns