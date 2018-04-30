/** Classes related to localhost launchers
 */
#ifndef EXPERIMAESTRO__LOCAL_HPP
#define EXPERIMAESTRO__LOCAL_HPP

#include <xpm/launchers.hpp>

namespace xpm {

class LocalProcess;

class LocalProcessBuilder : public ProcessBuilder {
public:
  virtual ptr<Process> start() override;
  friend class LocalProcess;
};

}

#endif 