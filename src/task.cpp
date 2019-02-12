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
  if (!type) throw std::invalid_argument("Type should not be null when constructing a task");
}

Task::Task(ptr<Type> const &type) : Task(type->name(), type) {
}

Typename Task::name() const { return _type->name(); }

void Task::submit(ptr<Workspace> const & _workspace,
  ptr<Launcher> const & _launcher,
  ptr<Value> const & _sv,
  std::vector<std::shared_ptr<Dependency>> const & dependencies
) const {
  LOGGER->info("Preparing job");

  auto sv = std::dynamic_pointer_cast<MapValue>(_sv);
  if (!sv) throw argument_error("Value are not a map");
    
  // Set task
  sv->task(const_cast<Task*>(this)->shared_from_this());

  auto workspace = _workspace ? _workspace : Workspace::currentWorkspace();
  if (!workspace) {
    throw argument_error("No workspace was created");
  }
  auto &launcher = _launcher ? _launcher : Launcher::defaultLauncher();

  // Set locator 
  // FIXME: identifier computed two times
  auto jobId = sv->uniqueIdentifier();
  auto svlocator = getPathGenerator()->generate(GeneratorContext(*workspace, sv));

  // Validate and seal the task sv
  LOGGER->debug("Configuring task");
  sv->configure(*workspace);

  // Get generated directory as locator

  // Set the command parameters
  auto job = mkptr<CommandLineJob>(std::dynamic_pointer_cast<ScalarValue>(svlocator)->asPath(), launcher, _commandLine);
  job->setIds(_identifier.toString(), jobId);
  job->parameters(sv);
  job->init();
  sv->job(job);
  LOGGER->info("Adding {} manual dependencies", dependencies.size());
  for(auto dependency: dependencies) {
    job->addDependency(dependency);
  }

  workspace->submit(job);
  LOGGER->debug("Submitting job {} (id {}) {}", job->locator(), job->getJobId(), sv->toJsonString());
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
