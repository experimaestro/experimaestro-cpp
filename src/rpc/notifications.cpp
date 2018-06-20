//
// Created by Benjamin Piwowarski on 12/12/2016.
//

#include <regex>
#include <ostream>
#include <sstream>
#include <chrono>
#include <condition_variable>

#define ASIO_STANDALONE
#include <asio.hpp>
#include <__xpm/common.hpp>

DEFINE_LOGGER("rpc")

namespace xpm {

using namespace asio::ip;

namespace {

struct Progress {
  // Progress
  typedef uint64_t ProgressType;
  static constexpr ProgressType MAX_PROGRESS = std::numeric_limits<ProgressType>::max();

  std::atomic<ProgressType> progress;
  ProgressType last_progress = 0;

  // TCP-IP related parameters
  std::string hostname;
  std::string path;
  std::string port;
  asio::io_service io_service;
  tcp::resolver::iterator endpoint_iterator;

  /// Last update time
  std::chrono::time_point<std::chrono::system_clock> last_update_time;

  // Threshold for reporting something
  uint64_t threshold;

  // Threshold for reporting something (on logging stream)
  uint64_t logging_threshold;

  // No more than one update every 5 seconds for changes above the threshold
  std::chrono::milliseconds time_threshold =  std::chrono::seconds(5);

  // Notifier thread
  std::thread notifierThread;
  std::mutex mx;
  std::condition_variable cv;

  /// Converts a threshold in % (range 0-1) into our own scale
  static ProgressType convertThreshold(double threshold) {
    auto _threshold = ProgressType(threshold * std::numeric_limits<ProgressType>::max());
    if (_threshold == 0) {
      return 1;
    }
    return _threshold;
  }

  Progress(double threshold = 0.01, double logging_threshold = 0.05)  {
    // Sets the thresholds
    this->threshold = convertThreshold(threshold);
    this->logging_threshold = convertThreshold(logging_threshold);

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

    // Creates a new thread that will run independently of the rest
    notifierThread = std::thread(&Progress::tick, this);
    notifierThread.detach();
  }

  void tick() {
    // First tick
    LOGGER->debug("First XPM notification...");
    notify(0);

    while (true) {
      std::unique_lock<std::mutex> lk(mx);

      // Wait for a maximum of time_threshold - or if enough progress has been made
      cv.wait_for(lk, time_threshold,  [&] {
        return last_progress - progress > threshold;
      });

      // just outputs
      bool b = last_progress - progress > threshold;
      bool logging_b = last_progress - progress > logging_threshold;
      last_progress = progress;

      float value = float(last_progress) / MAX_PROGRESS;
      if (hostname.empty()) {
        LOGGER->info("Progress: {} %", value * 100);
        return;
      } else {
        if (logging_b) {
          LOGGER->info("Progress: {} %", value * 100); 
        } else {
          LOGGER->debug("Notify progress {} [{}]...", value * 100, b);
        }
      }

      notify(value);

    }
  }

  /**
   * Sends progress information to XPM
   * @param value The current progress value (or -1)
   */
  void notify(float value) {
    try {
        tcp::socket socket(io_service);
        connect(socket, endpoint_iterator);

        asio::streambuf request;
        std::ostream request_stream(&request);
        request_stream << "GET " << path << "/progress/" << value << " HTTP/1.0\r\n";
        request_stream << "Host: " << hostname << "\r\n";
        request_stream << "Accept: */*\r\n";
        request_stream << "Connection: close\r\n\r\n";
        write(socket, request);
      } catch(std::exception &e) {
        LOGGER->info("Caught exception while reporting progress: {}", e.what());
      }
  }

  void update(float percentage) {
    if (percentage < 0) return;
    if (percentage > 1) percentage = 1;

    // Sets the new progress
    progress = ProgressType(MAX_PROGRESS * percentage);

    // Just log if no host to notify
    if (hostname.empty()) {
      if (last_progress - progress > logging_threshold) {
        last_progress = progress;
        float value = float(last_progress) / MAX_PROGRESS;
        LOGGER->info("Progress: {:1f} %", value * 100); 
      }
    }

    // Notify the notifier thread
    else if (last_progress - progress > threshold) {
      cv.notify_all();
    }
  }
};
}

void progress(float percentage) {
  static Progress progress;
  progress.update(percentage);
}
}
