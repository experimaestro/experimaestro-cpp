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

void Task::submit(std::shared_ptr<Configuration> const &configuration,
                  bool send,
                  std::shared_ptr<rpc::Launcher> const &launcher,
                  std::shared_ptr<rpc::LauncherParameters> const &launcherParameters
  ) const {
  // Find dependencies
  std::vector<std::shared_ptr<rpc::Dependency>> dependencies;
  if (send) {
    configuration->findDependencies(dependencies, true);
  }

  // Validate and seal the task configuration
  configuration->configure();

  // Prepare the command line
  CommandContext context = { configuration };

  if (send) {
    // Get generated directory as locator
    auto locator = rpc::Path::toPath(configuration->resource());
    auto command = _commandLine.rpc(context);

    // Add dependencies
    LOGGER->info("Adding {} dependencies", dependencies.size());
    for (auto dependency: dependencies) {
      LOGGER->info("Adding dependency {}", dependency->identifier());
      command->add_dependency(dependency);
    }
    auto task = std::make_shared<rpc::CommandLineTask>(locator);
    task->taskId(configuration->task()->identifier().toString());
    task->command(command);
    task->setLauncher(launcher, launcherParameters);
    task->submit();
  } else {
    LOGGER->warn("Not sending task {}", configuration->task()->identifier());
  }
}

void Task::commandline(CommandLine command) {
  _commandLine = command;
}

TypeName const &Task::identifier() const {
  return _identifier;
}

// TODO: cleanup
// void Task::execute(std::shared_ptr<Configuration> const &object) const {
//   try {
//     _running = true;
//     object->_pre_execute();
//     object->execute();
//     object->_post_execute();
//     _running = false;
//   } catch(std::exception &e) {
//     _running = false;
//     throw;
//   }
// }

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
