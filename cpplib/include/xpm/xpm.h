#include <memory>
#include <string>
#include <map>
#include <vector>
#include <unordered_map>

namespace xpm {

// SHA-1 digest lenght
static const int DIGEST_LENGTH = 20;

// Forward declarations
class StructuredValue;
class AbstractObjectHolder;
class Type;
class Object;

/** Base exception */
class exception : public std::exception {
  std::string _message;
 public:
  exception() {}
  exception(std::string const &message) : _message(message) {}

  virtual const char *what() const noexcept override {
    return _message.c_str();
  }
};

/** Thrown when trying to modify a sealed value */
class sealed_error : public exception {
 public:
  sealed_error();
};

/** Thrown when the argument is invalid */
class argument_error : public exception {
 public:
  argument_error(std::string const &message);
};


// ---
// --- Namespace and type names
// ---

// Qualified name
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

enum class ValueType {
  NONE, INTEGER, REAL, STRING, BOOLEAN, ARRAY, OBJECT
};

class StructuredValue;
typedef std::vector<std::shared_ptr<xpm::StructuredValue>> ValueArray;
struct Helper;

class Value {
  union Union {
    long integer;
    double real;
    bool boolean;
    std::string string;
    ValueArray array;
    std::shared_ptr<Object> object;

    ~Union();
    Union();
  } _value;
  ValueType _type;
 public:
  Value();
  Value(double value);
  Value(bool value);
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

  bool defined() const;

  friend struct Helper;
};

/**
 * Structured value
 *
 * This is an associative dictionnary, where some entries
 * have a special meaning. It can be associated to a value.
 */
class StructuredValue : public std::enable_shared_from_this<StructuredValue> {
 public:
  typedef std::shared_ptr<StructuredValue> Ptr;

  /// Default constructor
  StructuredValue();

  /// Construct from scalar
  StructuredValue(Value &&value);

  /// Construct from scalar
  StructuredValue(Value const &value);

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
  static Ptr parse(std::string const &jsonString);

  /// Returns JSON string
  std::string toJson() const;

  /**
   * Returns a almost-unique identifier using a
   * hash function (SHA-1)
   */
  std::string uniqueIdentifier() const;


 private:
  /**
   *  Whether this element can be ignored for digest computation
   */
  bool canIgnore() const;

  /// Internal digest function
  std::array<unsigned char, DIGEST_LENGTH> digest() const;

  /// Whether this value is sealed or not
  bool _sealed;

  /// Scalar value
  Value _scalar;

  /// Sub-values
  std::map<std::string, Ptr> _content;

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
  virtual StructuredValue::Ptr generate(StructuredValue &object) = 0;
  virtual ~Generator() {}
};

/**
 * Generates a path within a working folder
 */
class PathGenerator : public Generator {
 public:
  static const PathGenerator SINGLETON;

  virtual StructuredValue::Ptr generate(StructuredValue &object);
};

extern const PathGenerator &pathGenerator;

/**
 * Argument
 */
class Argument {
  /// The argument name
  std::string _name;

  /// The argument type
  std::shared_ptr<Type> _type;

  /// Help string (in Markdown syntax)
  std::string _help;

  /// Required
  bool _required;

  /// Default value
  Value _defaultValue;

  /// A generator
  Generator const *_generator;
 public:
  Argument(std::string const &name);

  std::string const &name() const { return _name; };

  void required(bool required);
  bool required() const { return _required; }

  void defaultValue(Value const &defaultValue) {
    _defaultValue = defaultValue;
  }
  Value defaultValue() const { return _defaultValue; }

  Generator const *generator() const { return _generator; }
  void generator(Generator const *generator) { _generator = generator; }

  const std::string &help() const;
  void help(const std::string &help);

  std::shared_ptr<Type> const &type() const { return _type; }
  void type(std::shared_ptr<Type> const &type) { _type = type; }
};

class Register;

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
  const TypeName _type;
  std::shared_ptr<Type> _parent;
  std::map<std::string, std::shared_ptr<Argument>> arguments;
  bool _predefined;
  friend class Register;
 public:
  /**
   * Creates a new type
   * @param type The typename
   * @param _parent The parent type (or null pointer)
   */
  Type(TypeName const &type, std::shared_ptr<Type> const &_parent = nullptr, bool predefined = false);
  virtual ~Type();

  /**
   * Add new arguments for this type
   * @param argument
   */
  void addArgument(std::shared_ptr<Argument> const &argument);

  /// Returns the JSON string corresponding to this type
  std::string toJson();

  /// Returns the type name
  TypeName const &typeName() const { return _type; }

  /// Return the type
  std::string toString() { return "type(" + _type.toString() + ")"; }

  /// Returns hash code (only based on type name)
  int hash() const;

  /// Predefined types
  inline bool predefined() const { return _predefined; }

  /** Creates an object with a given type */
  virtual std::shared_ptr<Object> create() const {
    return nullptr;
  };
};

/**
 * A task can be executed
 */
class Task {
  /// The type for this task
  std::shared_ptr<Type> _type;
 public:
  /**
   * Defines a new task
   * @param identifier The task identifier
   * @param outputType The output type
   * @return
   */
  Task(std::shared_ptr<Type> const &type);

  /**
   * Execute a task given a configuration object
   * @param object The object corresponding to the task type
   */
  std::shared_ptr<Object> execute(std::shared_ptr<Object> const &object);

  /** Returns the type of this task */
  inline TypeName typeName() const { return _type->typeName(); }
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

extern const std::shared_ptr<Type> IntegerType;
extern const std::shared_ptr<Type> RealType;
extern const std::shared_ptr<Type> StringType;
extern const std::shared_ptr<Type> BooleanType;
extern const std::shared_ptr<Type> AnyType;
extern const std::shared_ptr<Type> PathType;


// ---
// --- Objects
// ---

/** Any generated object should inherit from this */
class Object {
  /// The associated structured value
  StructuredValue::Ptr _value;

  /// Type of the object
  std::shared_ptr<Type> _type;
 public:
  /** Creates a new object */
  Object();

  /** Virtual destructor */
  virtual ~Object();

  /** Sets the structured value (globally) */
  void setValue(StructuredValue::Ptr value);

  /** Sets a value  */
  virtual void setValue(std::string const &name, StructuredValue::Ptr value) {};

  /** Get the associated structured value */
  std::shared_ptr<StructuredValue const> getValue() const;

  /** String representation of the object */
  virtual std::string toString() const;

  /**
   * Seal the object
   */
  void seal();

  /** Set a value */
  template<typename _Value>
  inline void set(std::string const &key, _Value const &value) {
    set(key, std::make_shared<StructuredValue>(Value(value)));
  }

  /** Set a value */
  void set(std::string const &key, StructuredValue::Ptr const &value);

  /** Get type */
  inline std::shared_ptr<Type> type() const { return _type; }

  /** Get type */
  inline void type(std::shared_ptr<Type> const &type) { _type = type; }

  /** Transform to JSON */
  std::string json() const;
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
  std::shared_ptr<Task> getTask(TypeName const &typeName) const;

  /// Register a new type
  void addType(std::shared_ptr<Type> const &type);

  /// Find a type given a t ype name
  std::shared_ptr<Type> getType(TypeName const &typeName) const;

  /// Find a type given a t ype name
  std::shared_ptr<Type> getType(std::shared_ptr<Object const> const &object) const;

  /// Build a new object from parameters
  std::shared_ptr<Object> build(std::shared_ptr<StructuredValue> const &value) const;
};


}
