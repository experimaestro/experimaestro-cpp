#include <xpm/launchers.hpp>

namespace xpm {

Process::~Process() {}

ProcessBuilder::~ProcessBuilder() {}

void ProcessBuilder::environment(Environment const & environment) {
  _environment = environment;
}

void ProcessBuilder::command(std::vector<std::string> const & command) {
  _command = command;
}

void ProcessBuilder::workingDirectory(std::string const & workingDirectory) {
  _workingDirectory = workingDirectory;
}

void ProcessBuilder::stdin(ptr<Redirect> const & redirect) {
  _stdin = redirect;
}

void ProcessBuilder::stdout(ptr<Redirect> const & redirect) {
  _stdout = redirect;
}

void ProcessBuilder::stderr(ptr<Redirect> const & redirect) {
  _stderr = redirect;
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