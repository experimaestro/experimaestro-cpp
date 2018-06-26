//
// Created by Benjamin Piwowarski on 13/01/2017.
//

#include <xpm/common.hpp>
#include <xpm/task.hpp>
#include <xpm/workspace.hpp>
#include <xpm/launchers/launchers.hpp>

#include <__xpm/common.hpp>

DEFINE_LOGGER("xpm")

namespace xpm {

using nlohmann::json;

bool Task::_running = false;

Task::Task(Typename const &typeName, ptr<Type> const &type) : _identifier(typeName), _type(type) {
}

Task::Task(ptr<Type> const &type) : _identifier(type->name()), _type(type) {
}

Typename Task::name() const { return _type->name(); }

void Task::submit(ptr<Workspace> const & _workspace,
  ptr<Launcher> const & _launcher,
  ptr<Parameters> const & sv
) const {
  LOGGER->info("Preparing job");

  // Set task
  sv->task(const_cast<Task*>(this)->shared_from_this());

  auto workspace = _workspace ? _workspace : Workspace::currentWorkspace();
  if (!workspace) {
    throw argument_error("No workspace was created");
  }
  auto &launcher = _launcher ? _launcher : Launcher::defaultLauncher();

  // Set locator
  auto svlocator = getPathGenerator()->generate(GeneratorContext(*workspace, sv));

  // Validate and seal the task sv
  LOGGER->debug("Configuring task");
  sv->configure(*workspace);

  // Get generated directory as locator

  // Set the command parameters
  auto job = mkptr<CommandLineJob>(svlocator->asPath(), launcher, _commandLine);
  job->parameters(sv);
  job->init();
  sv->job(job);

  workspace->submit(job);
  LOGGER->debug("Submitting job {} (id {}) {}", job->locator(), job->getId(), sv->toJsonString());
}

void Task::commandline(ptr<CommandLine> const & command) {
  _commandLine = command;
}

Typename const &Task::identifier() const {
  return _identifier;
}

std::string Task::toString() const {
  return "Task '" + identifier().toString() + "' (output '" + name().toString() + "')";
}


nlohmann::json Task::toJson() {
  auto j = json::object();
  j["identifier"] = _identifier.toString();
  j["type"] = _type->name().toString();
  j["command"] = _commandLine->toJson();
  return j;
}

Type::Ptr Task::type() {
  return _type;
}
ptr<PathGenerator> Task::getPathGenerator() const {
  return std::make_shared<PathGenerator>(_identifier.localName());
};
}
