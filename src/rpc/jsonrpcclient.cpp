//
// Created by Benjamin Piwowarski on 09/11/2016.
//

#include <mutex>
#include <condition_variable>

#include <xpm/rpc/jsonrpcclient.hpp>

#include <xpm/common.hpp>
#include <__xpm/common.hpp>

DEFINE_LOGGER("rpc");

using nlohmann::json;

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

  /// Client
  // client c;

  std::string uri;

  /// The connection
  // websocketpp::connection_hdl hdl;

  // Opened connection
  std::mutex m_open;
  std::condition_variable cv_open;
  ConnectionStatus connected;

  // Incoming messages
  std::mutex m_incoming_message;
  std::condition_variable cv_incoming_message;

  std::map<RequestId, JsonMessage *> _requests;

  // Thread
  std::thread _thread;

  /// Callback handler for notifications
  JsonRPCCallback _callbackHandler;

  /// Debug flag
  bool debug;

  ~_JSONRPCClient() {
    // Closing connection
    try {
      LOGGER->info("Closing handle");
      // c.close(hdl, 0, "Finished");

      // Wait for close signal
      LOGGER->info("Waiting for close signal");
      std::unique_lock<std::mutex> lk(m_open);
      // cv_open.wait(lk, [&] { return hdl.expired(); });
    } catch(std::exception &e) {
      LOGGER->error("Caught exception \"{}\" while closing handle", e.what());
    }

    // Wait for websocket to be closed
    _thread.join();

    LOGGER->info("Connection closed");
  }

  _JSONRPCClient(std::string const &uri, std::string const &username, std::string const &password, bool debug)
      : uri(uri), connected(WAITING), debug(debug) {
    _thread = std::thread([&] {
      start(username, password);
    });
  }

  void setHandler(JsonRPCCallback handler) {
    _callbackHandler = handler;
  }

  void start(std::string const &username, std::string const &password) {
    // try {
      connected = WAITING;
      // if (debug) {
      //   // Set logging to be pretty verbose (everything except message payloads)
      //   c.set_access_channels(websocketpp::log::alevel::none);
      //   c.set_error_channels(websocketpp::log::alevel::all);
      //   c.clear_error_channels(websocketpp::log::alevel::frame_payload);
      //   c.clear_error_channels(websocketpp::log::alevel::frame_header);
      // } else {
      //   c.set_access_channels(websocketpp::log::alevel::none);
      // }

      // Initialize ASIO
      // c.init_asio();

      // Register our message handler
      // c.set_message_handler([&](websocketpp::connection_hdl hdl, message_ptr msg) {
      //   this->on_message(hdl, msg);
      // });
      // c.set_open_handler([&](websocketpp::connection_hdl hdl) {
      //   LOGGER->debug("Connection opening...");
      //   {
      //     std::lock_guard<std::mutex> lk(m_open);
      //     this->hdl = hdl;
      //   }
      //   LOGGER->info("Connection opened...");
      //   connected = OPENED;
      //   cv_open.notify_all();
      // });

      // c.set_fail_handler([&](websocketpp::connection_hdl hdl) {
      //   auto connection = c.get_con_from_hdl(hdl);
      //   LOGGER->error(
      //       "RPC failure: local {} (code {}) / remote {} (code {}) / message: {} (code {})",
      //       connection->get_local_close_reason(),
      //       connection->get_local_close_code(),
      //       connection->get_remote_close_reason(),
      //       connection->get_remote_close_code(),
      //       connection->get_ec().message(),
      //       connection->get_ec().value()
      //   );

        // Failure
        connected = FAILURE;
        cv_open.notify_all();

        // Close connection
        LOGGER->info("Closing connection...");
        // connection->close(connection->get_local_close_code(), connection->get_local_close_reason());
        LOGGER->info("Connection closed...");
      // });

      // c.set_close_handler([&](websocketpp::connection_hdl hdl) {
      //   LOGGER->debug("Connection closing...");
      //   {
      //     std::lock_guard<std::mutex> lk(m_open);
      //     // this->hdl.reset();
      //   }
      //   connected = CLOSED;
      //   cv_open.notify_all();
      //   LOGGER->info("Connection closed...");
      // });

      // websocketpp::lib::error_code ec;
      // client::connection_ptr con = c.get_connection(uri, ec);
      // con->append_header("Authorization", "Basic " +
      //     websocketpp::base64_encode(username + ":" + password));
      // if (ec) {
      //   throw std::runtime_error("could not create connection because: " + ec.message());
      // }



      // Note that connect here only requests a connection. No network messages are
      // exchanged until the event loop starts running in the next line.
      // c.connect(con);


      // Start the ASIO io_service run loop
      // this will cause a single connection to be made to the server. c.run()
      // will exit when this connection is closed.
      // c.run();
    // } catch (websocketpp::exception const &e) {
    //   LOGGER->debug("Error while trying to connect: {}", e.what());
    //   connected = FAILURE;
    //   cv_open.notify_all();
    // }
  }

  void send(json const &message) {
    while (true) {
      std::unique_lock<std::mutex> lk(m_open);
      LOGGER->debug("Waiting to send message...");
      cv_open.wait(lk, [&] { return connected != WAITING; });
      if (connected != ConnectionStatus::OPENED) {
        throw exception("Cannot send a message: connection is closed");
      }
      // if (auto ptr = hdl.lock()) {
      //   LOGGER->debug("Sending message...");
      //   c.send(hdl, message.dump(), OpValue::text);
      //   return;
      // }
    }
  }

  // void on_message(websocketpp::connection_hdl hdl, message_ptr msg) {
  //   {
  //     std::lock_guard<std::mutex> lk(m_incoming_message);
  //     auto &payload = msg->get_payload();
  //     json message = json::parse(payload);

  //     if (message["id"].is_null()) {
  //       // Notification
  //       if (this->_callbackHandler) {
  //         this->_callbackHandler(message);
  //       }
  //       return;
  //     }

  //     const std::string mId = message["id"];
  //     RequestId requestId = (RequestId) std::stol(mId.c_str());
  //     auto it = _requests.find(requestId);
  //     if (it == _requests.end()) {
  //       std::cerr << "[error] Unknown request id " << requestId << ", ignoring" << std::endl;
  //       return;
  //     }
  //     it->second->_message = std::move(message);
  //   }
  //   cv_incoming_message.notify_all();
  // }

  /// Sends a message an wait for the answer
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

JsonRPCClient::JsonRPCClient(std::string const &uri, std::string const &username,
                             std::string const &password, bool debug) {
  _client = new _JSONRPCClient(uri, username, password, debug);
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
