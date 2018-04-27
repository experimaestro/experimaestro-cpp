//
// Created by Benjamin Piwowarski on 16/01/2017.
//

#ifndef EXPERIMAESTRO_REGISTER_HPP
#define EXPERIMAESTRO_REGISTER_HPP

#include <xpm/xpm.hpp>
#include <xpm/task.hpp>

namespace YAML {
  class Node;
}

namespace xpm {
/** Register for types */
class Register {
  /// Maps typenames to types
  std::unordered_map<TypeName, ptr<Type>> _types;

  /// Maps typenames to tasks
  std::unordered_map<TypeName, ptr<Task>> _tasks;

 public:
  // Constructs a new register
  Register();

  virtual ~Register();

  /** Parse command line
   * <pre>[general options] command [command options]</pre>
   * where command can be :
   * <ul>
   *    <li><code>help</code> that generates help</li>
   *    <li><code>describe [type] [id]</code> that generates human readable information</li>
   *    <li><code>generate</code> that generates the JSON corresponding to the registry</li>
   *    <li><code>run <task-id> [task arguments...]</code> that runs a task</li>
   * </ul>
   * @param args
   */
  void parse(std::vector<std::string> const &args);

  /**
   * Parse command line
   *
   * @see parse(std::vector<strd::string> const &0
   * @param argc
   * @param argv
   */
  void parse(int argc, const char **argv);

  /// Register a new task
  void addTask(ptr<Task> const &task);

  /// Find a type given a t ype name
  ptr<Task> getTask(TypeName const &typeName, bool allowPlaceholder = false);

  /// Register a new type
  void addType(ptr<Type> const &type);

  /// Find a type given a t ype name
  ptr<Type> getType(TypeName const &typeName);

  /// Find a type given a t ype name
  ptr<Type> getType(ptr<StructuredValue> const &object);

  /// Build from a JSON string
  ptr<StructuredValue> build(std::string const &value);

  /// Get types
  std::unordered_map<TypeName, ptr<Type>> &getTypes() { 
    return _types;
  };

  std::unordered_map<TypeName, ptr<Task>> &getTasks() { 
    return _tasks;
  };

  /// Run task
  virtual void runTask(ptr<Task> const & task, ptr<StructuredValue> const & sv);

  /// Create object
  virtual ptr<Object> createObject(ptr<StructuredValue> const & sv);

#ifndef SWIG
  /// Load new definitions from YAML
  void load(YAML::Node const &j);
#endif

  /// Load new definitions from YAML
  void loadYAML(Path const &j);

  /// Load new definitions from YAML string
  void loadYAML(std::string const &j);

  /// Load new definitions from file
  void load(nlohmann::json const &value);

  /// Load new definitions from file
  void load(std::string const &value);

  /// Load new definitions from file
  void load(Path const &value);

  /// Outputs the JSON defition file
  void generate() const;
};
}

#endif //PROJECT_REGISTER_HPP
