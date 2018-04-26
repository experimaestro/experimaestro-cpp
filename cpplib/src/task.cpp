//
// Created by Benjamin Piwowarski on 13/01/2017.
//

#include <xpm/common.hpp>
#include <xpm/task.hpp>
#include "private.hpp"

DEFINE_LOGGER("xpm")

namespace xpm {

using nlohmann::json;

bool Task::_running = false;

Task::Task(TypeName const &typeName, std::shared_ptr<Type> const &type) : _identifier(typeName), _type(type) {
}

Task::Task(std::shared_ptr<Type> const &type) : _identifier(type->typeName()), _type(type) {
}

TypeName Task::typeName() const { return _type->typeName(); }

void Task::submit(std::shared_ptr<StructuredValue> const &sv,
                  bool send,
                  std::shared_ptr<rpc::Launcher> const &launcher,
                  std::shared_ptr<rpc::LauncherParameters> const &launcherParameters
  ) const {
  
  // Set task
  sv->task(const_cast<Task*>(this)->shared_from_this());

  // Find dependencies
  std::vector<std::shared_ptr<rpc::Dependency>> dependencies;
  if (send) {
    sv->findDependencies(dependencies, true);
  }

  // Set locator
  auto svlocator = getPathGenerator()->generate(sv);
  sv->resource(svlocator->value().asString());

  // Validate and seal the task sv
  LOGGER->info("Configuring task");
  sv->configure();
  LOGGER->info("Validating task");
  sv->validate();

  // Prepare the command line
  CommandContext context = { sv };

  if (send) {
    LOGGER->info("Sending task");

    // Get generated directory as locator
    auto locator = rpc::Path::toPath(sv->resource());
    auto command = _commandLine.rpc(context);

    // Add dependencies
    LOGGER->info("Adding {} dependencies", dependencies.size());
    for (auto dependency: dependencies) {
      LOGGER->info("Adding dependency {}", dependency->identifier());
      command->add_dependency(dependency);
    }
    auto task = std::make_shared<rpc::CommandLineTask>(locator);
    task->taskId(identifier().toString());
    task->command(command);
    task->setLauncher(launcher, launcherParameters);
    task->submit();
  } else {
    LOGGER->warn("Not sending task {}", identifier());
  }
}

void Task::commandline(CommandLine command) {
  _commandLine = command;
}

TypeName const &Task::identifier() const {
  return _identifier;
}

std::string Task::toString() const {
  return "Task '" + identifier().toString() + "' (output '" + typeName().toString() + "')";
}


nlohmann::json Task::toJson() {
  auto j = json::object();
  j["identifier"] = _identifier.toString();
  j["type"] = _type->typeName().toString();
  j["command"] = _commandLine.toJson();
  return j;
}

Type::Ptr Task::type() {
  return _type;
}
std::shared_ptr<PathGenerator> Task::getPathGenerator() const {
  return std::make_shared<PathGenerator>(_identifier.localName());
};
}