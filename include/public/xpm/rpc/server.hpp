//
// Created by Benjamin Piwowarski on 26/11/2016.
//

#ifndef PROJECT_RPC_SERVER_H
#define PROJECT_RPC_SERVER_H

#include <string>
#include <unordered_map>
#include <xpm/json.hpp>
#include <xpm/rpc/jsonrpcclient.hpp>
#include <Poco/Util/ServerApplication.h>

namespace xpm::rpc {
class Server : public Poco::Util::ServerApplication {
  int serve();
public:
  static void ensureStarted();
};
}

#endif //PROJECT_RPC_SERVER_H
