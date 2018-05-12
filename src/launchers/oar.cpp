#include <xpm/launchers/oar.hpp>
#include <__xpm/scriptbuilder.hpp>

namespace xpm {

class OARProcessBuilder : public ProcessBuilder {
  virtual std::shared_ptr<Process> start() override {
      NOT_IMPLEMENTED();
  }
};

OARLauncher::OARLauncher(std::shared_ptr<Connector> const &connector)
    : Launcher(connector) {}

std::shared_ptr<ProcessBuilder> OARLauncher::processBuilder() {
    return std::make_shared<OARProcessBuilder>();
}

std::shared_ptr<ScriptBuilder> OARLauncher::scriptBuilder() {
    return std::make_shared<ShScriptBuilder>();
}

} // namespace xpm