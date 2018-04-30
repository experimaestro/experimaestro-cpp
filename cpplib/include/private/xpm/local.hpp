/** Classes related to localhost launchers
 */
#ifndef EXPERIMAESTRO_LAUNCHERS_HPP
#define EXPERIMAESTRO_LAUNCHERS_HPP

#include <xpm/launchers.hpp>

namespace xpm {

class LocalProcessBuilder : public ProcessBuilder {
public:
  virtual void start() override;
}

}

#endif 