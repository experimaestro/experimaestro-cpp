//
// Created by Benjamin Piwowarski on 26/11/2016.
//

#ifndef PROJECT_RPC_SERVER_H
#define PROJECT_RPC_SERVER_H

#include <string>
#include <unordered_map>
#include <memory>

#include <xpm/json.hpp>
#include <xpm/rpc/jsonrpcclient.hpp>
#include <Poco/Util/ServerApplication.h>

namespace Poco::Data { class Session; }

namespace xpm::rpc {
class Server : public Poco::Util::ServerApplication {
  std::unique_ptr<Poco::Data::Session> session;

  int serve();
public:
  static void ensureStarted();
};
}

#endif //PROJECT_RPC_SERVER_H
