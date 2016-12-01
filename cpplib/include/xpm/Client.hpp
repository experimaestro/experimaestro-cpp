//
// Created by Benjamin Piwowarski on 26/11/2016.
//

#ifndef PROJECT_XPMCLIENT_H
#define PROJECT_XPMCLIENT_H

#include <string>
#include <xpm/json.hpp>
#include <xpm/JsonRPCClient.h>

namespace xpm {
class Client {
  JsonRPCClient _client;

  void handler(nlohmann::json const &message);

  static Client *DEFAULT_CLIENT;
 public:
  typedef nlohmann::json json;

  Client(std::string const &wsURL, std::string const &username, std::string const &password);
  ~Client();

  /// Generic request
  JsonMessage call(std::string const &name, json const &params);

  /// Returns true if the server is alive
  bool ping();

  static Client &defaultClient();
};
}
#endif //PROJECT_XPMCLIENT_H
