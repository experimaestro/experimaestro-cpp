//
// Created by Benjamin Piwowarski on 13/01/2017.
//

#include "private.hpp"
#include <xpm/xpm.hpp>

namespace xpm {
std::ostream &operator<<(std::ostream &os, const TypeName &c) {
  return os << c.toString();
}
std::ostream &operator<<(std::ostream &os, const Type &c) {
  return os << c.toString();
}
}
