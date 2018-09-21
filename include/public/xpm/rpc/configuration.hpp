#ifndef EXPERIMAESTRO_RPC_CONFIGURATION_H
#define EXPERIMAESTRO_RPC_CONFIGURATION_H

#include <string>
#include <unordered_map>

namespace xpm { namespace rpc {

struct ServerConfiguration {
  std::string name;

  // Port name
  int port;
  
  // Host name
  std::string host;

  // Main directory
  std::string directory;

  /// Path to the experimaestro executable
  std::string experimaestro;
};

class ConfigurationParameters {
  ServerConfiguration _serverConfiguration;
 public:
  ConfigurationParameters(std::string const &path = "");
  ServerConfiguration const &serverConfiguration() const;
};
}} // ns xpm::rpc

#endif