//
// Created by Benjamin Piwowarski on 13/01/2017.
//

#include <xpm/common.hpp>
#include <xpm/xpm.hpp>
#include "private.hpp"

DEFINE_LOGGER("xpm")

namespace xpm {

using nlohmann::json;

Task::Task(TypeName const &typeName, std::shared_ptr<Type> const &type) : _identifier(typeName), _type(type) {
}

Task::Task(std::shared_ptr<Type> const &type) : _identifier(type->typeName()), _type(type) {
}

TypeName Task::typeName() const { return _type->typeName(); }

void Task::submit(std::shared_ptr<Object> const &object) const {
  // Find dependencies
  std::vector<std::shared_ptr<rpc::Dependency>> dependencies;
  object->findDependencies(dependencies);

  // Validate and seal the task object
  object->validate();
  object->seal();

  // Get generated directory as locator
  auto locator = rpc::Path::toPath(PathGenerator::SINGLETON.generate(*object)->asString());

  // Prepare the command line
  CommandContext context;
  context.parameters = object->toJsonString();
  auto command = _commandLine.rpc(context);

  // Add dependencies
  LOGGER->info("Adding {} dependencies", dependencies.size());
  for (auto dependency: dependencies) {
    LOGGER->info("Adding dependency {}", dependency->identifier());
    command->add_dependency(dependency);
  }

  auto task = std::make_shared<rpc::CommandLineTask>(locator);
  task->taskId(object->task()->identifier().toString());
  task->command(command);
  task->submit();
}

void Task::commandline(CommandLine command) {
  _commandLine = command;
}

TypeName const &Task::identifier() const {
  return _identifier;
}

void Task::objectFactory(std::shared_ptr<ObjectFactory> const &factory) {
  _factory = factory;
}

std::shared_ptr<Object> Task::create() {
  if (!_factory) {
    throw argument_error("Task has no factory");
  }
  auto object = _factory->create();
  object->type(_type);
  object->task(this->shared_from_this());
  LOGGER->debug("Setting task to {}", _identifier);
  return object;
}

void Task::execute(std::shared_ptr<Object> const &object) const {
  object->configure();
  // FIXME: the object should not be executed, but the task!
  object->execute();
}

nlohmann::json Task::toJson() {
  auto j = json::object();
  j["identifier"] = _identifier.toString();
  j["type"] = _type->typeName().toString();
  j["command"] = _commandLine.toJson();
  return j;
};
}
