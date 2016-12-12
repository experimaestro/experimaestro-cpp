//
// Created by Benjamin Piwowarski on 12/12/2016.
//

#ifndef PROJECT_PRIVATE_HPP
#define PROJECT_PRIVATE_HPP

#include <spdlog/spdlog.h>

#define DEFINE_LOGGER(name) namespace { auto LOGGER = ::xpm::logger(name); }

namespace xpm {
  std::shared_ptr<spdlog::logger> logger(std::string const &name);
}

#endif //PROJECT_PRIVATE_HPP
