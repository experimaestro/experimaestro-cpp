#include <cstdlib>
#include <fstream>

#include <xpm/json.hpp>
#include <xpm/rpc/configuration.hpp>
#include <__xpm/common.hpp>

DEFINE_LOGGER("rpc");

namespace xpm { namespace rpc {

using nlohmann::json;

ConfigurationParameters::ConfigurationParameters(std::string const &path) {
  auto defaultPath = std::string(std::getenv("HOME")) + "/.experimaestro";

  std::string _path = path;
  if (path.empty()) {
    _path = defaultPath + "/settings.json";
  }
  LOGGER->info("Reading configuration {}", path);

  std::ifstream in(_path);
  if (!in) {
    throw exception("Cannot read configuration file " + _path);
  }

  json j = json::parse(in);

  if (j.count("server") == 0) {
    throw exception("No server section in " + _path);
  }

  auto server = j["server"];

  _serverConfiguration = {
      server["name"],
      (int)server["port"],
      server.count("host") > 0 ? server["host"].get<std::string>() : std::string("localhost"),
      server.count("directory") > 0 ? server["directory"].get<std::string>() : defaultPath
  };


}

ServerConfiguration const& ConfigurationParameters::serverConfiguration() const {
  return _serverConfiguration;
}

}} // xpm::rpc
