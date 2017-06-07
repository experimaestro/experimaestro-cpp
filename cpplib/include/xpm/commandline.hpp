//
// Created by Benjamin Piwowarski on 09/12/2016.
//

#ifndef PROJECT_COMMANDLINE_HPP
#define PROJECT_COMMANDLINE_HPP

#include <vector>

#include "utils.hpp"
#include "rpc/objects.hpp"
#include "json.hpp"
#include "filesystem.hpp"

namespace xpm {


class Object;

struct CommandContext {
  std::shared_ptr<Object> parameters;
};

/// Base class for all command arguments
class XPM_PIMPL(AbstractCommandComponent) {
 protected:
  AbstractCommandComponent();
 public:
  virtual ~AbstractCommandComponent();
  std::shared_ptr<rpc::AbstractCommandComponent> rpc(CommandContext &context) const;
  virtual nlohmann::json toJson() const;
};

/** A command argument */
class XPM_PIMPL_CHILD(CommandString, AbstractCommandComponent) {
 public:
  CommandString(const std::string &value);
  virtual ~CommandString();
  std::string toString() const;
};

/** A command argument as a path */
class XPM_PIMPL_CHILD(CommandPath, AbstractCommandComponent) {
 public:
  CommandPath(Path path);
  void path(Path path);
  virtual ~CommandPath();
  std::string toString() const;
};

/** A command argument as a path */
class XPM_PIMPL_CHILD(CommandPathReference, AbstractCommandComponent) {
 public:
  CommandPathReference(std::string const &key);
  virtual ~CommandPathReference();
  std::string toString() const;
};


/** A command component where the name is replaced by a string */
class XPM_PIMPL_CHILD(CommandContent, AbstractCommandComponent) {
 public:
  CommandContent(const std::string &key, const std::string &value);
  virtual ~CommandContent();
  std::string toString() const;
};

/** Just a placeholder for JSON command line parameters */
class XPM_PIMPL_CHILD(CommandParameters, AbstractCommandComponent) {
 public:
  CommandParameters();
  virtual ~CommandParameters();
};


class Command {
  std::vector<AbstractCommandComponent> components;
 public:
  void add(AbstractCommandComponent component);

  std::shared_ptr<rpc::Command> rpc(CommandContext &context) const;
  nlohmann::json toJson() const;
  void load(nlohmann::json const & j);
};

class CommandLine {
  std::vector<Command> commands;
 public:
  CommandLine();

  std::shared_ptr<rpc::AbstractCommand> rpc(CommandContext &context) const;
  void add(Command command);
  nlohmann::json toJson() const;
  void load(nlohmann::json const & j);
};
}

#endif //PROJECT_COMMANDLINE_HPP
