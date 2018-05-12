#ifndef EXPERIMAESTRO_LAUNCHERS_OAR_HPP
#define EXPERIMAESTRO_LAUNCHERS_OAR_HPP

#include <xpm/launchers/launchers.hpp>

namespace xpm {

class OARLauncher : public Launcher {
public:
  OARLauncher(std::shared_ptr<Connector> const &connector);
  virtual std::shared_ptr<ProcessBuilder> processBuilder() override;
  virtual std::shared_ptr<ScriptBuilder> scriptBuilder() override;
};

}

#endif