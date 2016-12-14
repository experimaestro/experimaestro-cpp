//
// Created by Benjamin Piwowarski on 12/12/2016.
//

#include <iostream>

#include <xpm/logging.hpp>
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
  return v;

}

namespace {
spdlog::level::level_enum convert(LogLevel l) {
  switch (l) {
    case LogLevel::TRACE:return spdlog::level::trace;
    case LogLevel::DEBUG:return spdlog::level::debug;
    case LogLevel::INFO:return spdlog::level::info;
    case LogLevel::WARN:return spdlog::level::warn;
    case LogLevel::ERROR:return spdlog::level::err;
    case LogLevel::CRITICAL:return spdlog::level::critical;
    case LogLevel::OFF:return spdlog::level::off;
  }
}
}

void setLogLevel(std::string const &loggername, LogLevel level) {
  auto l = logger(loggername);
  l->set_level(convert(level));
}

}
