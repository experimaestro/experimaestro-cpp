//
// Created by Benjamin Piwowarski on 09/12/2016.
//

#ifndef PROJECT_COMMANDLINE_HPP
#define PROJECT_COMMANDLINE_HPP

#include <vector>
#include <xpm/utils.hpp>
#include <xpm/rpc/objects.hpp>
#include "json.hpp"
#include "filesystem.hpp"

namespace xpm {


struct CommandContext {
  std::string parameters;
};

/// Base class for all command arguments
class XPM_PIMPL(AbstractCommandComponent) {
 protected:
  AbstractCommandComponent();
 public:
  virtual ~AbstractCommandComponent();
  std::shared_ptr<rpc::AbstractCommandComponent> rpc(CommandContext &context);
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
  virtual ~CommandPath();
  std::string toString() const;
};

/** A command component where the name is replaced by a string */
class XPM_PIMPL_CHILD(CommandContent, AbstractCommandComponent) {
 public:
  CommandContent(const std::string &key, const std::string &value);
  virtual ~CommandContent();
  std::string toString() const;
};

/** Just a placeholder */
class XPM_PIMPL_CHILD(CommandParameters, AbstractCommandComponent) {
 public:
  CommandParameters();
  virtual ~CommandParameters();
};


class Command {
  std::vector<AbstractCommandComponent> components;
 public:
  void add(AbstractCommandComponent component);
  std::shared_ptr<rpc::Command> rpc(CommandContext &context) ;
};

class CommandLine {
  std::vector<Command> commands;
 public:
  CommandLine();

  std::shared_ptr<rpc::AbstractCommand> rpc(CommandContext &context);
  void add(Command command);
};
}

#endif //PROJECT_COMMANDLINE_HPP
