#ifndef EXPERIMAESTRO_TASK_HPP
#define EXPERIMAESTRO_TASK_HPP

#include <xpm/xpm.hpp>

namespace xpm {

/**
 * A task can be executed and has an associated type
 */
class Task
#ifndef SWIG
    : public std::enable_shared_from_this<Task>
#endif
{
 public:
  /**
   * Defines a new task with a specific identifier
   * @param taskIdentifier The task identifier
   * @param outputType The output type
   */
  Task(TypeName const &taskIdentifier, ptr<Type> const &outputType);

  /**
   * Initialize a task with the same identifier as the type
   * @param outputType The output type, whose typename is used as the task identifier
   */
  Task(ptr<Type> const &outputType);

  /**
   * Configure the object
   * @param object The object corresponding to the task type
   * @param send If false, the job will not be sent to the experimaestro server
   */
  void submit(ptr<StructuredValue> const &object,
              bool send,
              ptr<rpc::Launcher> const &launcher,
              ptr<rpc::LauncherParameters> const &launcherParameters) const;

  /** Returns the type of this task */
  TypeName typeName() const;

  /** Returns the type of this task */
  Type::Ptr type();

  /** String representation */
  std::string toString() const;
  
  /** Sets the command line for the task */
  void commandline(CommandLine command);

  /** Gets the task identifier */
  TypeName const &identifier() const;

  /** Convert to JSON */
  nlohmann::json toJson();

  /** Get path generator for resource location */
  ptr<PathGenerator> getPathGenerator() const;

  /** Gets the running status */
  static bool isRunning() { return _running; }
 private:
  /// Task identifier
  TypeName _identifier;

  /// The type for this task
  ptr<Type> _type;

  /// Command line
  CommandLine _commandLine;

  /// True if a task is running
  static bool _running;
};

} // ns

#endif