//
// Created by Benjamin Piwowarski on 09/11/2016.
//

#ifndef XPM_JSONRPCCLIENT_H
#define XPM_JSONRPCCLIENT_H

#include <xpm/json.hpp>

namespace xpm {

// Hidden class
class _JSONRPCClient;

class JsonMessage {
  friend class _JSONRPCClient;

  nlohmann::json _message;
 public:
  bool empty() const;
  int errorCode() const;
  std::string errorMessage() const;

  nlohmann::json const &result() const;
  bool error() const;
};

typedef std::function<void(nlohmann::json const &)> JsonRPCCallback;

class JsonRPCClient {
  JsonRPCClient(JsonRPCClient const &) = delete;
  _JSONRPCClient *_client;
 public:

  /// Construct a connection with a given URI
  JsonRPCClient(std::string const &host, int port, std::string const &username,
                std::string const &password, bool debug);

  /// Close connection
  ~JsonRPCClient();

  /// Send a message and wait for an answer
  JsonMessage request(std::string const &method, nlohmann::json const &json);

  /// Set the notification handler
  void setHandler(JsonRPCCallback handler);

};

}

#endif //XPM_JSONRPCCLIENT_H
