//
// Created by Benjamin Piwowarski on 12/12/2016.
//

#include <regex>
#include <ostream>
#include <sstream>
#include <chrono>

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

  // Threshold for reporting something
  double threshold = 0.01;
  /// Last update time
  std::chrono::time_point<std::chrono::system_clock> last_update_time;
  // No more than one update every 5 seconds for changes above the threshold
  std::chrono::milliseconds time_threshold =  std::chrono::seconds(5);


  Progress() {
    const char *notification_url = getenv("XPM_NOTIFICATION_URL");
    if (!notification_url) {
      LOGGER->warn("XPM_NOTIFICATION_URL environment variable is not defined");
      return;
    }

    std::regex re_http(R"(^http://([\w\d\.-]+):(\d+)(/.*)$)",
                       std::regex_constants::ECMAScript | std::regex_constants::icase);

    std::smatch match;
    std::string _notification_url(notification_url);
    if (std::regex_search(_notification_url, match, re_http)) {
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
    // No change or no host, do not notify
    if (last_progress == percentage)
      return;

    // Threshold on time or progress
    auto now = std::chrono::system_clock::now();
    if (std::abs(last_progress - percentage) < threshold && (last_update_time - now) < time_threshold)
      return;

    last_progress = percentage;
    last_update_time = now;

    // just outputs
    if (hostname.empty()) {
      LOGGER->info("Progress: {} %", last_progress * 100);
      return;
    }

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
