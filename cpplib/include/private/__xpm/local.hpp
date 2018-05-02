/** Classes related to localhost launchers
 */
#ifndef EXPERIMAESTRO__LOCAL_HPP
#define EXPERIMAESTRO__LOCAL_HPP

#include <xpm/launchers.hpp>

namespace xpm {

class LocalProcess;

/** Localhost connector */
class LocalConnector : public Connector {
public:
  virtual ptr<ProcessBuilder> processBuilder() override;
  std::string resolve(Path const & path) override;
};

class LocalProcessBuilder : public ProcessBuilder {
public:
  virtual ptr<Process> start() override;
  friend class LocalProcess;
};

}

#endif 