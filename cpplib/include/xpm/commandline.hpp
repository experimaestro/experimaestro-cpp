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


class CommandContext {

};

/// Base class for all command arguments
class XPM_PIMPL(AbstractCommandComponent) {
 protected:
  AbstractCommandComponent();
};

/** A command argument */
class XPM_PIMPL_CHILD(CommandString, AbstractCommandComponent) {
 public:
  CommandString(const std::string &value);
  std::string toString() const;
};

/** A command component where the name is replaced by a string */
class XPM_PIMPL_CHILD(CommandContent, AbstractCommandComponent) {
 public:
  CommandContent(const std::string &value);
  std::string toString() const;
};

/** Just a placeholder */
class CommandParameters : public AbstractCommandComponent {
 public:
};

class Command {
  std::vector<AbstractCommandComponent> components;
 public:
  void add(AbstractCommandComponent component);
};

class CommandLine {
  std::vector<Command> commands;
 public:
  CommandLine();

  std::shared_ptr<rpc::AbstractCommand> rpc();
};
}

#endif //PROJECT_COMMANDLINE_HPP
