//
// Created by Benjamin Piwowarski on 12/12/2016.
//

#ifndef PROJECT_PRIVATE_HPP
#define PROJECT_PRIVATE_HPP

#include <spdlog/spdlog.h>
#include <spdlog/fmt/ostr.h>

#define DEFINE_LOGGER(name) namespace { auto LOGGER = ::xpm::logger(name); }

namespace xpm {
std::shared_ptr<spdlog::logger> logger(std::string const &name);

class TypeName;
class Type;

std::ostream &operator<<(std::ostream &os, const TypeName &c);
std::ostream &operator<<(std::ostream &os, const Type &c);

}

#endif //PROJECT_PRIVATE_HPP
