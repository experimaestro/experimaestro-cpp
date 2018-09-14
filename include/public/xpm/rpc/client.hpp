//
// Created by Benjamin Piwowarski on 26/11/2016.
//

#ifndef EXPERIMAESTRO_RPC_CLIENT_H
#define EXPERIMAESTRO_RPC_CLIENT_H

#include <string>
#include <unordered_map>
#include <xpm/json.hpp>
#include <xpm/rpc/jsonrpcclient.hpp>

namespace xpm::rpc {

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

} // ns xpm::rpc

#endif //PROJECT_XPMCLIENT_H
