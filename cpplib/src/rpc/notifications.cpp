//
// Created by Benjamin Piwowarski on 12/12/2016.
//

#include <regex>
#include <ostream>
#include <sstream>

#include <asio.hpp>
#include "../private.hpp"

DEFINE_LOGGER("rpc")

namespace xpm {

using namespace asio::ip;

namespace {
struct Progress {
  float last_progress = -1.;
  std::string hostname;
  std::string path;
  std::string port;
  asio::io_service io_service;
  tcp::resolver::iterator endpoint_iterator;

  Progress() {
    const char *notification_url = getenv("XPM_NOTIFICATION_URL");
    if (!notification_url) {
      LOGGER->warn("XPM_NOTIFICATION_URL environment variable is not defined");
      return;
    }

    std::regex re_http(R"(^http://([\w\d\.-]+):(\d+)(/.*)$)",
                       std::regex_constants::ECMAScript | std::regex_constants::icase);

    std::smatch match;
    if (std::regex_search(std::string(notification_url), match, re_http)) {
      hostname = match[1];
      port = match[2];
      path = match[3];
      LOGGER->info("Notifications: host {}, port {}, path {}", hostname, port, path);
    }

    // Resolves hostname
    tcp::resolver resolver(io_service);
    tcp::resolver::query query(hostname, port, asio::ip::resolver_query_base::flags::all_matching);
    endpoint_iterator = resolver.resolve(query);
  }

  void update(float percentage) {
    if (last_progress == percentage || hostname.empty())
      return;
    last_progress = percentage;

    try {
      asio::ip::tcp::socket socket(io_service);
      asio::connect(socket, endpoint_iterator);

      asio::streambuf request;
      std::ostream request_stream(&request);
      request_stream << "GET " << path << "/progress/" << percentage << " HTTP/1.0\r\n";
      request_stream << "Host: " << hostname << "\r\n";
      request_stream << "Accept: */*\r\n";
      request_stream << "Connection: close\r\n\r\n";
      asio::write(socket, request);
    } catch(std::exception &e) {
      LOGGER->info("Caught exception while reporting progress: {}", e.what());
    }
  }
};
}

void progress(float percentage) {
  static Progress progress;
  progress.update(percentage);
}
}
