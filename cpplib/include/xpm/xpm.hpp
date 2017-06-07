
#ifndef EXPERIMAESTRO_XPM_HPP
#define EXPERIMAESTRO_XPM_HPP

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

SWIG_IMMUTABLE;
extern std::shared_ptr<Type> IntegerType;
extern std::shared_ptr<Type> RealType;
extern std::shared_ptr<Type> StringType;
extern std::shared_ptr<Type> BooleanType;
extern std::shared_ptr<Type> ArrayType;
extern std::shared_ptr<Type> AnyType;
extern std::shared_ptr<Type> PathType;

extern const std::string KEY_TYPE;
extern const std::string KEY_TASK;

extern const std::string KEY_PATH;
extern const std::string KEY_VALUE;
extern const std::string KEY_IGNORE;
extern const std::string KEY_DEFAULT;
extern const std::string KEY_RESOURCE;
SWIG_MUTABLE;


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

SWIG_IMMUTABLE;
extern const TypeName STRING_TYPE;
extern const TypeName BOOLEAN_TYPE;
extern const TypeName INTEGER_TYPE;
extern const TypeName REAL_TYPE;
extern const TypeName ARRAY_TYPE;
extern const TypeName ANY_TYPE;
extern const TypeName PATH_TYPE;
extern const TypeName RESOURCE_TYPE;
SWIG_MUTABLE;

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
  NONE, INTEGER, REAL, STRING, PATH, BOOLEAN
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
  typedef uint8_t Flags;
  enum class Flag : Flags {
    SEALED = 1, DEFAULT = 2, VALIDATED = 4, GENERATED = 8
  };

  typedef std::shared_ptr<Object> Ptr;

  /// Default constructor
  Object();

  /// Constructor from a map
  Object(std::map<std::string, std::shared_ptr<Object>> &map);

  /// Construct from a JSON object
  static std::shared_ptr<Object> createFromJson(Register &xpmRegister, nlohmann::json const &jsonValue);

  /// Returns true if objects are equal
  virtual bool equals(Object const &other) const;

  /// Returns the string
  virtual std::string asString();

  /// Returns the string
  virtual bool asBoolean();

  /// Returns an integer
  virtual long asInteger();

  /// Returns an integer
  virtual double asReal();

  /// Returns a path
  virtual Path asPath() const;

  /// Checks whether a key exists
  bool hasKey(std::string const &key) const;

  /** Get the standard output path
   *
   * @return The path corresponding to the standard output stream
   * @throws std::invalid_argument if the object is not a resource
   */
  Path outputPath() const;

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
  void validate(bool generate);

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
  void configure(bool generate = true);

  /**
   * Submit the underlying task to experimaestro server
   */
  void submit(bool send = true,
              std::shared_ptr<rpc::Launcher> const &launcher = nullptr,
              std::shared_ptr<rpc::LauncherParameters> const &launcherParameters = nullptr);

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

  /// Fill from JSON
  void fill(Register &xpmRegister, const nlohmann::json &jsonValue);

 private:
  /// Set flag
  void set(Flag flag, bool value);
  bool get(Flag flag) const;

  /// Associated task, if any
  std::shared_ptr<Task> _task;

  /// The register
  std::shared_ptr<Register> _register;

  friend class ObjectFactory;

  /// Whether this value is sealed or not
  Flags _flags;

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

  static std::shared_ptr<Generator> createFromJSON(nlohmann::json const &);
  virtual nlohmann::json toJson() const = 0;
};

/**
 * Generates a path within a working folder
 */
class PathGenerator : public Generator {
  /// Last component name
  std::string _name;
 public:
  static const std::string TYPE;
  PathGenerator(const char *s) : PathGenerator(std::string(s)) {}
  PathGenerator(std::string const & = "");
  PathGenerator(nlohmann::json const &);
  virtual std::shared_ptr<Object> generate(Object &object);
  virtual nlohmann::json toJson() const;
};

/**
 * Argument
 */
class Argument {
 public:
  typedef std::shared_ptr<Argument> Ptr;
  Argument();
  Argument(std::string const &name);

  std::string const &name() const;
  Argument &name(std::string const &name);

  Argument &required(bool required);
  bool required() const;

  Argument &defaultValue(std::shared_ptr<Object> const &defaultValue);
  std::shared_ptr<Object> defaultValue() const;

  std::shared_ptr<Generator> generator();
  std::shared_ptr<Generator> const &generator() const;
  Argument &generator(std::shared_ptr<Generator> const &generator);

  std::shared_ptr<Type> const &type() const;
  Argument &type(std::shared_ptr<Type> const &type);

  const std::string &help() const;
  Argument &help(const std::string &help);

 private:
  /// The argument name
  std::string _name;

  /// The argument type
  std::shared_ptr<Type> _type = AnyType;

  /// Help string (in Markdown syntax)
  std::string _help;

  /// Required
  bool _required;

  /// Default value
  std::shared_ptr<Object> _defaultValue;

  /// A generator
  std::shared_ptr<Generator> _generator;
};

/**
 * Object factory
 */
class ObjectFactory {
  /// The register
  std::shared_ptr<Register> _register;

 public:
  ObjectFactory(std::shared_ptr<Register> const &theRegister);
  virtual ~ObjectFactory() {}
  virtual std::shared_ptr<Object> create();
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
  typedef std::shared_ptr<Type> Ptr;
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
  std::unordered_map<std::string, std::shared_ptr<Argument>> &arguments();

  /**
   * Get the arguments
   */
  std::unordered_map<std::string, std::shared_ptr<Argument>> const &arguments() const;

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
  std::shared_ptr<Object> create(std::shared_ptr<ObjectFactory> const &defaultFactory);

  /** Can ignore */
  inline bool canIgnore() { return _canIgnore; }

  /** Get parent type */
  Ptr parentType();

  /** Set parent type */
  void parentType(Ptr const &type);

  /** Get placeholder status */
  bool placeholder() const { return _placeholder; }
  void placeholder(bool placeholder) { _placeholder = placeholder; }

  /** Get placeholder status */
  std::string const & description() const { return _description; }
  void description(std::string const &description) { _description = description; }

  /** Set a property */
  void setProperty(std::string const &name, Object::Ptr const &value);
  Object::Ptr getProperty(std::string const &name);

 private:
  const TypeName _type;
  /**
   * Parent type
   */
  std::shared_ptr<Type> _parent;

  /**
   * Argument of this type.
   */
  std::unordered_map<std::string, std::shared_ptr<Argument>> _arguments;

  /**
   * Properties of this type.
   * They are useful to characterize a type when building experiments
   */
  std::unordered_map<std::string, std::shared_ptr<Object>> _properties;

  std::string _description;
  bool _predefined;
  bool _canIgnore;
  bool _placeholder = false;

  std::shared_ptr<ObjectFactory> _factory;
};

class SimpleType : public Type {
  ValueType _valueType;
 public:
  SimpleType(TypeName const &tname, ValueType valueType, bool canIgnore = false)
      : Type(tname, nullptr, true, canIgnore), _valueType(valueType) {}
  inline ValueType valueType() { return _valueType; }
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
   * Defines a new task with a specific identifier
   * @param identifier The task identifier
   * @param type The output type
   * @return
   */
  Task(TypeName const &typeName, std::shared_ptr<Type> const &type);

  /**
   * Initialize a task with the same identifier as the type
   * @param type The output type, whose typename is used as the task identifier
   */
  Task(std::shared_ptr<Type> const &type);

  /**
   * Configure the object
   * @param object The object corresponding to the task type
   * @param send If false, the job will not be sent to the experimaestro server
   */
  void submit(std::shared_ptr<Object> const &object,
              bool send,
              std::shared_ptr<rpc::Launcher> const &launcher,
              std::shared_ptr<rpc::LauncherParameters> const &launcherParameters) const;

  /** Returns the type of this task */
  TypeName typeName() const;

  /** Returns the type of this task */
  Type::Ptr type();

  /** Sets the command line for the task */
  void commandline(CommandLine command);

  /** Gets the task identifier */
  TypeName const &identifier() const;

  /** Executes */
  void execute(std::shared_ptr<Object> const &value) const;

  /// Sets the object factory
  void objectFactory(std::shared_ptr<ObjectFactory> const &factory);

  /** Creates an object with a given type */
  std::shared_ptr<Object> create(std::shared_ptr<ObjectFactory> const &defaultFactory);

  /** Convert to JSON */
  nlohmann::json toJson();

  /** Get path generator for resource location */
  std::shared_ptr<PathGenerator> getPathGenerator() const;

  /** Gets the running status */
  static bool isRunning() { return _running; }
 private:
  /// Task identifier
  TypeName _identifier;

  /// The type for this task
  std::shared_ptr<Type> _type;

  /// The object factory
  std::shared_ptr<ObjectFactory> _factory;

  /// Command line
  CommandLine _commandLine;

  /// True if a task is running
  static bool _running;
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


// --- Useful functions

/**
 * Report progress when running a task
 *
 * It is also used to setup a notification thread that
 * will ping the server at regular interval to signal
 * that the job is still alive.
 */
void progress(float percentage);

}

#endif // EXPERIMAESTRO_XPM_HPP
