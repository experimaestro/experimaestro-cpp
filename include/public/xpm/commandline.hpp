/**
 * How a command line can be defined
 */

#ifndef PROJECT_COMMANDLINE_HPP
#define PROJECT_COMMANDLINE_HPP

#include <vector>
#include <unordered_map>

#include <xpm/common.hpp>
#include "json.hpp"
#include "filesystem.hpp"

namespace xpm {

class StructuredValue;
class Job;

/** Base class for all command related classes */
class CommandPart NOSWIG(: public std::enable_shared_from_this<CommandPart>) {
public:
  virtual void addDependencies(Job & job);
  virtual void forEach(std::function<void(CommandPart &)> f);
};

/**
 * A command component that can be processed depending on where the command is running.
 */
class AbstractCommandComponent : public CommandPart {
 protected:
  AbstractCommandComponent();
 public:
  virtual ~AbstractCommandComponent();
  virtual nlohmann::json toJson() const = 0;
};

/** A command argument */
class CommandString : public AbstractCommandComponent {
  std::string value;
public:
  CommandString(const std::string &value);
  virtual ~CommandString();
  std::string toString() const;
  virtual nlohmann::json toJson() const override;
};

/** A command argument as a path */
class CommandPath : public AbstractCommandComponent {
  Path _path;
public:
  CommandPath(Path path);
  void path(Path path);
  virtual ~CommandPath();
  std::string toString() const;
  virtual nlohmann::json toJson() const override;
};

/** A command argument as a path */
class CommandPathReference : public AbstractCommandComponent {
  std::string key;
public:
  CommandPathReference(std::string const &key);
  virtual ~CommandPathReference();
  virtual nlohmann::json toJson() const override;
  std::string toString() const;
};


/** A command component where the name is replaced by a string */
class CommandContent : public AbstractCommandComponent {
  std::string key;
  std::string content;
 public:
  CommandContent(const std::string &key, const std::string &value);
  virtual ~CommandContent();
  std::string toString() const;
  virtual nlohmann::json toJson() const override;
};

/** Just a placeholder for the JSON parameter file path */
class CommandParameters : public AbstractCommandComponent {
  ptr<StructuredValue> value;
public:
  CommandParameters();
  virtual ~CommandParameters();
  void setValue(ptr<StructuredValue> const & value);
  virtual void addDependencies(Job & job) override;
  virtual nlohmann::json toJson() const override;
};

/**
 * Base class for all commands
 */
class AbstractCommand : public CommandPart {};

/**
 * A command is composed of command components
 */
class Command : public AbstractCommand {
  std::vector<ptr<AbstractCommandComponent>> components;
 public:
  void add(ptr<AbstractCommandComponent> const & component);

  nlohmann::json toJson() const;
  void load(nlohmann::json const & j);
  virtual void forEach(std::function<void(CommandPart &)> f);
};

/**
 * A command line
 */
class CommandLine : public AbstractCommand {
  std::vector<ptr<Command>> commands;
 public:
  CommandLine();

  void add(ptr<Command> const & command);
  nlohmann::json toJson() const;
  void load(nlohmann::json const & j);
  virtual void forEach(std::function<void(CommandPart &)> f);

};


struct NamedPipeRedirections {
  std::vector<Path> outputRedirections;
  std::vector<Path> errorRedirections;
};

/**
 * Context of a command used to generate the actual command line instructions
 */
struct CommandContext {
  ptr<StructuredValue> parameters;
  std::unordered_map<CommandPart const *, NamedPipeRedirections> namedPipeRedirectionsMap;

  NamedPipeRedirections &getNamedRedirections(CommandPart const & key,
                                              bool create);
  Path getWorkingDirectory();  
};

}

#endif //PROJECT_COMMANDLINE_HPP
