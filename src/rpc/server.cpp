#include <algorithm>
#include <regex>

// --- File

#include <Poco/File.h>
#include <Poco/FileStream.h>
#include <Poco/StreamCopier.h>
#include <Poco/Util/ServerApplication.h>

// --- Net
#include <Poco/Logger.h>
#include <Poco/Net/AcceptCertificateHandler.h>
#include <Poco/Net/ConsoleCertificateHandler.h>
#include <Poco/Net/HTTPRequestHandler.h>
#include <Poco/Net/HTTPRequestHandlerFactory.h>
#include <Poco/Net/HTTPSStreamFactory.h>
#include <Poco/Net/HTTPServer.h>
#include <Poco/Net/HTTPServerParams.h>
#include <Poco/Net/HTTPServerRequest.h>
#include <Poco/Net/HTTPServerResponse.h>
#include <Poco/Net/HTTPStreamFactory.h>
#include <Poco/Net/InvalidCertificateHandler.h>
#include <Poco/Net/KeyConsoleHandler.h>
#include <Poco/Net/NetException.h>
#include <Poco/Net/PrivateKeyPassphraseHandler.h>
#include <Poco/Net/SSLManager.h>
#include <Poco/Net/SecureServerSocket.h>
#include <Poco/Net/WebSocket.h>
#include <Poco/Process.h>
#include <Poco/URI.h>
#include <Poco/Util/Application.h>

#include <csignal>
#include <spdlog/fmt/fmt.h>

#include <__xpm/common.hpp>
#include <xpm/connectors/local.hpp>
#include <xpm/json.hpp>
#include <xpm/rpc/configuration.hpp>
#include <xpm/workspace.hpp>
#include <xpm/rpc/configuration.hpp>
#include <xpm/rpc/server.hpp>
#include <xpm/rpc/servercontext.hpp>

DEFINE_LOGGER("xpm.rpc");

namespace xpm::rpc {

using namespace Poco::Net;
using namespace Poco::Util;

using Poco::Net::HTTPRequestHandler;
using Poco::Net::WebSocketException;
using Poco::Util::Application;

class PageRequestHandler : public HTTPRequestHandler {

public:
  PageRequestHandler(ServerContext &context) : _context(context) {}

  void handleJobNotification(std::string jobId, Poco::URI &uri, HTTPServerResponse &response) {
    auto query = uri.getQueryParameters();
    
    LOGGER->debug("Sending response...");
    std::ostream &ostr = response.send();
    ostr << "OK" << std::endl;
    response.setStatus(Poco::Net::HTTPResponse::HTTP_OK);

    for(auto & p: query) {
      if (p.first == "progress") {
        LOGGER->debug("Notifying listeners...");
        float progress = std::atof(p.second.c_str());
        _context.forEach([&](auto &l) {
          l.send({ { "type", "JOB_UPDATE" }, { "payload", {
            { "jobId", jobId },
            { "progress", progress }
          }}});
        });
      } else if (p.first == "status") {
        _context.jobStatusNotification(jobId, p.second);
      }
    }
    LOGGER->debug("Done");
  }

  void handleRequest(HTTPServerRequest &request, HTTPServerResponse &response) {
    Poco::URI uri(request.getURI());
    
    LOGGER->debug("URI is {}", request.getURI());
    
    static std::regex re_notify(R"(^/notify/([a-zA-Z0-9]+)$)");
    std::smatch matches;
    if (std::regex_search(uri.getPath(), matches, re_notify)) {
      handleJobNotification(matches[1].str(), uri, response);
      return;
    }
    
    Poco::Path base(_context.htdocs() + "/");
    Poco::Path path(base, Poco::Path(uri.getPath().substr(1)));
    Poco::File file(path);
    if (!file.exists()) {
      LOGGER->warn("Page does not exist {}", request.getURI());
      response.setStatus(Poco::Net::HTTPResponse::HTTP_NOT_FOUND);
      response.setContentType("text/html");
      std::ostream &ostr = response.send();
      ostr << "<html><head><title>Page not found</title></head><body>404 : "
              "Page not found</body></html>";
      return;
    }

    auto const ext = path.getExtension();

    if (ext == "html") {
      response.setContentType("text/html");
    } else if (ext == "js") {
      response.setContentType("application/javascript");
    } else if (ext == "css") {
      response.setContentType("text/css");
    } else if (ext == "ico") {
      response.setContentType("image/x-icon");
    } else if (ext == "png") {
      response.setContentType("image/png");
    } else if (ext == "woff2") {
      response.setContentType("font/woff2");
    } else if (ext == "ttf") {
      response.setContentType("font/ttf");
    } else {
      LOGGER->info("Unknown type {}", request.getURI());
      response.setStatus(Poco::Net::HTTPResponse::HTTP_BAD_REQUEST);
      response.setContentType("text/html");
      std::ostream &ostr = response.send();
      ostr << "<html><head><title>Page not found</title></head><body>Bad "
              "request</body></html>";
      return;
    }

    response.setChunkedTransferEncoding(true);
    Poco::FileInputStream s(path.toString());
    std::ostream &ostr = response.send();
    Poco::StreamCopier::copyStream(s, ostr);
  }
protected:
  ServerContext &_context;

};

Emitter::~Emitter() {}

struct WebSocketEmitter : public Emitter {
  std::weak_ptr<WebSocket> ws;

  WebSocketEmitter(std::weak_ptr<WebSocket> ws) : ws(ws) {}

  virtual bool active() override {
    return !ws.expired();
  }

  virtual void send(nlohmann::json const & j) override { 
    auto s = j.dump();
    if (auto _ws = ws.lock()) {
      auto s = j.dump();
      _ws->sendFrame(s.c_str(), s.size());
    } else {
      // TODO: handle errors?
    }
  }
};

/// Handle a WebSocket connection.
class WebSocketRequestHandler : public HTTPRequestHandler, public Emitter {
public:
  WebSocketRequestHandler(ServerContext &context) : _context(context) {}

  std::shared_ptr<WebSocket> _ws;

  void handleRequest(HTTPServerRequest &request, HTTPServerResponse &response) {
    _context.add(this);
    try {
      _ws = mkptr<WebSocket>(request, response);
      auto emitter = mkptr<WebSocketEmitter>(_ws);

      // Set a large timeout
      _ws->setReceiveTimeout(Poco::Timespan(1, 0, 0, 0, 0));

      LOGGER->info("WebSocket connection established.");
      char buffer[32769];
      int flags;
      int n;
      do {
        n = _ws->receiveFrame(buffer, sizeof(buffer) - 1, flags);

        LOGGER->debug(Poco::format("Frame received (length=%d, flags=0x%x).", n,
                                  unsigned(flags)));

        if ((flags & WebSocket::FRAME_OP_TEXT) &&
            (flags & WebSocket::FRAME_FLAG_FIN) && (n > 0)) {
          buffer[n] = 0;
          auto request = nlohmann::json::parse(buffer);
          try {
            auto answer = _context.handle(emitter, request);
            if (!answer.is_null()) {
              LOGGER->info("Sending answer {}", answer);
              send(answer);
            }
          } catch(std::exception & e) {
            send({ { "type", "SERVER_ERROR" }, { "payload", e.what() } });
          }
        }

      } while (n > 0 && (flags & WebSocket::FRAME_OP_BITMASK) !=
                            WebSocket::FRAME_OP_CLOSE);
      _ws = 0;
      LOGGER->info("WebSocket connection closed.");
    } catch (WebSocketException &exc) {
      LOGGER->warn(exc.message());
      switch (exc.code()) {
      case WebSocket::WS_ERR_HANDSHAKE_UNSUPPORTED_VERSION:
        response.set("Sec-WebSocket-Version", WebSocket::WEBSOCKET_VERSION);
        // fallthrough
      case WebSocket::WS_ERR_NO_HANDSHAKE:
      case WebSocket::WS_ERR_HANDSHAKE_NO_VERSION:
      case WebSocket::WS_ERR_HANDSHAKE_NO_KEY:
        response.setStatusAndReason(HTTPResponse::HTTP_BAD_REQUEST);
        response.setContentLength(0);
        response.send();
        break;
      case WebSocket::WS_ERR_PAYLOAD_TOO_BIG:
        break;
      }
    } catch (std::exception &e) {
      LOGGER->warn("Uncaught exception: {}", e.what());
    } catch (...) {
      LOGGER->warn("Uncaught exception");
    }

    _context.remove(this);
  }

  virtual bool active() {
    return _ws.get();
  }

  virtual void send(nlohmann::json const & j) { 
      if (_ws) {
        auto s = j.dump();
        _ws->sendFrame(s.c_str(), s.size());
      }
  }

protected:
  ServerContext &_context;
};

class RequestHandlerFactory : public HTTPRequestHandlerFactory {
public:
  RequestHandlerFactory(ServerContext &context) : _context(context) {}

  HTTPRequestHandler *createRequestHandler(const HTTPServerRequest &request) {
    LOGGER->debug("Request from " + request.clientAddress().toString() + ": " +
                 request.getMethod() + " " + request.getURI() + " " +
                 request.getVersion());

    if (LOGGER->should_log(spdlog::level::debug)) {
      for (HTTPServerRequest::ConstIterator it = request.begin();
          it != request.end(); ++it) {
        LOGGER->debug(it->first + ": " + it->second);
      }

    }

    if (request.find("Upgrade") != request.end() &&
        Poco::icompare(request["Upgrade"], "websocket") == 0) {
      return new WebSocketRequestHandler(_context);

    } else
      return new PageRequestHandler(_context);
  }

protected:
  ServerContext &_context;
};

void Server::start(ServerContext &context, bool pidlocked) {
  _pidfile = pidlocked ? context.pidFile() : nullptr;

  try {
    if (_pidfile) {
      // --- Set lock file
      try {
        if (!_pidfile->createFile()) {
          throw std::runtime_error(fmt::format(
              "Could not create the PID file {} - aborting", _pidfile->path()));

        }
      } catch (...) {
        LOGGER->error("Could not create the PID file {} - aborting",
                      _pidfile->path());
        throw std::runtime_error(fmt::format(
            "Could not create the PID file {} - aborting", _pidfile->path()));
      }

      pidlocked = true;
      {
        Poco::FileOutputStream s(_pidfile->path());
        s << Poco::Process::id() << std::endl;
      }
    }

    // --- Start the web server

    HTTPStreamFactory::registerFactory();

    // --- IF SSL

    // HTTPSStreamFactory::registerFactory();
    // Poco::Net::initializeSSL();
    // Poco::SharedPtr<PrivateKeyPassphraseHandler> ptrConsole =
    //     new KeyConsoleHandler(true);
    // Poco::SharedPtr<InvalidCertificateHandler> pInvalidCertHandler =
    //     new ConsoleCertificateHandler(false);

    // Poco::Net::SSLManager::InvalidCertificateHandlerPtr ptrHandler ( new
    //   Poco::Net::AcceptCertificateHandler(false)
    // );
    // Context::Ptr pContext = new Context(
    //     Context::SERVER_USE, "/Users/bpiwowar/.experimaestro/key.pem",
    //     "/Users/bpiwowar/.experimaestro/certificate.pem", ""
    // );
    // Poco::Net::SSLManager::instance().initializeServer(ptrConsole,
    // pInvalidCertHandler, pContext);

    // --- END IF SSL

    SocketAddress t_osocketaddr(context.host(), context.port());
    // SecureServerSocket svs(t_osocketaddr, 64, pContext);
    ServerSocket svs(t_osocketaddr);

    Poco::Net::HTTPServerParams::Ptr t_pServerParams =
        new Poco::Net::HTTPServerParams;
    t_pServerParams->setMaxThreads(100);
    t_pServerParams->setMaxQueued(100);
    t_pServerParams->setThreadIdleTime(1000);

    _httpserver = std::unique_ptr<HTTPServer>(new HTTPServer(
        new RequestHandlerFactory(context), svs, t_pServerParams));
    _httpserver->start();
    _baseurl = fmt::format("http://{}:{}", context.host(), context.port());
    LOGGER->info("Started server on {}:{}", context.host(), context.port());

  } catch (...) {
    if (_pidfile) {
      _pidfile->remove();
    }
    throw;
  }
}

std::string Server::getNotificationURL() const {
  return _baseurl + "/notify";
}

void Server::terminate() {
  LOGGER->info("Shuting down server");
  _httpserver->stop();
  _httpserver = nullptr;

  if (_pidfile) {
    _pidfile->remove();
    _pidfile = nullptr;
  }
}

void Server::serve(ServerContext &context, bool pidlocked) {
  start(context, pidlocked);
  Poco::Util::ServerApplication::waitForTerminationRequest(); // wait for CTRL-C
                                                              // or kill
  terminate();
}
Server::Server() { 
}
Server::~Server() { 
  terminate(); 
}

/**
 * Check the the server has started, and starts it if not.
 * Returns a JSON client
 */
void Server::client() {
  ConfigurationParameters parameters;
  auto conf = parameters.serverConfiguration();

  auto basepath = Poco::Path::forDirectory(conf.directory);
  auto pidfile = Poco::File(basepath.resolve("server.pid"));
  if (!pidfile.createFile()) {
    LOGGER->error("Could not create the PID file {} - aborting",
                  pidfile.path());
    return;
  }

  // (1) Check that server is on

  // (2) Launch server

  auto builder = LocalConnector().processBuilder();
  builder->detach = true;
  builder->stdout = Redirect::file("");
  builder->stderr = Redirect::file("");
  builder->command = {conf.experimaestro, "server"};
  builder->start();
}

} // namespace xpm::rpc