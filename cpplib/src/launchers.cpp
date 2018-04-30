#include <xpm/launchers.hpp>

namespace xpm {

ProcessBuilder::~ProcessBuilder() {}

void ProcessBuilder::environment(Environment const & environment) {
  _environment = environment;
}

void ProcessBuilder::command(std::vector<std::string> const & command) {

}

Connector::~Connector() {}

Launcher::Launcher(ptr<Connector> const & connector) : _connector(connector) {
}

Launcher::~Launcher() {}

ptr<ProcessBuilder> DirectLauncher::processBuilder() {
  auto builder = connector()->processBuilder();
  builder->environment(environment());
  return builder;
}

}