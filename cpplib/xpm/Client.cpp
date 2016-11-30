//
// Created by Benjamin Piwowarski on 26/11/2016.
//
#include <functional>

#include "Client.h"

namespace xpm {

using nlohmann::json;

Client::Client(const std::string &wsURL, const std::string &username, const std::string &password)
    : _client(wsURL, username, password, true) {
  _client.setHandler(std::bind(&Client::handler, this, std::placeholders::_1));
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

} // xpm ns

// Just to try...
int main(int ac, char const **av) {
  xpm::Client client(av[1], av[2], av[3]);

  std::cerr << "Ping: " << client.ping() << std::endl;
}
