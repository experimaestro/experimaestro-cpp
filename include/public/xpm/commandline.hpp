/**
 * How a command line can be defined
 */

#ifndef PROJECT_COMMANDLINE_HPP
#define PROJECT_COMMANDLINE_HPP

#include <vector>
#include <unordered_map>

#include <xpm/common.hpp>
#include <xpm/launchers/launchers.hpp>
#include "json.hpp"
#include "filesystem.hpp"

namespace xpm {

class Value;
class Job;
struct CommandContext;
class Workspace;

/** Base class for all command related classes */
class CommandPart : public std::enable_shared_from_this<CommandPart> {
public:
  virtual void forEach(std::function<void(CommandPart &)> f);
  virtual void output(CommandContext & context, std::ostream & out) const = 0;
  virtual nlohmann::json toJson() const = 0;
};

/**
 * A command component that can be processed depending on where the command is running.
 */
class AbstractCommandComponent : public CommandPart {
 public:
  AbstractCommandComponent();
  virtual ~AbstractCommandComponent();
};

/** A command argument */
class CommandString : public AbstractCommandComponent {
  std::string value;
public:
  CommandString(const std::string &value);
  virtual ~CommandString();
  std::string toString() const;
  virtual nlohmann::json toJson() const override;
  virtual void output(CommandContext & context, std::ostream & out) const override;
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
  virtual void output(CommandContext & context, std::ostream & out) const override;
};

/** A command argument as a path */
class CommandPathReference : public AbstractCommandComponent {
  std::string key;
public:
  CommandPathReference(std::string const &key);
  virtual ~CommandPathReference();
  virtual nlohmann::json toJson() const override;
  std::string toString() const;
  virtual void output(CommandContext & context, std::ostream & out) const override;
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
  virtual void output(CommandContext & context, std::ostream & out) const override;
};

/** Just a placeholder for the JSON parameter file path */
class CommandParameters : public AbstractCommandComponent {
public:
  CommandParameters();
  virtual ~CommandParameters();
  virtual nlohmann::json toJson() const override;
  virtual void output(CommandContext & context, std::ostream & out) const override;
};

/**
 * Base class for all commands
 */
class AbstractCommand : public CommandPart {
public:
  Redirect inputRedirect;
  Redirect outputRedirect;
  Redirect errorRedirect;

  virtual void output(CommandContext & context, std::ostream & out) const override;
protected:
  virtual std::vector<std::shared_ptr<AbstractCommand>> reorder() const = 0;
};

/**
 * A command is composed of command components
 */
class Command : public AbstractCommand {
  std::vector<std::shared_ptr<AbstractCommandComponent>> components;
 public:
  virtual ~Command();
  void add(std::shared_ptr<AbstractCommandComponent> const & component);
  void output(CommandContext & context, std::ostream & out) const override;
  
  nlohmann::json toJson() const override;
  void load(nlohmann::json const & j);
  virtual void forEach(std::function<void(CommandPart &)> f) override;
protected:
  virtual std::vector<std::shared_ptr<AbstractCommand>> reorder() const override;
};

/**
 * A command line
 */
class CommandLine : public AbstractCommand {
  std::vector<std::shared_ptr<AbstractCommand>> commands;
 public:
  CommandLine();
  virtual ~CommandLine();

  void add(std::shared_ptr<Command> const & command);
  nlohmann::json toJson() const override;
  void load(nlohmann::json const & j);
  virtual void forEach(std::function<void(CommandPart &)> f) override;
protected:
  virtual std::vector<std::shared_ptr<AbstractCommand>> reorder() const override;
};


struct NamedPipeRedirections {
  std::vector<Path> outputRedirections;
  std::vector<Path> errorRedirections;
};

/**
 * Context of a command used to generate the actual command line instructions
 */
struct CommandContext {
  Workspace & workspace;
  Connector const & connector;
  std::shared_ptr<Value> parameters;
  std::unordered_map<CommandPart const *, NamedPipeRedirections> namedPipeRedirectionsMap;
  Path folder;
  std::string name;
  std::unordered_map<std::string, int> counts;
  
  
  CommandContext(Workspace & workspace, Connector const & connector, Path const &folder, std::string const &name);
  
  NamedPipeRedirections &getNamedRedirections(CommandPart const & key, bool create);

  /**
   * Returns the path to a generated file 
   */
  Path getAuxiliaryFile(std::string const & prefix, std::string const & suffix);

  Path getWorkingDirectory();  

  void writeRedirection(std::ostream &out, Redirect const & redirect, int stream);
  void printRedirections(int stream, std::ostream &out, 
      Redirect const & outputRedirect, std::vector<Path> const & outputRedirects);

};

}

#endif //PROJECT_COMMANDLINE_HPP
