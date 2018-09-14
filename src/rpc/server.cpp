#include <Poco/Net/HTTPRequestHandler.h>
#include <Poco/Net/HTTPRequestHandlerFactory.h>
#include <Poco/Net/HTTPServer.h>
#include <Poco/Net/HTTPServerRequest.h>
#include <Poco/Net/HTTPServerResponse.h>
#include <Poco/Net/HTTPStreamFactory.h>
#include <Poco/Net/HTTPSStreamFactory.h>
#include <Poco/Net/SSLManager.h>
#include <Poco/Net/WebSocket.h>
#include <Poco/Net/NetException.h>
#include <Poco/Net/HTTPServerParams.h>
#include <Poco/Net/PrivateKeyPassphraseHandler.h>
#include <Poco/Net/InvalidCertificateHandler.h>
#include <Poco/Util/ServerApplication.h>
#include <Poco/Net/SecureServerSocket.h>
#include <Poco/Net/KeyConsoleHandler.h>
#include <Poco/Net/ConsoleCertificateHandler.h>
#include <Poco/Util/Application.h>

#include <xpm/rpc/configuration.hpp>

namespace xpm::rpc {

using namespace Poco::Net;
using namespace Poco::Util;

using Poco::Net::HTTPRequestHandler;
using Poco::Util::Application;
using Poco::Net::WebSocketException;

class PageRequestHandler : public HTTPRequestHandler {
public:
  void handleRequest(HTTPServerRequest &request, HTTPServerResponse &response) {

  }
};

/// Handle a WebSocket connection.
class WebSocketRequestHandler : public HTTPRequestHandler {
public:
  void handleRequest(HTTPServerRequest &request, HTTPServerResponse &response) {
    Application &app = Application::instance();
    try {
      WebSocket ws(request, response);
      app.logger().information("WebSocket connection established.");
      char buffer[1024];
      int flags;
      int n;
      do {
        n = ws.receiveFrame(buffer, sizeof(buffer), flags);
        app.logger().information(Poco::format(
            "Frame received (length=%d, flags=0x%x).", n, unsigned(flags)));
        ws.sendFrame(buffer, n, flags);
      } while (n > 0 && (flags & WebSocket::FRAME_OP_BITMASK) !=
                            WebSocket::FRAME_OP_CLOSE);
      app.logger().information("WebSocket connection closed.");
    } catch (WebSocketException &exc) {
      app.logger().log(exc);
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
    Application &app = Application::instance();
    app.logger().information("Request from " +
                             request.clientAddress().toString() + ": " +
                             request.getMethod() + " " + request.getURI() +
                             " " + request.getVersion());

    for (HTTPServerRequest::ConstIterator it = request.begin();
         it != request.end(); ++it) {
      app.logger().information(it->first + ": " + it->second);
    }

    if (request.find("Upgrade") != request.end() &&
        Poco::icompare(request["Upgrade"], "websocket") == 0)
      return new WebSocketRequestHandler;
    else
      return new PageRequestHandler;
  }
};

class Server : public ServerApplication {
public:
  int serve() {
    HTTPStreamFactory::registerFactory();
    HTTPSStreamFactory::registerFactory();

    Poco::Net::initializeSSL();
    Poco::SharedPtr<PrivateKeyPassphraseHandler> ptrConsole =
        new KeyConsoleHandler(true);
    Poco::SharedPtr<InvalidCertificateHandler> pInvalidCertHandler =
        new ConsoleCertificateHandler(false);

    // Poco::Net::SSLManager::InvalidCertificateHandlerPtr ptrHandler ( new
    // Poco::Net::AcceptCertificateHandler(false) );
    Context::Ptr pContext = new Context(
        Context::SERVER_USE, "my-key.pem", "src/my-cert.pem", "",
        Context::VERIFY_RELAXED, 9, false, "ALL:!ADH:!LOW:!EXP:!MD5:@STRENGTH");
    Poco::Net::SSLManager::instance().initializeServer(
        ptrConsole, pInvalidCertHandler, pContext);

    SocketAddress t_osocketaddr("localhost:8080");

    SecureServerSocket svs(t_osocketaddr, 64, pContext);
    Poco::Net::HTTPServerParams::Ptr t_pServerParams = new Poco::Net::HTTPServerParams;
    t_pServerParams->setMaxThreads(100);
    t_pServerParams->setMaxQueued(100);
    t_pServerParams->setThreadIdleTime(1000);

    HTTPServer srv(new RequestHandlerFactory, svs, t_pServerParams);
    srv.start();
    // HTTPServer s(nullptr, ServerSocket(9090), new HTTPServerParams);
    // s.start();

    waitForTerminationRequest(); // wait for CTRL-C or kill

    srv.stop();

    return Application::EXIT_OK;
  }

  /**
   * Check the the server has started, and starts it if not
   */
  static void ensureStarted() {
    // (1) Check that server is on
    ConfigurationParameters parameters;
    auto conf = parameters.serverConfiguration();
  }
};
} // namespace xpm::rpc