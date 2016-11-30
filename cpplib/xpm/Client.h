//
// Created by Benjamin Piwowarski on 26/11/2016.
//

#ifndef PROJECT_XPMCLIENT_H
#define PROJECT_XPMCLIENT_H

#include <string>
#include "JsonRPCClient.h"

namespace xpm {
class Client {
  JsonRPCClient _client;

  void handler(nlohmann::json const &message);
 public:
  Client(std::string const &wsURL, std::string const &username, std::string const &password);

  /// Returns true if the server is alive
  bool ping();
};
}
#endif //PROJECT_XPMCLIENT_H
