#include <algorithm>

// --- File
#include <Poco/File.h>
#include <Poco/FileStream.h>
#include <Poco/StreamCopier.h>
#include <Poco/Util/ServerApplication.h>

// --- SQL

#include <Poco/Data/SQLite/Connector.h>
#include <Poco/Data/Session.h>

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
#include <Poco/URI.h>
#include <Poco/Util/Application.h>
#include <Poco/Process.h>

#include <csignal>
#include <spdlog/fmt/fmt.h>

// #include <xpm/json.hpp>
#include <__xpm/common.hpp>
#include <xpm/rpc/configuration.hpp>
#include <xpm/rpc/server.hpp>
#include <xpm/connectors/local.hpp>

DEFINE_LOGGER("xpm");

namespace xpm::rpc {

using namespace Poco::Net;
using namespace Poco::Util;

using Poco::Net::HTTPRequestHandler;
using Poco::Net::WebSocketException;
using Poco::Util::Application;

class PageRequestHandler : public HTTPRequestHandler {
public:
  void handleRequest(HTTPServerRequest &request, HTTPServerResponse &response) {
    Poco::URI uri(request.getURI());

    Poco::Path path(Poco::Path("app/"), Poco::Path(uri.getPath().substr(1)));
    LOGGER->info("Path is {}", path.absolute().toString());
    Poco::File file(path);

    if (!file.exists()) {
      LOGGER->info("Page does not exist {}", request.getURI());
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
};

/// Handle a WebSocket connection.
class WebSocketRequestHandler : public HTTPRequestHandler {
public:
  void handleRequest(HTTPServerRequest &request, HTTPServerResponse &response) {
    try {
      WebSocket ws(request, response);
      LOGGER->info("WebSocket connection established.");
      char buffer[1024];
      int flags;
      int n;
      do {
        LOGGER->info("Reading...\n");
        n = ws.receiveFrame(buffer, sizeof(buffer), flags);
        LOGGER->info(Poco::format("Frame received (length=%d, flags=0x%x).", n,
                                  unsigned(flags)));
        ws.sendFrame("Hello", 5);
        ws.sendFrame("Yop", 3);
      } while (n > 0 && (flags & WebSocket::FRAME_OP_BITMASK) !=
                            WebSocket::FRAME_OP_CLOSE);
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
      }
    }
  }
};

class RequestHandlerFactory : public HTTPRequestHandlerFactory {
public:
  HTTPRequestHandler *createRequestHandler(const HTTPServerRequest &request) {
    LOGGER->info("Request from " + request.clientAddress().toString() + ": " +
                 request.getMethod() + " " + request.getURI() + " " +
                 request.getVersion());

    for (HTTPServerRequest::ConstIterator it = request.begin();
         it != request.end(); ++it) {
      LOGGER->info(it->first + ": " + it->second);
    }

    if (request.find("Upgrade") != request.end() &&
        Poco::icompare(request["Upgrade"], "websocket") == 0) {
      return new WebSocketRequestHandler;

    } else
      return new PageRequestHandler;
  }
};

namespace {
int DB_VERSION = 1;
}

int Server::serve(bool pidlocked) {
  ConfigurationParameters parameters;
  auto conf = parameters.serverConfiguration();
  auto basepath = Poco::Path::forDirectory(conf.directory);
  auto pidfile = Poco::File(basepath.resolve("server.pid"));

  try {
    if (!pidlocked) {
      // --- Set lock file
      if (!pidfile.createFile()) {
        LOGGER->error("Could not create the PID file {} - aborting", pidfile.path());
        return 1;
      }

      pidlocked = true;
      {
        Poco::FileOutputStream s(pidfile.path());
        s << Poco::Process::id() << std::endl;
      }
    }

    try {
      // --- SQLite connection

      using namespace Poco::Data::Keywords;

      auto sqlitepath = basepath.resolve("data.sqlite");
      Poco::Data::SQLite::Connector::registerConnector();
      session = std::make_unique<Poco::Data::Session>(
          "SQLite", sqlitepath.absolute().toString());

      *session << "CREATE TABLE IF NOT EXISTS Config (key VARCHAR(30) PRIMARY "
                  "KEY, value VARCHAR NOT NULL)",
          now;

      Poco::Data::Statement select(*session);
      int version = 0;
      *session << "PRAGMA foreign_keys = ON", now;
      select << "SELECT Value FROM Config WHERE key='version'", into(version),
          now;
      LOGGER->info("Database version is {}", version);

      switch (version) {
      case 0: {
        // New database
        *session << R"SQL(
          CREATE TABLE IF NOT EXISTS Auth (
            token VARCHAR(255) PRIMARY KEY, 
            validity DATETIME NOT NULL
          );
          
          CREATE TABLE IF NOT EXISTS Experiment (
            id INTEGER PRIMARY KEY,
            name VARCHAR(255) NOT NULL,
            timestamp DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
            CONSTRAINT unique_experiment UNIQUE(name, timestamp)
          );

          CREATE TABLE IF NOT EXISTS Task (
            id INTEGER PRIMARY KEY,
            experiment_id INTEGER NOT NULL,
            CONSTRAINT valid_experiment FOREIGN KEY (experiment_id) REFERENCES Experiment(id)
          );

          CREATE TABLE IF NOT EXISTS Tag (
            task_id INTEGER NOT NULL REFERENCES Task(id),
            key VARCHAR(255) NOT NULL,
            value JSON NOT NULL,
            CONSTRAINT tag_reference PRIMARY KEY(task_id, key)
          );

          CREATE TABLE IF NOT EXISTS Token (
            id INTEGER NOT NULL PRIMARY KEY,
            key VARCHAR(255) NOT NULL,
            capacity int NOT NULL,
            value int NOT NULL,
            CONSTRAINT unique_token UNIQUE(key)
          );

          CREATE TABLE IF NOT EXISTS LockedToken (
            token_id INTEGER NOT NULL REFERENCES Token(id),
            task_id INTEGER NOT NULL REFERENCES Task(id),
            value INTEGER NOT NULL,
            CONSTRAINT unique_lockedtoken UNIQUE(token_id, task_id)
          );

        )SQL",
            now;

        // Poco::Data::Statement update(*session);
        *session << "INSERT OR REPLACE INTO Config(key, value) VALUES "
                    "('version', ?)",
            use(DB_VERSION), now;
        break;
      }
      }

    } catch (Poco::Data::DataException &e) {
      LOGGER->error("Erreur while updating database: {}", e.displayText());
      throw;
    }

    LOGGER->info("Database update to version {}", DB_VERSION);

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

    SocketAddress t_osocketaddr(conf.host, conf.port);
    // SecureServerSocket svs(t_osocketaddr, 64, pContext);
    ServerSocket svs(t_osocketaddr);

    Poco::Net::HTTPServerParams::Ptr t_pServerParams =
        new Poco::Net::HTTPServerParams;
    t_pServerParams->setMaxThreads(100);
    t_pServerParams->setMaxQueued(100);
    t_pServerParams->setThreadIdleTime(1000);

    HTTPServer httpserver(new RequestHandlerFactory, svs, t_pServerParams);
    httpserver.start();
    LOGGER->info("Started server on {}:{}", conf.host, conf.port);
    waitForTerminationRequest(); // wait for CTRL-C or kill
    LOGGER->info("Shuting down server on {}:{}", conf.host, conf.port);

    httpserver.stop();
    pidfile.remove(); 
  } catch(...) {
    if (pidlocked) {
      pidfile.remove(); 
    }
  }
  return Application::EXIT_OK;
}

void Server::start(bool locked) {
  Server().serve(locked);
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
    LOGGER->error("Could not create the PID file {} - aborting", pidfile.path());
    return;
  }

  // (1) Check that server is on

  // (2) Launch server


  auto builder = LocalConnector().processBuilder();
  builder->detach = true;
  builder->stdout = Redirect::file("");
  builder->stderr = Redirect::file("");
  builder->command = { conf.experimaestro, "server" };
  builder->start();
}

} // namespace xpm::rpc