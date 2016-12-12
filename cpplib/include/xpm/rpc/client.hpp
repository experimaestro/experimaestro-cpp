//
// Created by Benjamin Piwowarski on 26/11/2016.
//

#ifndef PROJECT_XPMCLIENT_H
#define PROJECT_XPMCLIENT_H

#include <string>
#include <unordered_map>
#include <xpm/json.hpp>
#include <xpm/rpc/jsonrpcclient.hpp>

namespace xpm {
namespace rpc {

struct HostConfiguration {
  std::string id;
  std::string host;
  int port;
  std::string username;
  std::string password;
};

class Configuration {
  std::unordered_map<std::string, HostConfiguration> configurations;
  std::string defaultHost;
 public:
  Configuration(std::string const &path = "");
  HostConfiguration const &defaultConfiguration() const;
};

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

  /// Returns the default client
  static Client &defaultClient();
};

}
}

#endif //PROJECT_XPMCLIENT_H
