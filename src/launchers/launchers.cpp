#include <xpm/launchers/launchers.hpp>
#include <__xpm/scriptbuilder.hpp>
#include <xpm/connectors/local.hpp>
#include <xpm/workspace.hpp>

namespace xpm {

void Process::notify(JobState const &) {
  // do nothing
}

Redirect::Redirect() : type(Redirection::INHERIT) {}

Redirect::Redirect(Redirection r) : type(r) {}

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

Launcher::Launcher(ptr<Connector> const & connector) : _connector(connector) {
}
Launcher::~Launcher() {}

ptr<Launcher> Launcher::DEFAULT_LAUNCHER;

ptr<Launcher> Launcher::defaultLauncher() {
  if (!DEFAULT_LAUNCHER) {
    DEFAULT_LAUNCHER = mkptr<DirectLauncher>(mkptr<LocalConnector>());
  }
  return DEFAULT_LAUNCHER;
}

std::string const & Launcher::notificationURL() const {
  return _notificationURL;
}
void Launcher::notificationURL(std::string const & notificationURL) {
  _notificationURL = notificationURL;
}


DirectLauncher::DirectLauncher(ptr<Connector> const & connector) : Launcher(connector) {}

ptr<ProcessBuilder> DirectLauncher::processBuilder() {
  auto builder = connector()->processBuilder();
  builder->environment = environment();
  return builder;
}

std::shared_ptr<ScriptBuilder> DirectLauncher::scriptBuilder() {
  auto builder = mkptr<ShScriptBuilder>();
  builder->notificationURL = notificationURL();
  return builder;
}

ptr<Process> DirectLauncher::check(Job const & job) const {
  auto pidPath = job.pathTo(PID_PATH);
  if (connector()->fileType(pidPath) != FileType::FILE) {
    return nullptr;
  }

  auto istream = connector()->istream(pidPath);
  long pid;
  *istream >> pid;

  return connector()->getProcess(pid);
}


}