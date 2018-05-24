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

Task::Task(TypeName const &typeName, ptr<Type> const &type) : _identifier(typeName), _type(type) {
}

Task::Task(ptr<Type> const &type) : _identifier(type->typeName()), _type(type) {
}

TypeName Task::typeName() const { return _type->typeName(); }

void Task::submit(ptr<Workspace> const & _workspace,
  ptr<Launcher> const & _launcher,
  ptr<StructuredValue> const & sv
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
  LOGGER->debug("Validating task");
  sv->validate();


  // Get generated directory as locator

  // Set the command parameters
  _commandLine->forEach([&sv](CommandPart & c) -> void {
    if (auto cp = dynamic_cast<CommandParameters*>(&c)) {
      cp->setValue(sv);
    }
  });

  auto job = mkptr<CommandLineJob>(svlocator->value().asPath(), launcher, _commandLine);
  job->init();
  sv->job(job);

  workspace->submit(job);
  LOGGER->info("Submitting job {} (id {})", job->locator(), job->getId());
}

void Task::commandline(ptr<CommandLine> const & command) {
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
