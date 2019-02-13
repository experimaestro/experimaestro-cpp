#ifndef EXPERIMAESTRO_TASK_HPP
#define EXPERIMAESTRO_TASK_HPP

#include <xpm/xpm.hpp>
#include <xpm/type.hpp>
#include <xpm/launchers/launchers.hpp>

namespace xpm {

class Workspace;
class CommandLine;
class Dependency;

/**
 * A task can be executed and has an associated type
 */
class Task : public std::enable_shared_from_this<Task> {
 public:
  /**
   * Defines a new task with a specific identifier
   * @param taskIdentifier The task identifier
   * @param outputType The output type
   */
  Task(Typename const &taskIdentifier, std::shared_ptr<Type> const &outputType);

  /**
   * Initialize a task with the same identifier as the type
   * @param outputType The output type, whose typename is used as the task identifier
   */
  Task(std::shared_ptr<Type> const &outputType);

  /**
   * Configure the object
   * @param workspace The workspace
   * @param launcher The launcher used to launch the task (or null if not sending)
   * @param object The structured value with which the task is defined
   */
  void submit(std::shared_ptr<Workspace> const & workspace,
              std::shared_ptr<Launcher> const & launcher,
              std::shared_ptr<Value> const & sv,
              std::vector<std::shared_ptr<Dependency>> const & dependencies) const;

  /** Returns the type of this task */
  Typename name() const;

  /** Returns the type of this task */
  Type::Ptr type();

  /** String representation */
  std::string toString() const;
  
  /** Sets the command line for the task */
  void commandline(std::shared_ptr<CommandLine> const & command);

  /** Gets the task identifier */
  Typename const &identifier() const;

  /** Convert to JSON */
  nlohmann::json toJson();

  /** Get path generator for resource location */
  std::shared_ptr<PathGenerator> getPathGenerator() const;

  /** Gets the running status */
  static bool isRunning() { return _running; }

 private:
  /// Task identifier
  Typename _identifier;

  /// The type for this task
  std::shared_ptr<Type> _type;

  /// Command line
  std::shared_ptr<CommandLine> _commandLine;

  /// True if a task is running
  static bool _running;

  friend class Register;
};

} // ns

#endif