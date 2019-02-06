//
// Created by Benjamin Piwowarski on 26/11/2016.
//

#ifndef PROJECT_RPC_SERVER_H
#define PROJECT_RPC_SERVER_H

#include <memory>
#include <string>
#include <unordered_map>

#include <Poco/Util/ServerApplication.h>
#include <xpm/json.hpp>
#include <xpm/rpc/jsonrpcclient.hpp>

namespace Poco {
  class File;
}
namespace Poco::Data {
  class Session;
}
namespace Poco::Net {
  class HTTPServer;
}

namespace xpm::rpc {

class ServerContext;

class Server : public Poco::Util::ServerApplication {
  std::unique_ptr<Poco::Net::HTTPServer> _httpserver;
  std::unique_ptr<Poco::File> _pidfile;
  std::string _baseurl;
public:
  /// Close the server
  Server();
  virtual ~Server();

  /// Get a client handle
  static void client();

  /// Start the server and wait for termination
  void serve(ServerContext & context, bool locked);

  /// Start the server
  void start(ServerContext & context, bool locked);

  /// Close
  void terminate();

  /// Get the notification URL
  std::string getNotificationURL() const;
};

} // namespace xpm::rpc

#endif // PROJECT_RPC_SERVER_H
