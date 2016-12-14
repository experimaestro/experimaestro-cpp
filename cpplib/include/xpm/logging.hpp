//
// Created by Benjamin Piwowarski on 14/12/2016.
//

#ifndef PROJECT_LOGGING_HPP
#define PROJECT_LOGGING_HPP

namespace xpm {
enum LogLevel {
  TRACE,
  DEBUG,
  INFO,
  WARN,
  ERROR,
  CRITICAL,
  OFF
};

/**
* Set log level
*/
void setLogLevel(std::string const &loggername, LogLevel level);

}

#endif //PROJECT_LOGGING_HPP
