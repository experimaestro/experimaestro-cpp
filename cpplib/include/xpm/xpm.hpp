#include <memory>
#include <string>
#include <map>
#include <vector>
#include <unordered_map>

#include <xpm/json.hpp>
#include <xpm/utils.hpp>
#include <xpm/commandline.hpp>

namespace xpm {

#ifdef SWIG
#define SWIG_MUTABLE %mutable;
#define SWIG_IMMUTABLE %immutable;
#else
#define SWIG_MUTABLE
#define SWIG_IMMUTABLE
#endif

// SHA-1 digest lenght
static const int DIGEST_LENGTH = 20;

// Forward declarations
class Object;
class AbstractObjectHolder;
class Type;
class Task;
class Register;
struct Helper;
class Value;

/// Valeur optionnelle
template<typename T>
class optional {
  T *t;
 public:
  optional() : t(nullptr) {}

  optional(T &t) : t(&t) {}

  ~optional() {
  }

  operator optional<T>() const {
    return t != nullptr ? optional<T>(*t) : optional<T>();
  }

  T &operator*() {
    if (t == nullptr) throw std::runtime_error("Optional value is not set");
    return *t;
  }

  T const &operator*() const {
    if (t == nullptr) throw std::runtime_error("Optional value is not set");
    return *t;
  }

  T *operator->() {
    if (t == nullptr) throw std::runtime_error("Optional value is not set");
    return t;
  }

  T const *operator->() const {
    if (t == nullptr) throw std::runtime_error("Optional value is not set");
    return t;
  }

  operator bool() const {
    return t != nullptr;
  }
};


// ---
// --- Namespace and type names
// ---

/**
 * Qualified name
 */
class TypeName {
  std::string name;
 public:
  /** Creates a qualified name
  * @param ns the namespace
  * @param name the local name
  */
  TypeName(std::string const &name);

  TypeName call(std::string const &localname) const;

  std::string toString() const;

  int hash() const;

  std::string localName() const;

  bool operator==(TypeName const &other) const {
    return other.name == name;
  }
  bool operator!=(TypeName const &other) const {
    return other.name != name;
  }
};

}

#ifndef SWIG
namespace std {
/** Hash of type name */
template<>
struct hash<xpm::TypeName> {
  inline size_t operator()(xpm::TypeName const &typeName) const { return typeName.hash(); }
};
}
#endif

namespace xpm {


// ---
// --- Structured values
// ---

enum class ValueType : int8_t {
  NONE, INTEGER, REAL, STRING, BOOLEAN
};


/**
 * Object
 *
 * This is an associative dictionnary, where some entries
 * have a special meaning. It can be associated to a value.
 */
class Object
#ifndef SWIG
    : public std::enable_shared_from_this<Object>
#endif
{
 public:
  /// Default constructor
  Object();

  /// Constructor from a map
  Object(std::map<std::string, std::shared_ptr<Object>> &map);

  virtual ~Object();

  /// Construct from a JSON object
  static std::shared_ptr<Object> createFromJson(Register &xpmRegister, nlohmann::json const &jsonValue);

  /// Returns true if objects are equal
  virtual bool equals(Object &other);

  /// Returns the string
  virtual std::string asString();

  /// Returns the string
  virtual bool asBoolean();

  /// Returns an integer
  virtual long asInteger();

  /// Returns an integer
  virtual double asReal();


  /// Checks whether a key exists
  bool hasKey(std::string const &key) const;

  /// Get access to one value
  std::shared_ptr<Object> set(const std::string &key, std::shared_ptr<Object> const &);

  template<typename _Value>
  inline void set(std::string const &key, _Value const &value) {
      set(key, std::static_pointer_cast<Object>(std::make_shared<Value>(value)));
  }

  /// Get access to one value
  std::shared_ptr<Object> get(const std::string &key);

  /// Seal the object
  void seal();

  /// Returns whether this object is sealed
  bool isSealed() const;

  /// Returns JSON string
  std::string toJsonString();

  /// Returns JSON string
  inline std::string toString() { return toJsonString(); }

  /**
   * Returns a almost-unique identifier using a
   * hash function (SHA-1)
   */
  std::string uniqueIdentifier() const;

  /**
   * Check whether the value is set and is default
   */
  bool isDefault() const;

  /**
   * Converts to JSON
   * @return
   */
  virtual nlohmann::json toJson();

  /**
   *  Whether this element can be ignored for digest computation
   */
  bool canIgnore();

  /**
   * Retrieve content
   */
  std::map<std::string, std::shared_ptr<Object>> const &content();

  /** Sets the structured value (globally) */
  void setValue(std::shared_ptr<Object> const &value);

  /** Sets a value  */
  virtual void setValue(std::string const &name, std::shared_ptr<Object> const &value) {};

  /** Sets the task */
  void task(std::shared_ptr<Task> const &task);

  /** Sets the task */
  std::shared_ptr<Task> task();

  /**
   * Find dependencies
   * @param dependencies A dependency vector to fill
   */
  virtual void findDependencies(std::vector<std::shared_ptr<rpc::Dependency>> &dependencies);

  /**
   * Validate values
   */
  void validate();

  /** Get type */
  std::shared_ptr<Type> type();

  /** Get type */
  void type(std::shared_ptr<Type> const &type);

  /** Configure the object
   * <ol>
   * <li>Sets the value</li>
   * <li>Validate and generate values</li>
   * <li>Seal the object</li>
   * </ol>
   */
  void configure();

  /**
   * Submit the underlying task to experimaestro server
   */
  void submit();

  /**
   * Execute the underlying task
  */
  virtual void execute();

  /**
   * Copy the value
   */
  virtual std::shared_ptr<Object> copy();

  virtual std::array<unsigned char, DIGEST_LENGTH> digest() const;

  Object(Object &&other) = default;

  Object(Object const &other);

 private:

  /// Associated task, if any
  std::shared_ptr<Task> _task;

  /// The register
  std::shared_ptr<Register> _register;

  friend class ObjectFactory;

  /// Whether this value is sealed or not
  bool _sealed;

  /// Whether the value is default
  bool _default;

  /// Sub-values (map is used for sorted keys, ensuring a consistent unique identifier)
  std::map<std::string, std::shared_ptr<Object>> _content;

  friend struct Helper;
 protected:
  /// Type of the object
  std::shared_ptr<Type> _type;
};

class Array : public Object {
 public:
  typedef std::vector<std::shared_ptr<Object>> Content;
  virtual ~Array();

  virtual std::shared_ptr<Object> copy() override;
  void add(std::shared_ptr<Object> const &element);
  virtual std::array<unsigned char, DIGEST_LENGTH> digest() const override;
 private:
  Content _array;
};

/**
 * A value
 */
class Value : public Object {
  union Union {
    long integer;
    double real;
    bool boolean;
    std::string string;

    ~Union();
    Union();
  } _value;
  ValueType _scalarType;
 public:
  Value();
  Value(double value);
  Value(bool value);
  Value(int value);
  Value(long value);
  Value(std::string const &value);
  Value(Value const &other);
  Value &operator=(Value const &other);

  virtual ~Value();
  virtual std::shared_ptr<Object> copy() override;

  ValueType const scalarType() const;

  /** Is the value defined? */
  bool defined() const;

  /// Get the value
  bool getBoolean() const;
  double getReal() const;
  long getInteger() const;

  virtual void findDependencies(std::vector<std::shared_ptr<rpc::Dependency>> &dependencies) override;

  std::string const &getString();
  virtual nlohmann::json toJson() override;

  virtual bool equals(Object &) override ;

  /// Returns the string
  virtual std::string asString() override;

  /// Returns the string
  virtual bool asBoolean() override;

  /// Returns an integer
  virtual long asInteger() override;

  /// Returns an integer
  virtual double asReal() override;

  virtual std::array<unsigned char, DIGEST_LENGTH> digest() const override;

 protected:
  friend struct Helper;
  nlohmann::json jsonValue() const;
};



// ---
// --- Type and parser
// ---

/**
 * Generator for values
 */
class Generator {
 public:
  virtual std::shared_ptr<Object> generate(Object &object) = 0;
  virtual ~Generator() {}
};

/**
 * Generates a path within a working folder
 */
class PathGenerator : public Generator {
 public:
  static PathGenerator SINGLETON;

  virtual std::shared_ptr<Object> generate(Object &object);
};

SWIG_IMMUTABLE
extern PathGenerator &pathGenerator;
SWIG_MUTABLE

/**
 * Argument
 */
class Argument {
 public:
  Argument();
  Argument(std::string const &name);

  std::string const &name() const;

  void required(bool required);
  bool required() const;

  void defaultValue(std::shared_ptr<Object> const &defaultValue);
  std::shared_ptr<Object> defaultValue() const;

  Generator *generator();
  void generator(Generator *generator);

  std::shared_ptr<Type> const &type() const;
  void type(std::shared_ptr<Type> const &type);

  const std::string &help() const;
  void help(const std::string &help);
 private:
  /// The argument name
  std::string _name;

  /// The argument type
  std::shared_ptr<Type> _type;

  /// Help string (in Markdown syntax)
  std::string _help;

  /// Required
  bool _required;

  /// Default value
  std::shared_ptr<Object> _defaultValue;

  /// A generator
  Generator *_generator;
};

bool operator==(Value const &a, Value const &b);

/**
 * Object factory
 */
class ObjectFactory {
  /// The register
  std::shared_ptr<Register> _register;

 public:
  ObjectFactory(std::shared_ptr<Register> const &theRegister);
  virtual ~ObjectFactory() {}
  std::shared_ptr<Object> create();
  virtual std::shared_ptr<Object> _create() const = 0;
};

/**
 * Object type definition
 *
 * An object type is composed of:
 * <ul>
 * <li>A unique type name</li>
 * <li>A list of typed arguments</li>
 * <li>A parent type</li>
 * </ul>
 */
class Type
#ifndef SWIG
    : public std::enable_shared_from_this<Type>
#endif
{
  friend class Register;
 public:
  /**
   * Creates a new type
   * @param type The typename
   * @param _parent The parent type (or null pointer)
   */
  Type(TypeName const &type, std::shared_ptr<Type> _parent = nullptr,
       bool predefined = false, bool canIgnore = false);

  /** Type destruction */
  virtual ~Type();

  /**
   * Add new arguments for this type
   * @param argument
   */
  void addArgument(std::shared_ptr<Argument> const &argument);

  /**
   * Get the arguments
   */
  std::map<std::string, std::shared_ptr<Argument>> &arguments();

  /**
   * Get the arguments
   */
  std::map<std::string, std::shared_ptr<Argument>> const &arguments() const;

  /// Returns the JSON string corresponding to this type
  std::string toJson() const;

  /// Returns the type name
  TypeName const &typeName() const;

  /// Return the type
  std::string toString() const;

  /// Returns hash code (only based on type name)
  int hash() const;

  /// Predefined types
  bool predefined() const;

  /// Sets the object factory
  void objectFactory(std::shared_ptr<ObjectFactory> const &factory);

  /// Gets the object factory
  std::shared_ptr<ObjectFactory> const &objectFactory();

  /** Creates an object with a given type */
  std::shared_ptr<Object> create();

  /** Can ignore */
  inline bool canIgnore() { return _canIgnore; }
 private:
  const TypeName _type;
  std::shared_ptr<Type> _parent;
  std::map<std::string, std::shared_ptr<Argument>> _arguments;
  bool _predefined;
  bool _canIgnore;

  std::shared_ptr<ObjectFactory> _factory;

};

/**
 * A task can be executed
 */
class Task
#ifndef SWIG
    : public std::enable_shared_from_this<Task>
#endif
{
 public:
  /**
   * Defines a new task
   * @param identifier The task identifier
   * @param outputType The output type
   * @return
   */
  Task(std::shared_ptr<Type> const &type);

  /**
   * Configure the object
   * @param object The object corresponding to the task type
   */
  void submit(std::shared_ptr<Object> const &object) const;

  /** Returns the type of this task */
  TypeName typeName() const;

  /** Sets the command line for the task */
  void commandline(CommandLine command);

  /** Gets the task identifier */
  TypeName const &identifier() const;

  /** Executes */
  void execute(std::shared_ptr<Object> const &value) const;

  /// Sets the object factory
  void objectFactory(std::shared_ptr<ObjectFactory> const &factory);

  /** Creates an object with a given type */
  std::shared_ptr<Object> create();
 private:
  /// Task identifier
  TypeName _identifier;

  /// The type for this task
  std::shared_ptr<Type> _type;

  /// The object factory
  std::shared_ptr<ObjectFactory> _factory;

  /// Command line
  CommandLine _commandLine;
};

} // namespace xpm

#ifndef SWIG
namespace std {
/** Hash of type */
template<>
struct hash<xpm::Type> {
  inline size_t operator()(xpm::Type const &type) const { return type.typeName().hash(); }
};
}
#endif

namespace xpm {

SWIG_IMMUTABLE;
extern std::shared_ptr<Type> IntegerType;
extern std::shared_ptr<Type> RealType;
extern std::shared_ptr<Type> StringType;
extern std::shared_ptr<Type> BooleanType;
extern std::shared_ptr<Type> ArrayType;
extern std::shared_ptr<Type> AnyType;
extern std::shared_ptr<Type> PathType;
SWIG_MUTABLE;

// --- Building objects

/** Register for types */
class Register {
  /// Maps typenames to types
  std::unordered_map<TypeName, std::shared_ptr<Type>> _types;

  /// Maps typenames to tasks
  std::unordered_map<TypeName, std::shared_ptr<Task>> _tasks;
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

  /// Register a new task
  void addTask(std::shared_ptr<Task> const &task);

  /// Find a type given a t ype name
  std::shared_ptr<Task> getTask(TypeName const &typeName);

  /// Register a new type
  void addType(std::shared_ptr<Type> const &type);

  /// Find a type given a t ype name
  std::shared_ptr<Type> getType(TypeName const &typeName);

  /// Find a type given a t ype name
  std::shared_ptr<Type> getType(std::shared_ptr<Object> const &object);

  /// Build
  std::shared_ptr<Object> build(std::shared_ptr<Object> const &value);

  /// Build
  std::shared_ptr<Object> build(std::string const &value);
};

// --- Useful functions

/**
 * Report progress when running a task
 */
void progress(float percentage);

}
