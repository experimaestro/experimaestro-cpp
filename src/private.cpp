//
// Created by Benjamin Piwowarski on 12/12/2016.
//

#include <iostream>

#include <xpm/logging.hpp>
#include <__xpm/common.hpp>
#include <spdlog/sinks/ansicolor_sink.h>

namespace xpm {
namespace {
ptr<spdlog::sinks::ansicolor_stderr_sink_mt> sink;
std::unordered_map<std::string, ptr<spdlog::logger>> loggers;

void initLogging() {
  static bool initialized = false;
  if (!initialized) {
    initialized = true;

    spdlog::set_pattern("[%x %H:%M:%S/%n] %v");
    // spdlog::set_async_mode(8192);

    // auto errsink = spdlog::sinks::stderr_sink_mt::instance();
    sink = std::make_shared<spdlog::sinks::ansicolor_stderr_sink_mt>();
    sink->set_level(spdlog::level::debug);
  }
}

}

ptr<spdlog::logger> logger(std::string const &name) {
  initLogging();

  auto logger = spdlog::get(name);
  if (logger) {
    return logger;
  }

  logger = std::make_shared<spdlog::logger>(name, sink);
  spdlog::register_logger(logger);

  return logger;

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
    default:
      throw std::runtime_error("Unhandled log level"); // safeguard
  }
}
}

void setLogLevel(std::string const &loggername, LogLevel level) {
  initLogging();

  auto l = logger(loggername);
  
  l->set_level(convert(level));
}

}
