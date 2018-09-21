//
// Created by Benjamin Piwowarski on 09/11/2016.
//

#include <mutex>
#include <condition_variable>

#include "Poco/Net/HTTPRequest.h"
#include "Poco/Net/HTTPResponse.h"
#include "Poco/Net/HTTPMessage.h"
#include "Poco/Net/WebSocket.h"
#include "Poco/Net/SocketNotification.h"
#include "Poco/Net/HTTPClientSession.h"
#include <Poco/Net/SocketReactor.h>
#include <Poco/NObserver.h>

#include <xpm/rpc/jsonrpcclient.hpp>

#include <xpm/common.hpp>
#include <__xpm/common.hpp>

DEFINE_LOGGER("rpc");

using nlohmann::json;

using Poco::Net::HTTPClientSession;
using Poco::Net::HTTPRequest;
using Poco::Net::HTTPResponse;
using Poco::Net::HTTPMessage;
using Poco::Net::WebSocket;
using Poco::Net::SocketReactor;
using Poco::Buffer;

namespace xpm {

typedef unsigned long RequestId;

std::string JSONRPC_VERSION = "2.0";

enum ConnectionStatus {
  WAITING,
  OPENED,
  FAILURE,
  CLOSED
};

class _JSONRPCClient {
 public:
  /// Counter for JSON request
  RequestId _currentRequestId;

  // Pointer to the web socket
  std::unique_ptr<Poco::Net::WebSocket> _websocket;

  std::string host;
  int port;

  // Opened connection
  std::mutex m_open;
  std::condition_variable cv_open;
  ConnectionStatus connected;

  // Incoming messages
  std::mutex m_incoming_message;
  std::condition_variable cv_incoming_message;

  std::map<RequestId, JsonMessage *> _requests;

  // Reading thread
  std::thread _thread;

  /// Callback handler for notifications
  JsonRPCCallback _callbackHandler;

  /// Debug flag
  bool debug;

  Poco::NObserver<_JSONRPCClient, Poco::Net::ReadableNotification> readHandler;
  Poco::NObserver<_JSONRPCClient, Poco::Net::WritableNotification> writeHandler;
  Poco::NObserver<_JSONRPCClient, Poco::Net::ShutdownNotification> shutdownHandler;
  SocketReactor reactor;

  ~_JSONRPCClient() {
    // Closing connection
    try {
      // Wait for close signal
      LOGGER->info("Waiting for close signal");
      std::unique_lock<std::mutex> lk(m_open);
      cv_open.wait(lk, [&] { return connected == CLOSED; });
    } catch(std::exception &e) {
      LOGGER->error("Caught exception \"{}\" while closing handle", e.what());
    }

    // Wait for websocket to be closed
    _thread.join();

    LOGGER->info("Connection closed");
  }

  _JSONRPCClient(std::string const &host, int port, std::string const &username, std::string const &password, bool debug)
      : host(host), port(port), connected(WAITING), debug(debug), 
      readHandler(*this, &_JSONRPCClient::handleEvent),
      writeHandler(*this, &_JSONRPCClient::handleWrite),
      shutdownHandler(*this, &_JSONRPCClient::handleShutdown),
      reactor(Poco::Timespan(1, 0)) {

    _thread = std::thread([&] {
      start(username, password);
    });
  }

  void setHandler(JsonRPCCallback handler) {
    _callbackHandler = handler;
  }

  void handleEvent(Poco::AutoPtr<Poco::Net::ReadableNotification> const & p) {
    LOGGER->info("Received read notification");

    LOGGER->info("Reading...");
    reactor.removeEventHandler(*_websocket, readHandler);
      
      Buffer<char> buffer(4096);
      int flags = 0;
      int count = _websocket->receiveFrame(buffer, flags);
      if (count == 0) return; // WS shutdown
      LOGGER->info("Received a frame of length {}: {} / {}", count, buffer.begin(), flags);

      if (flags & WebSocket::FRAME_BINARY) {
        LOGGER->info("Binary frame");
        handle(json::from_cbor(buffer));
      } else if (flags & WebSocket::FRAME_TEXT) {
        LOGGER->info("Text frame");
        handle(json::parse(buffer));
      } else if (flags & WebSocket::FRAME_OP_PING) {
        LOGGER->info("Ping frame");
        // _websocket->sendFrame("", 0, WebSocket::FRAME_OP_PONG);
      } else {
        LOGGER->warn("Received unknown frame {}", flags);
      }
    reactor.addEventHandler(*_websocket, readHandler);
  }

  void handleWrite(Poco::AutoPtr<Poco::Net::WritableNotification> const & p) {
    LOGGER->info("Received writable notification");
    reactor.removeEventHandler(*_websocket, writeHandler);
    _websocket->sendFrame("Hello", 0);
  }

  void handleShutdown(Poco::AutoPtr<Poco::Net::ShutdownNotification> const & p) {
    LOGGER->error("Received shutdown notification");
  }

  void start(std::string const &username, std::string const &password) {
    try {
      connected = WAITING;
      HTTPClientSession cs(host, port); // "echo.websocket.org", 80);    
      HTTPRequest request(HTTPRequest::HTTP_GET, "/ws",HTTPMessage::HTTP_1_1);
      HTTPResponse response;

      _websocket = std::make_unique<WebSocket>(cs, request, response);

      reactor.addEventHandler(*_websocket, readHandler);
      reactor.addEventHandler(*_websocket, writeHandler);
      reactor.addEventHandler(*_websocket, shutdownHandler);

      reactor.run();

      LOGGER->debug("Connected to websocket client");
      connected = ConnectionStatus::OPENED;
      cv_open.notify_all();

      // Closed
      LOGGER->info("Connection closed");
      connected = CLOSED;
      cv_open.notify_all();
   } catch (std::exception const &e) {
      LOGGER->debug("Error while trying to connect: {}", e.what());
      connected = FAILURE;
      cv_open.notify_all();
    }
  }

  void send(json const &message) {
    while (true) {
      std::unique_lock<std::mutex> lk(m_open);
      LOGGER->debug("Waiting to send message...");
      cv_open.wait(lk, [&] { return connected != WAITING; });
      if (connected != ConnectionStatus::OPENED) {
        throw exception("Cannot send a message: connection is closed");
      }

      LOGGER->debug("Sending message {}",  message.dump()); // FIXME: remove
      auto data = nlohmann::json::to_cbor(message);
      _websocket->sendFrame(data.data(), data.size() * sizeof(decltype(data)::value_type), WebSocket::FRAME_BINARY);
    }
  }

  void handle(json && message) {
    {
      LOGGER->debug("Received message {}", message.dump()); // FIXME: remove
      std::lock_guard<std::mutex> lk(m_incoming_message);

      if (message["id"].is_null()) {
        // Notification
        if (this->_callbackHandler) {
          this->_callbackHandler(message);
        }
        return;
      }

      const std::string mId = message["id"];
      RequestId requestId = (RequestId) std::stol(mId.c_str());
      auto it = _requests.find(requestId);
      if (it == _requests.end()) {
        std::cerr << "[error] Unknown request id " << requestId << ", ignoring" << std::endl;
        return;
      }
      it->second->_message = std::move(message);
    }

    cv_incoming_message.notify_all();
  }

  /// Sends a message and wait for the answer
  JsonMessage request(std::string const &method, json const &params) {
    // Add request to list of requests
    JsonMessage answer;
    RequestId requestId;
    json request;
    {
      std::lock_guard<std::mutex> lk(m_incoming_message);
      requestId = _currentRequestId++;
      request = json(
          {
              {"jsonrpc", JSONRPC_VERSION},
              {"id", requestId},
              {"method", method},
              {"params", params}
          }
      );
      _requests[requestId] = &answer;
    }

    // Send to server
    send(request);

    // Wait for answer
    {
      std::unique_lock<std::mutex> lk(m_incoming_message);
      cv_incoming_message.wait(lk, [&] { return !answer.empty(); });
    }

    // Remove request from request map
    {
      std::lock_guard<std::mutex> lk(m_incoming_message);
      _requests.erase(requestId);
    }
    return answer;
  }

};

JsonRPCClient::JsonRPCClient(std::string const &host, int port,
                             std::string const &username, std::string const &password, bool debug) {
  _client = new _JSONRPCClient(host, port, username, password, debug);
}

JsonRPCClient::~JsonRPCClient() {
  LOGGER->info("Closing RPC client");
  // Delete connection
  if (_client) delete _client;
}

JsonMessage JsonRPCClient::request(std::string const &method, const json &params) {
  return _client->request(method, params);
}
void JsonRPCClient::setHandler(JsonRPCCallback handler) {
  _client->setHandler(handler);
}

bool JsonMessage::error() const {
  return _message.count("error") > 0;
}

nlohmann::json const &JsonMessage::result() const {
  if (_message.count("result") > 0) {
    return _message["result"];
  }

  static const json nulljson;
  return nulljson;
}

int JsonMessage::errorCode() const {
  return _message["error"]["code"];
}

std::string JsonMessage::errorMessage() const {
  return _message["error"]["message"];
}
bool JsonMessage::empty() const {
  return _message.is_null();
}

}
