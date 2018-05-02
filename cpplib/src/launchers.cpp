#include <xpm/launchers.hpp>

namespace xpm {

Redirect::Redirect(Redirection r) : redirection(r) {}

Redirect Redirect::file(std::string const &path) {
  Redirect r(Redirection::FILE);
  r.path = path;
  return r;
}

Redirect Redirect::pipe(PipeFunction function) {
  Redirect r(Redirection::PIPE);
  r.function = function;
  return r;

}
Redirect Redirect::none() {
  return Redirect(Redirection::NONE);
}
Redirect Redirect::inherit() {
  return Redirect(Redirection::INHERIT);
}

Process::~Process() {}

ProcessBuilder::~ProcessBuilder() {}



Connector::~Connector() {}

Launcher::Launcher(ptr<Connector> const & connector) : _connector(connector) {
}
Launcher::~Launcher() {}


ptr<ProcessBuilder> DirectLauncher::processBuilder() {
  auto builder = connector()->processBuilder();
  builder->environment = environment();
  return builder;
}

}