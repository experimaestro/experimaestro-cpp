#include <xpm/launchers.hpp>

namespace xpm {

void ProcessBuilder::environment(Environment const & environment) {
  _environment = environment;
}

Launcher::Launcher(Connector::Ptr const & connector) : _connector(connector) {
}

ProcessBuilder::Ptr DirectLauncher::processBuilder() {
  auto builder = connector()->processBuilder();
  builder->environment(environment());
  return builder;
}

}