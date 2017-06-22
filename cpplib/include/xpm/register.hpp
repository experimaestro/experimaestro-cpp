//
// Created by Benjamin Piwowarski on 16/01/2017.
//

#ifndef EXPERIMAESTRO_REGISTER_HPP
#define EXPERIMAESTRO_REGISTER_HPP

#include <xpm/xpm.hpp>

namespace YAML {
  class Node;
}

namespace xpm {
/** Register for types */
class Register {
  /// Maps typenames to types
  std::unordered_map<TypeName, std::shared_ptr<Type>> _types;

  /// Maps typenames to tasks
  std::unordered_map<TypeName, std::shared_ptr<Task>> _tasks;

  /// Default object factory
  std::shared_ptr<ObjectFactory> _defaultObjectFactory;

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
  void addTask(std::shared_ptr<Task> const &task);

  /// Find a type given a t ype name
  std::shared_ptr<Task> getTask(TypeName const &typeName, bool allowPlaceholder = false);

  /// Register a new type
  void addType(std::shared_ptr<Type> const &type);

  /// Find a type given a t ype name
  std::shared_ptr<Type> getType(TypeName const &typeName);

  /// Find a type given a t ype name
  std::shared_ptr<Type> getType(std::shared_ptr<Object> const &object);

  /// Build
  std::shared_ptr<Object> build(std::shared_ptr<Object> const &value);

  /// Build from a string
  std::shared_ptr<Object> build(std::string const &value);

  /// Get types
  std::unordered_map<TypeName, std::shared_ptr<Type>> &getTypes() { 
    return _types;
  };

  std::unordered_map<TypeName, std::shared_ptr<Task>> &getTasks() { 
    return _tasks;
  };


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

  /// Default object factory
  void objectFactory(std::shared_ptr<ObjectFactory> const &);
  std::shared_ptr<ObjectFactory> objectFactory();
};
}

#endif //PROJECT_REGISTER_HPP
