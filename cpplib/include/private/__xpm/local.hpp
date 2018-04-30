/** Classes related to localhost launchers
 */
#ifndef EXPERIMAESTRO__LOCAL_HPP
#define EXPERIMAESTRO__LOCAL_HPP

#include <xpm/launchers.hpp>

namespace TinyProcessLib { class Process; }

namespace xpm {

class LocalProcessBuilder : public ProcessBuilder {
public:
  virtual void start() override;
  
private:
  std::unique_ptr<TinyProcessLib::Process> _process;
};

}

#endif 