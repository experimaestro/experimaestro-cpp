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
  virtual std::shared_ptr<ProcessBuilder> processBuilder() const override;
  std::string resolve(Path const & path) const override;

  std::unique_ptr<std::ostream> ostream(Path const & path) const override;
  std::unique_ptr<std::istream> istream(Path const & path) const override;
};

class LocalProcessBuilder : public ProcessBuilder {
public:
  virtual std::shared_ptr<Process> start() override;
  friend class LocalProcess;
};

}

#endif 