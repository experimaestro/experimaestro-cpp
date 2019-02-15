//
// Created by Benjamin Piwowarski on 12/12/2016.
//

#include <regex>
#include <ostream>
#include <sstream>
#include <chrono>
#include <condition_variable>

#include <Poco/Net/HTTPRequest.h>
#include <Poco/Net/HTTPMessage.h>
#include <Poco/Net/HTTPResponse.h>
#include <Poco/Net/HTTPClientSession.h>

#include <__xpm/common.hpp>

DEFINE_LOGGER("rpc")

namespace xpm {

namespace {

struct Progress {
  // Progress
  typedef uint64_t ProgressType;
  static constexpr ProgressType MAX_PROGRESS = std::numeric_limits<ProgressType>::max();

  std::atomic<ProgressType> progress;
  ProgressType last_progress = 0;
  ProgressType last_logging_progress = 0;

  // TCP-IP related parameters
  std::string hostname;
  std::string path;
  int port;

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
      port = std::atoi(match[2].str().c_str());
      path = match[3];
      LOGGER->info("Notifications: host {}, port {}, path {}", hostname, port, path);
    }

    // Resolves hostname
    // tcp::resolver resolver(io_service);
    // tcp::resolver::query query(hostname, port, asio::ip::resolver_query_base::flags::all_matching);
    // endpoint_iterator = resolver.resolve(query);

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
      bool b = cv.wait_for(lk, time_threshold,  [&] {
        return progress - last_progress > threshold;
      });

      // Notify
      last_progress = progress;
      float value = float(last_progress) / MAX_PROGRESS;
      LOGGER->debug("Notify progress {} [{}]...", value * 100, b);

      notify(value);

    }
  }

  /**
   * Sends progress information to XPM
   * @param value The current progress value (or -1)
   */
  void notify(float value) {
    try {

        Poco::Net::HTTPClientSession session(hostname, port);
        session.setTimeout(Poco::Timespan(1, 0)); // 1s timeout
        Poco::Net::HTTPRequest request(Poco::Net::HTTPRequest::HTTP_GET, 
           fmt::format("{}?progress={:.3f}", path, value), Poco::Net::HTTPMessage::HTTP_1_0);
        // Poco::Net::HTTPResponse response;
        session.sendRequest(request);
      } catch(std::exception &e) {
        LOGGER->info("Caught exception while reporting progress on http://{}:{}{}?progress={:.3f}: {}", hostname, port, path, value, e.what());
      }
  }

  void update(float percentage) {
    if (percentage < 0) return;
    if (percentage > 1) percentage = 1;

    // Sets the new progress
    progress = ProgressType(MAX_PROGRESS * percentage);


    // Notify the notifier thread
    if (progress - last_progress > threshold) {
      cv.notify_all();
    }

    // Just log if no host to notify
    if ((progress - last_logging_progress) > logging_threshold) {
      last_logging_progress = progress;
      float value = float(progress) / MAX_PROGRESS;
      LOGGER->info("Progress: {:.2f} %", value * 100); 
    }

  }
};
}

void progress(float percentage) {
  static Progress progress;
  progress.update(percentage);
}
}
