//
// Created by Benjamin Piwowarski on 09/11/2016.
//

#include <mutex>
#include <condition_variable>

#include <xpm/jsonrpcclient.hpp>

#define ASIO_STANDALONE

#include <websocketpp/config/asio_no_tls_client.hpp>
#include <websocketpp/client.hpp>

using nlohmann::json;
typedef websocketpp::frame::opcode::value OpValue;

typedef websocketpp::client<websocketpp::config::asio_client> client;

using websocketpp::lib::placeholders::_1;
using websocketpp::lib::placeholders::_2;
using websocketpp::lib::bind;

// pull out the type of messages sent by our config
typedef websocketpp::config::asio_client::message_type::ptr message_ptr;

namespace xpm {

typedef unsigned long RequestId;

std::string JSONRPC_VERSION = "2.0";

class _JSONRPCClient {
 public:
  /// Counter for JSON request
  RequestId _currentRequestId;

  /// Client
  client c;

  std::string uri;

  /// The connection
  websocketpp::connection_hdl hdl;

  // Opened connection
  std::mutex m_open;
  std::condition_variable cv_open;
  bool connected;

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
    c.close(hdl, 0, "Finished");

    // Wait for close signal
    std::unique_lock<std::mutex> lk(m_open);
    cv_open.wait(lk, [&] { return hdl.expired(); });

    // Wait for websocket to be closed
    _thread.join();

    std::cerr << "Connection closed";
  }

  _JSONRPCClient(std::string const &uri, std::string const &username, std::string const &password, bool debug)
      : uri(uri), debug(debug) {
    _thread = std::thread([&] {
      start(username, password);
    });
  }

  void setHandler(JsonRPCCallback handler) {
    _callbackHandler = handler;
  }

  void start(std::string const &username, std::string const &password) {
    try {
      connected = false;
      if (debug) {
        // Set logging to be pretty verbose (everything except message payloads)
        c.set_access_channels(websocketpp::log::alevel::none);
        c.set_error_channels(websocketpp::log::alevel::all);
        c.clear_error_channels(websocketpp::log::alevel::frame_payload);
        c.clear_error_channels(websocketpp::log::alevel::frame_header);
      } else {
        c.set_access_channels(websocketpp::log::alevel::none);
      }

      // Initialize ASIO
      c.init_asio();

      // Register our message handler
      c.set_message_handler([&](websocketpp::connection_hdl hdl, message_ptr msg) {
        this->on_message(hdl, msg);
      });
      c.set_open_handler([&](websocketpp::connection_hdl hdl) {
        std::cerr << "Connection opening..." << std::endl;
        {
          std::lock_guard<std::mutex> lk(m_open);
          this->hdl = hdl;
        }
        std::cerr << "Connection opened, notifying" << std::endl;
        connected = true;
        cv_open.notify_all();
      });

      c.set_fail_handler([&](websocketpp::connection_hdl hdl) {
        auto connection = c.get_con_from_hdl(hdl);
        std::cerr << "[ERROR/RPC] " << connection->get_state() << " "
                  << connection->get_local_close_code() << " "
                  << connection->get_local_close_reason() << " "
                  << connection->get_remote_close_code() << " "
                  << connection->get_remote_close_reason() << ": "
                  << connection->get_ec() << " - " << connection->get_ec().message() << std::endl;

        // Close connection
        connection->close(connection->get_local_close_code(), connection->get_local_close_reason());
      });

      c.set_close_handler([&](websocketpp::connection_hdl hdl) {
        std::cerr << "Connection closing..." << std::endl;
        {
          std::lock_guard<std::mutex> lk(m_open);
          this->hdl.reset();
        }
        std::cerr << "Connection closed, notifying" << std::endl;
        cv_open.notify_all();
        connected = false;
      });

      websocketpp::lib::error_code ec;
      client::connection_ptr con = c.get_connection(uri, ec);
      con->append_header("Authorization", "Basic " +
          websocketpp::base64_encode(username + ":" + password));
      if (ec) {
        throw std::runtime_error("could not create connection because: " + ec.message());
      }



      // Note that connect here only requests a connection. No network messages are
      // exchanged until the event loop starts running in the next line.
      c.connect(con);


      // Start the ASIO io_service run loop
      // this will cause a single connection to be made to the server. c.run()
      // will exit when this connection is closed.
      c.run();
    } catch (websocketpp::exception const &e) {
      throw std::runtime_error(e.what());
    }
  }

  void send(json const &message) {
    while (true) {
      std::unique_lock<std::mutex> lk(m_open);
      cv_open.wait(lk, [&] { return connected; });
      if (auto ptr = hdl.lock()) {
        c.send(hdl, message.dump(), OpValue::text);
        return;
      }
    }
  }

  void on_message(websocketpp::connection_hdl hdl, message_ptr msg) {
    {
      std::lock_guard<std::mutex> lk(m_incoming_message);
      auto &payload = msg->get_payload();
      json message = json::parse(payload);

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
  std::cerr << "Closing connection...\n";
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
