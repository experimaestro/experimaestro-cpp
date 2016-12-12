//
// Created by Benjamin Piwowarski on 12/12/2016.
//

#include <iostream>
#include "private.hpp"

namespace xpm {
  namespace {
    std::shared_ptr<spdlog::sinks::ansicolor_sink> sink;
    std::unordered_map<std::string, std::shared_ptr<spdlog::logger>> loggers;

    void initLogging() {
      static bool initialized = false;
      if (!initialized) {
        initialized = true;
        spdlog::set_async_mode(8192);

        auto errsink = spdlog::sinks::stderr_sink_mt::instance();
        sink = std::make_shared<spdlog::sinks::ansicolor_sink>(errsink);
        sink->set_level(spdlog::level::debug);
      }
    }


  }

  std::shared_ptr<spdlog::logger> logger(std::string const &name) {
    initLogging();

    auto iterator = loggers.find(name);
    if (iterator != loggers.end())
      return iterator->second;
    auto v = loggers[name] = std::make_shared<spdlog::logger>(name, sink);
    v->set_level(spdlog::level::debug);
    return v;

  }

}
