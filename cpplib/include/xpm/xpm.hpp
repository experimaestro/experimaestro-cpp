#include <memory>
#include <string>
#include <map>
#include <vector>
#include <unordered_map>

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

// Implementation classes
struct _StructuredValue;
struct _Type;
struct _Argument;

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

  T &operator*() {
    if (!t) throw std::runtime_error("Optional value is not set");
    return *t;
  }

  T const &operator*() const {
    if (!t) throw std::runtime_error("Optional value is not set");
    return *t;
  }

  T *operator->() {
    if (!t) throw std::runtime_error("Optional value is not set");
    return t;
  }

  operator bool() const {
    return t;
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
typedef std::vector<xpm::StructuredValue> ValueArray;
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
  std::string const &getString() const;

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
  StructuredValue(std::map<std::string, StructuredValue> &map);

  /// Returns true if the object has a value
  bool hasValue() const;

  /// Type of this object
  TypeName type() const;

  /// Set the type of this object
  void type(TypeName const &typeName);

  /// Checks whether a key exists
  bool hasKey(std::string const &key) const;

  /// Get access to one value
  StructuredValue &operator[](const std::string &key);

  /// Get access to one value
  StructuredValue const operator[](const std::string &key) const;

  /// Get access to the associated value
  Value value() const;

  /// Set the scalar
  void value(Value const &);

  /// Seal the object
  void seal();

  /// Returns whether this object is sealed
  bool isSealed() const;

  /// Parse
  static StructuredValue parse(std::string const &jsonString);

  /// Returns JSON string
  std::string toJson() const;

  /// Returns JSON string
  inline std::string toString() const { return toJson(); }

  /**
   * Returns a almost-unique identifier using a
   * hash function (SHA-1)
   */
  std::string uniqueIdentifier() const;

  /**
   * Check whether the value is set and is default
   */
  bool isDefault() const;

 private:
  /**
   *  Whether this element can be ignored for digest computation
   */
  bool canIgnore() const;

  /// Underlying implementation
  std::shared_ptr<_StructuredValue> _this;

  StructuredValue(std::shared_ptr<_StructuredValue> const &);

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
  virtual StructuredValue generate(Object const & object) const = 0;
  virtual ~Generator() {}
};

/**
 * Generates a path within a working folder
 */
class PathGenerator : public Generator {
 public:
  static const PathGenerator SINGLETON;

  virtual StructuredValue generate(Object const & object) const;
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

  Type const &type() const;
  void type(Type const &type);

  const std::string &help() const;
  void help(const std::string &help);
 private:
  std::shared_ptr<_Argument> _this;
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
class Type {
  std::shared_ptr<_Type> _this;
  friend class Register;
  friend struct _Type;
 public:
  Type();

  /**
   * Creates a new type
   * @param type The typename
   * @param _parent The parent type (or null pointer)
   */
  Type(TypeName const &type, Type *_parent = nullptr, bool predefined = false);

  /** Type destruction */
  virtual ~Type();

  /**
   * Add new arguments for this type
   * @param argument
   */
  void addArgument(Argument &argument);

  /**
   * Get the arguments
   */
  std::map<std::string, Argument> &arguments();

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
};

/**
 * A task can be executed
 */
class XPM_PIMPL(Task) {
 public:
  Task();

  /**
   * Defines a new task
   * @param identifier The task identifier
   * @param outputType The output type
   * @return
   */
  Task(Type &type);

  /**
   * Configure the object
   * @param object The object corresponding to the task type
   */
  void submit(std::shared_ptr<Object> const &object);

  /** Returns the type of this task */
  TypeName typeName() const;

  /** Sets the command line for the task */
  void commandline(CommandLine command);

  /** Gets the task identifier */
  TypeName const &identifier() const;

  /** Executes */
  void execute(StructuredValue value) const;

  /// Sets the object factory
  void objectFactory(std::shared_ptr<ObjectFactory> const &factory);

  /** Creates an object with a given type */
  std::shared_ptr<Object> create() const;
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
extern Type IntegerType;
extern Type RealType;
extern Type StringType;
extern Type BooleanType;
extern Type AnyType;
extern Type PathType;
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
  void setValue(StructuredValue &value);

  /** Sets a value  */
  virtual void setValue(std::string const &name, StructuredValue &value) {};

  /** Sets the task */
  void task(Task task);

  /** Sets the task */
  optional<Task const> task() const;

  /** Get the associated structured value */
  StructuredValue const getValue() const;

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
  template<typename _Value>
  inline void set(std::string const &key, _Value const &value) {
    set(key, StructuredValue(Value(value)));
  }

  /** Set a value */
  void set(std::string const &key, StructuredValue value);

  /** Get type */
  Type type() const;

  /** Get type */
  void type(Type const &type);

  /** Transform to JSON */
  std::string json() const;

  /** Configure the object
   * <ol>
   * <li>Sets the value</li>
   * <li>Validate and generate values</li>
   * <li>Seal the object</li>
   * </ol>
   */
  void configure(StructuredValue value);

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
  StructuredValue _value;

  /// Type of the object
  Type _type;

  /// Associated task, if any
  optional<Task> _task;
};

// --- Building objects

/** Register for types */
class Register {
  /// Maps typenames to types
  std::unordered_map<TypeName, Type> _types;

  /// Maps typenames to tasks
  std::unordered_map<TypeName, Task> _tasks;
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
  void addTask(Task &task);

  /// Find a type given a t ype name
  optional<Task const> getTask(TypeName const &typeName) const;

  /// Register a new type
  void addType(Type &type);

  /// Find a type given a t ype name
  optional<Type const> getType(TypeName const &typeName) const;

  /// Find a type given a t ype name
  Type getType(Object const &object) const;

  /// Build a new object from parameters
  std::shared_ptr<Object> build(StructuredValue &value) const;
};

// --- Useful functions

/**
 * Report progress when running a task
 */
void progress(float percentage);

}
