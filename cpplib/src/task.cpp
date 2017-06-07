//
// Created by Benjamin Piwowarski on 13/01/2017.
//

#include <xpm/common.hpp>
#include <xpm/xpm.hpp>
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

void Task::submit(std::shared_ptr<Object> const &object,
                  bool send,
                  std::shared_ptr<rpc::Launcher> const &launcher,
                  std::shared_ptr<rpc::LauncherParameters> const &launcherParameters
  ) const {
  // Find dependencies
  std::vector<std::shared_ptr<rpc::Dependency>> dependencies;
  if (send) {
    object->findDependencies(dependencies);
  }

  // Validate and seal the task object
  object->validate(true);
  object->seal();

  // Prepare the command line
  CommandContext context = { object };

  if (send) {
    // Get generated directory as locator
    auto path = getPathGenerator()->generate(*object)->asString();
    auto resource = object->get(KEY_RESOURCE)->asString();
    if (path != resource) {
      throw std::runtime_error("Resource [" + resource + "] and resource path [" + path + "] do not match");
    }
    auto locator = rpc::Path::toPath(path);
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
    task->setLauncher(launcher, launcherParameters);
    task->submit();
  } else {
    LOGGER->warn("Not sending task {}", object->task()->identifier());
  }
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

std::shared_ptr<Object> Task::create(std::shared_ptr<ObjectFactory> const &defaultFactory) {
  Object::Ptr object;
  if (!_factory) {
    object = _type->create(defaultFactory);
  } else {
    object = _factory->create();
  }
  object->type(_type);
  object->task(this->shared_from_this());
  LOGGER->debug("Setting task to {}", _identifier);
  return object;
}

void Task::execute(std::shared_ptr<Object> const &object) const {
  object->configure(false);
  try {
    _running = true;
    object->execute();
    _running = false;
  } catch(std::exception &e) {
    _running = false;
    throw;
  }
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
