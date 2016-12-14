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
class StructuredValue;
class AbstractObjectHolder;
class Type;
class Object;

/// Valeur optionnelle
template<typename T>
class optional {
  T *t;
 public:
  optional() : t(nullptr) {}

  optional(T &t) : t(&t) {}

  ~optional() {
  }

  operator optional<T const>() const {
    return t != nullptr ? optional<T const>(*t) : optional<T const>();
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
  NONE, INTEGER, REAL, STRING, BOOLEAN, ARRAY, OBJECT
};

class StructuredValue;
typedef std::vector<std::shared_ptr<StructuredValue>> ValueArray;
struct Helper;

/**
 * A value associated to a structured value
 *
 */
class Value {
  union Union {
    long integer;
    double real;
    bool boolean;
    std::string string;
    ValueArray array;

    /// Object is a special case: it is not a simple value
    std::shared_ptr<Object> object;

    ~Union();
    Union();
  } _value;
  ValueType _type;
 public:
  Value();
  Value(double value);
  Value(bool value);
  Value(int value);
  Value(long value);
  Value(std::string const &value);
  Value(ValueArray &&value);
  Value(std::shared_ptr<Object> const &value);
  Value(Value const &other);
  Value &operator=(Value const &other);

  std::string toString() const;
  ~Value();

  TypeName const &type() const;
  ValueType const scalarType() const;

  /** Is the value defined? */
  bool defined() const;

  /** Defined and not an object */
  bool simple() const;

  /// Get the value
  bool getBoolean() const;
  double getReal() const;
  long getInteger() const;
  std::string const &getString() const;
  ValueArray &getArray();
  std::shared_ptr<Object> getObject();

  friend struct Helper;
};

/**
 * Structured value
 *
 * This is an associative dictionnary, where some entries
 * have a special meaning. It can be associated to a value.
 */
class StructuredValue {
 public:
  /// Default constructor
  StructuredValue();

  /// Construct from scalar
  StructuredValue(Value &&value);

  /// Construct from scalar
  StructuredValue(Value const &value);

  /// Constructor from a map
  StructuredValue(std::map<std::string, std::shared_ptr<StructuredValue>> &map);

  /// Construct from a JSON object
  StructuredValue(nlohmann::json const &jsonValue);

  /// Returns true if the object has a value
  bool hasValue() const;

  /// Type of this object
  TypeName type() const;

  /// Set the type of this object
  void type(TypeName const &typeName);

  /// Checks whether a key exists
  bool hasKey(std::string const &key) const;

  /// Get access to one value
  std::shared_ptr<StructuredValue> &operator[](const std::string &key);

  /// Get access to one value
  std::shared_ptr<StructuredValue const> operator[](const std::string &key) const;

  /// Get access to the associated value
  Value value() const;

  /// Set the scalar
  void value(Value const &);

  /// Seal the object
  void seal();

  /// Returns whether this object is sealed
  bool isSealed() const;

  /// Parse
  static std::shared_ptr<StructuredValue> parse(std::string const &jsonString);

  /// Returns JSON string
  std::string toJsonString() const;

  /// Returns JSON string
  inline std::string toString() const { return toJsonString(); }

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
  nlohmann::json toJson() const;

  /**
   *  Whether this element can be ignored for digest computation
   */
  bool canIgnore() const;

  /**
   * Retrieve content
   */
  std::map<std::string, std::shared_ptr<StructuredValue>> const & content() const;

 private:
  /// Whether this value is sealed or not
  bool _sealed;

  /// Scalar value
  Value _value;

  /// Sub-values (map is used for sorted keys, ensuring a consistent unique identifier)
  std::map<std::string, std::shared_ptr<StructuredValue>> _content;

  std::array<unsigned char, DIGEST_LENGTH> digest() const;

  friend struct _StructuredValue;
  friend struct Helper;
};



// ---
// --- Type and parser
// ---

/**
 * Generator for values
 */
class Generator {
 public:
  virtual std::shared_ptr<StructuredValue> generate(Object const &object) const = 0;
  virtual ~Generator() {}
};

/**
 * Generates a path within a working folder
 */
class PathGenerator : public Generator {
 public:
  static const PathGenerator SINGLETON;

  virtual std::shared_ptr<StructuredValue> generate(Object const &object) const;
};

extern const PathGenerator &pathGenerator;

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

  void defaultValue(Value const &defaultValue);
  Value defaultValue() const;

  Generator const *generator() const;
  void generator(Generator const *generator);

  std::shared_ptr<Type const> const &type() const;
  void type(std::shared_ptr<Type const> const &type);

  const std::string &help() const;
  void help(const std::string &help);
 private:
  /// The argument name
  std::string _name;

  /// The argument type
  std::shared_ptr<Type const> _type;

  /// Help string (in Markdown syntax)
  std::string _help;

  /// Required
  bool _required;

  /// Default value
  Value _defaultValue;

  /// A generator
  Generator const *_generator;
};

bool operator==(Value const &a, Value const &b);

class Register;

/**
 * Object factory
 */
class ObjectFactory {
 public:
  virtual ~ObjectFactory() {}
  virtual std::shared_ptr<Object> create() const = 0;
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
  Type(TypeName const &type, std::shared_ptr<Type const> _parent = nullptr, bool predefined = false);

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
  std::shared_ptr<Object> create() const;

 private:
  const TypeName _type;
  std::shared_ptr<Type const> _parent;
  std::map<std::string, std::shared_ptr<Argument>> _arguments;
  bool _predefined;
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
  void execute(std::shared_ptr<StructuredValue> const &value) const;

  /// Sets the object factory
  void objectFactory(std::shared_ptr<ObjectFactory> const &factory);

  /** Creates an object with a given type */
  std::shared_ptr<Object> create() const;
 private:
  /// Task identifier
  TypeName _identifier;

  /// The type for this task
  std::shared_ptr<Type> _type;

  /// Command line
  CommandLine _commandLine;

  /// The object factory
  std::shared_ptr<ObjectFactory> _factory;
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
extern std::shared_ptr<Type> AnyType;
extern std::shared_ptr<Type> PathType;
SWIG_MUTABLE;

// ---
// --- Objects
// ---

/**
 * Base class for objects.
 *
 * Objects are constructed, and linked to, structured values.
 */
class Object
#ifndef SWIG
    : public std::enable_shared_from_this<Object>
#endif
{
 public:
  /** Creates a new object */
  Object();

  /** Virtual destructor */
  virtual ~Object();

  /** Sets the structured value (globally) */
  void setValue(std::shared_ptr<StructuredValue> const &value);

  /** Sets a value  */
  virtual void setValue(std::string const &name, std::shared_ptr<StructuredValue> const &value) {};

  /** Sets the task */
  void task(std::shared_ptr<Task const> const &task);

  /** Sets the task */
  std::shared_ptr<Task const> task() const;

  /** Get the associated structured value */
  std::shared_ptr<StructuredValue const> getValue() const;

  /** String representation of the object */
  virtual std::string toString() const;

  /**
   * Validate values
   */
  void validate();

  /**
   * Seal the object
   */
  void seal();

  /** Set a value */
  void set(std::string const &key, std::shared_ptr<StructuredValue> const &value);

  /** Set a value */
  template<typename _Value>
  inline void set(std::string const &key, _Value const &value) {
    set(key, std::make_shared<StructuredValue>(Value(value)));
  }

  inline void set(std::string const &key, Value const &value) {
    set(key, std::make_shared<StructuredValue>(value));
  }

  /** Get type */
  std::shared_ptr<Type const> type() const;

  /** Get type */
  void type(std::shared_ptr<Type const> const &type);

  /** Transform to JSON */
  std::string json() const;

  /** Configure the object
   * <ol>
   * <li>Sets the value</li>
   * <li>Validate and generate values</li>
   * <li>Seal the object</li>
   * </ol>
   */
  void configure(std::shared_ptr<StructuredValue> const &value);

  /**
   * Submit the underlying task to experimaestro server
   */
  void submit();

  /**
   * Execute the underlying task
  */
  virtual void execute();

 private:
  /// The associated structured value
  std::shared_ptr<StructuredValue> _value;

  /// Type of the object
  std::shared_ptr<Type const> _type;

  /// Associated task, if any
  std::shared_ptr<Task const> _task;
};

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
  std::shared_ptr<Task const> getTask(TypeName const &typeName) const;

  /// Register a new type
  void addType(std::shared_ptr<Type> const &type);

  /// Find a type given a t ype name
  std::shared_ptr<Type const> getType(TypeName const &typeName) const;

  /// Find a type given a t ype name
  std::shared_ptr<Type const> getType(Object const &object) const;

  /// Build a new object from parameters
  std::shared_ptr<Object> build(std::shared_ptr<StructuredValue> const &value) const;
};

// --- Useful functions

/**
 * Report progress when running a task
 */
void progress(float percentage);

}
