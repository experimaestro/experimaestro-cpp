//
// Created by Benjamin Piwowarski on 12/12/2016.
//

#include <regex>
#include <ostream>

#include <asio.hpp>
#include "../private.hpp"

DEFINE_LOGGER("rpc")

namespace xpm {

namespace {
struct Progress {
  float last_progress = -1.;
  std::string hostname;
  std::string path;
  std::string port;

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

  }

  void update(float percentage) {
    if (last_progress == percentage || hostname.empty())
      return;
    last_progress = percentage;

    asio::io_service io_service;
    asio::ip::tcp::resolver resolver(io_service);
    asio::ip::tcp::resolver::query query(hostname, port, asio::ip::resolver_query_base::flags::all_matching);
    asio::ip::tcp::resolver::iterator endpoint_iterator = resolver.resolve(query);

    asio::ip::tcp::socket socket(io_service);
    asio::connect(socket, endpoint_iterator);

    asio::streambuf request;
    std::ostream request_stream(&request);
    request_stream << "GET " << path << "/progress/" << percentage << " HTTP/1.0\r\n";
    request_stream << "Host: " << hostname << "\r\n";
    request_stream << "Accept: */*\r\n";
    request_stream << "Connection: close\r\n\r\n";
    asio::write(socket, request);
  }
};
}

void progress(float percentage) {
  static Progress progress;
  progress.update(percentage);
}
}
