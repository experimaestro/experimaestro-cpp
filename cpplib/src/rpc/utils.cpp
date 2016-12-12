//
// Created by Benjamin Piwowarski on 29/11/2016.
//

#include <xpm/rpc/utils.hpp>
#include <xpm/rpc/client.hpp>
#include <spdlog/spdlog.h>

namespace xpm {
namespace rpc {
using nlohmann::json;

namespace {
  auto LOGGER = spdlog::stdout_color_mt("rpc");
}

json ServerObject::__call__(std::string const &name, json &params) {
  params["__this__"] = _identifier;
  auto response = Client::defaultClient().call(name, params);
  if (response.error()) {
    throw std::runtime_error("Error with RPC call: " + response.errorMessage());
  }
  return response.result();
}

json ServerObject::__static_call__(std::string const &name, json const &params) {
  LOGGER->info("rpc call {}", name);
  auto response = Client::defaultClient().call(name, params);
  if (response.error()) {
    throw std::runtime_error("Error with RPC call: " + response.errorMessage());
  }
  return response.result();
}

void ServerObject::__set__(json const &params) {
  _identifier = params;
}

ServerObject::ServerObject() : _identifier(-1) {
}

ServerObject::~ServerObject() {
  if (_identifier >= 0) {
    LOGGER->info("Deleting object {}", _identifier);
    auto response = Client::defaultClient().call("objects.__delete__", {{"__this__", _identifier}});
    if (response.error()) {
      std::cerr << "Error while destroying object on server: " << response.errorMessage() << std::endl;
    }
  }
}

}
}
