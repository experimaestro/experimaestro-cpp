//
// Created by Benjamin Piwowarski on 29/11/2016.
//

#include <unordered_set>

#include <xpm/rpc/utils.hpp>
#include <xpm/rpc/client.hpp>
#include <xpm/common.hpp>
#include "../private.hpp"

namespace xpm {
namespace rpc {
using nlohmann::json;

namespace {
  auto LOGGER = logger("rpc");
  std::unordered_multiset<ObjectIdentifierType> objectCounts;

  void removeObject(ObjectIdentifierType identifier) {
    auto range = objectCounts.equal_range(identifier);
    if (range.first == objectCounts.end()) {
      LOGGER->warn("Object {} existed but had no associated count", identifier);
      return;
    }
    objectCounts.erase(range.first);
    auto count = objectCounts.count(identifier);
    if (count == 0) {
      LOGGER->debug("Deleting object {}", identifier);
      auto response = Client::defaultClient().call("objects.__delete__", {{"__this__", identifier}});
      if (response.error()) {
        std::cerr << "Error while destroying object on server: " << response.errorMessage() << std::endl;
      }
    } else {
      LOGGER->debug("NOT deleting object {} since there are still {} references", identifier, count);
    }
  }
}

ServerObject::ServerObject(ObjectIdentifier o) : _identifier(o.id) {
  LOGGER->debug("New object ID {} [{}]", _identifier, typeid(*this).name());
  if (_identifier >= 0) {
    objectCounts.insert(_identifier);
  }
}

json ServerObject::__call__(std::string const &name, json &params) {
  params["__this__"] = _identifier;
  if (_identifier < 0) {
    throw exception("Identifier for the object is null");
  }
  LOGGER->debug("rpc call {} [{}]", name, _identifier);

  auto response = Client::defaultClient().call(name, params);
  if (response.error()) {
    throw std::runtime_error("Error with RPC call: " + response.errorMessage());
  }
  return response.result();
}

json ServerObject::__static_call__(std::string const &name, json const &params) {
  LOGGER->debug("rpc static call {}", name);
  auto response = Client::defaultClient().call(name, params);
  if (response.error()) {
    throw std::runtime_error("Error with RPC call: " + response.errorMessage());
  }
  return response.result();
}

void ServerObject::__set__(json const &params) {
  ObjectIdentifierType identifier = params;
  LOGGER->debug("Changing object ID {} to {} [{}]", _identifier, identifier, typeid(*this).name());

  if (identifier != _identifier && _identifier >= 0) {
    removeObject(_identifier);
  }

  _identifier = params;
  if (_identifier >= 0) {
    objectCounts.insert(_identifier);
  }
}

ServerObject::ServerObject() : _identifier(-1) {
}

ServerObject::~ServerObject() {
  if (_identifier >= 0) {
    removeObject(_identifier);
  }
}

}
}
