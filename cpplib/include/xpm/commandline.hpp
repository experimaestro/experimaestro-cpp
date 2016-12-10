//
// Created by Benjamin Piwowarski on 09/12/2016.
//

#ifndef PROJECT_COMMANDLINE_HPP
#define PROJECT_COMMANDLINE_HPP

#include <vector>
#include <xpm/utils.hpp>
#include <include/xpm/rpc/objects.hpp>
#include "json.hpp"

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
  virtual std::shared_ptr<rpc::AbstractCommandComponent> rpc(CommandContext &context);
};

/** A command argument */
class XPM_PIMPL_CHILD(CommandString, AbstractCommandComponent) {
 public:
  CommandString(const std::string &value);
  virtual ~CommandString();
  virtual std::shared_ptr<rpc::AbstractCommandComponent> rpc(CommandContext &context) override;
  std::string toString() const;
};

/** A command component where the name is replaced by a string */
class XPM_PIMPL_CHILD(CommandContent, AbstractCommandComponent) {
 public:
  CommandContent(const std::string &key, const std::string &value);
  virtual ~CommandContent();
  virtual std::shared_ptr<rpc::AbstractCommandComponent> rpc(CommandContext &context) override;
  std::string toString() const;
};

/** Just a placeholder */
class CommandParameters : public AbstractCommandComponent {
 public:
  virtual ~CommandParameters();
  virtual std::shared_ptr<rpc::AbstractCommandComponent> rpc(CommandContext &context) override;
};

class Command {
  std::vector<AbstractCommandComponent> components;
 public:
  void add(AbstractCommandComponent component);
  std::shared_ptr<rpc::Command> rpc(CommandContext &context);
};

class CommandLine {
  std::vector<Command> commands;
 public:
  CommandLine();

  std::shared_ptr<rpc::AbstractCommand> rpc(CommandContext &context);
};
}

#endif //PROJECT_COMMANDLINE_HPP
