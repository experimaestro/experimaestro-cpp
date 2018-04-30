#include <xpm/launchers.hpp>

namespace xpm {

void ProcessBuilder::environment(Environment const & environment) {
  _environment = environment;
}
void ProcessBuilder::command(std::vector<std::string> const & command) {

}

Connector::~Connector() {}

Launcher::Launcher(Connector::Ptr const & connector) : _connector(connector) {
}

Launcher::~Launcher() {}

ProcessBuilder::Ptr DirectLauncher::processBuilder() {
  auto builder = connector()->processBuilder();
  builder->environment(environment());
  return builder;
}

}