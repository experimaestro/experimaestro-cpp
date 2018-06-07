
#ifndef EXPERIMAESTRO_XPM_HPP
#define EXPERIMAESTRO_XPM_HPP

#include <memory>
#include <string>
#include <map>
#include <vector>
#include <unordered_map>

namespace xpm { 
  // Forward declarations
  class Parameters;
  class AbstractObjectHolder;
  class Type;
  class Object;
  class Job;
  class Task;
  class Register;
  struct Helper;
  class Resource;
  struct Digest;
  class Workspace;
  class GeneratorContext;
}

#include <xpm/json.hpp>
#include <xpm/commandline.hpp>
#include <xpm/value.hpp>

namespace xpm {

#ifdef SWIG
#define SWIG_MUTABLE %mutable;
#define SWIG_IMMUTABLE %immutable;
#else
#define SWIG_MUTABLE
#define SWIG_IMMUTABLE
#endif



SWIG_IMMUTABLE;
extern std::shared_ptr<Type> IntegerType;
extern std::shared_ptr<Type> RealType;
extern std::shared_ptr<Type> StringType;
extern std::shared_ptr<Type> BooleanType;
extern std::shared_ptr<Type> AnyType;
extern std::shared_ptr<Type> PathType;
extern const std::shared_ptr<Object> NULL_OBJECT;

extern const std::string KEY_TYPE;
extern const std::string KEY_TASK;
extern const std::string KEY_VALUE;
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

  /** Returns a typename prefix by this */
  TypeName operator()(std::string const &localname) const;

  std::string toString() const;

  TypeName array() const;

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
extern const TypeName ANY_TYPE;
extern const TypeName PATH_TYPE;
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

/**
 * "Native" object associated with a configuration  
 */
class Object {
public:
  /** Since this is a pure virtual object */
  virtual ~Object();

  /** Sets a value in the native object */
  virtual void setValue(std::string const &name, std::shared_ptr<Parameters> const & value) = 0;

  /** Run (if this is a task) */
  virtual void run();
};


/**
 * Experimaestro configuration structure.
 * 
 * A configuration can have:
 * 
 * - subconfigurations accessible through a dictionary
 * - a value (optional)
 * - an object
 */
class Parameters NOSWIG(: public std::enable_shared_from_this<Parameters>) {
 public:
  typedef uint8_t Flags;
  enum class Flag : Flags {
    SEALED = 1, 
    DEFAULT = 2, 
    VALIDATED = 4, 
    GENERATED = 8, //< 
    IGNORE = 16 //< This structured value should be ignored
  };

  typedef std::shared_ptr<Parameters> Ptr;

  /// Default constructor
  Parameters();

#ifndef SWIG
  /// Constructor from JSON
  Parameters(Register &xpmRegister, nlohmann::json const &jsonValue);
#endif

  /// Constructor from a map
  Parameters(std::map<std::string, std::shared_ptr<Parameters>> &map);

  /// Constructs from value
  Parameters(Value const & v);

  /// Construct from other (shallow copy)
  Parameters(Parameters const &other) = delete;


  /// Move constructor
  Parameters(Parameters &&other) = default;

  /// Destructor
  ~Parameters();

  /// Returns true if objects are equal
  bool equals(Parameters const &other) const;

  /// Checks whether a key exists
  bool hasKey(std::string const &key) const;

  /// Get access to one value
  std::shared_ptr<Parameters> set(const std::string &key, std::shared_ptr<Parameters> const &);

  /// Get access to one value
  std::shared_ptr<Parameters> get(const std::string &key);

  /// Seal the object
  void seal();

  /// Returns whether this object is sealed
  bool isSealed() const;

  /// Ignore the object when computing 
  bool ignore() const;

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
  nlohmann::json toJson();

  /**
   *  Whether this element can be ignored for digest computation
   */
  bool canIgnore();

  /**
   * Retrieve content
   */
  std::map<std::string, std::shared_ptr<Parameters>> const &content();


  /** Sets the task */
  void task(std::shared_ptr<Task> const &task);

  /** Sets the object */
  void object(std::shared_ptr<Object> const &object);

  /** Sets the object */
  std::shared_ptr<Object> object();

  /** Sets the task */
  std::shared_ptr<Task> task();

  /**
   * Add dependencies to a given job
   * @param job Job for which the dependencies have to be added
   * @param skipThis True if skipping this object
   */
  void addDependencies(Job & job, bool skipThis);

  /**
   * Validate values
   */
  void validate();

  /**
   * Generate values (default values, and generators)
   */
  void generate(GeneratorContext & context);

  /** Get type */
  std::shared_ptr<Type> type() const;

  /** Get type */
  void type(std::shared_ptr<Type> const &type);

  /** Configure the object (used when submitting a job)
   * <ol>
   * <li>Sets the value</li>
   * <li>Validate and generate values</li>
   * <li>Seal the object</li>
   * </ol>
   */
  void configure(Workspace & ws);

  /**
   * Copy the configuration
   */
  std::shared_ptr<Parameters> copy();

  /**
   * Compute the a digest for this configuration
   */
  std::array<unsigned char, DIGEST_LENGTH> digest() const;

  /// Get generating job
  std::shared_ptr<Job> const & job() const;

  /// Set generating job
  void job( std::shared_ptr<Job> const & _job);

  /// Create objects
  std::shared_ptr<Object> createObjects(xpm::Register &xpmRegister);



  /// @defgroup Access to value
  /// @{

  /// Returns true if the value is defined
  bool hasValue() const;

  /// Returns true if the value is defined and null
  bool null() const;

  nlohmann::json valueAsJson() const;
  ValueType valueType() const;

  void set(bool value);
  void set(long value);
  void set(std::string const & value, bool typeHint = false);
  void set(std::vector<std::shared_ptr<Parameters>> const & v);
  void set(YAML::Node const &node);

  /// Returns the size of the array
  size_t size() const;
  /// Append an element to the array
  void push_back(std::shared_ptr<Parameters> const & parameters);

  /// Append an element to the array
  std::shared_ptr<Parameters> operator[](size_t index);

  /// Returns the string
  std::string asString() const;

  /// Returns the string
  bool asBoolean() const;

  /// Returns an integer
  long asInteger() const;

  /// Returns an integer
  double asReal() const;

  /// Returns a path
  Path asPath() const;

  /// 

private:
  /** Sets a value for the associated object (if any)  */
  void setObjectValue(std::string const &name, std::shared_ptr<Parameters> const &value);

  /// Set flag
  void set(Flag flag, bool value);

  /// Get flag
  bool get(Flag flag) const;

  /**
   * Resource identifier.
   * 
   * This field is set when the corresponding task is submitted
   */
  std::shared_ptr<Job> _job;

  /// Associated object, if any
  std::shared_ptr<Object> _object;

  /// Associated task, if any
  std::shared_ptr<Task> _task;

  /// The associated value
  Value _value;

  /// Whether this value is sealed or not
  Flags _flags;

  /// Sub-values (map is used for sorted keys, necessary to compute a stable unique identifier)
  std::map<std::string, std::shared_ptr<Parameters>> _content;


  friend class ObjectFactory;
  friend struct Helper;
  friend class Task;
  friend struct Digest;
  friend class Register;
 protected:
  /// Type of the object
  std::shared_ptr<Type> _type;
};


// ---
// --- Type and parser
// ---

#ifndef SWIG

struct GeneratorLock {
  GeneratorLock(GeneratorContext * context, Parameters * configuration);
  inline operator bool() { return true; }
  GeneratorContext * context;
};

class GeneratorContext {
public:
  std::vector<Parameters *> stack;
  Workspace & workspace;
  
  GeneratorContext(Workspace & ws);
  GeneratorContext(Workspace & ws, std::shared_ptr<Parameters> const &sv);
  inline GeneratorLock enter(Parameters * configuration) {
    return GeneratorLock(this, configuration);
  }
};
#endif

/**
 * Generator for values
 */
class Generator {
 public:
  virtual std::shared_ptr<Parameters> generate(GeneratorContext const &context) = 0;
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
  virtual std::shared_ptr<Parameters> generate(GeneratorContext const &context);
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

  Argument &ignore(bool required);
  bool ignore() const;

  Argument &defaultValue(std::shared_ptr<Parameters> const &defaultValue);
  std::shared_ptr<Parameters> defaultValue() const;

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

  /// Ignore
  bool _ignore;

  /// Default value
  std::shared_ptr<Parameters> _defaultValue;

  /// A generator
  std::shared_ptr<Generator> _generator;
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
class Type NOSWIG(: public std::enable_shared_from_this<Type>) {
  friend class Register;
 public:
  typedef std::shared_ptr<Type> Ptr;
  
  /// Least common ancestor between two types
  static Type::Ptr lca(Type::Ptr const & a, Type::Ptr const & b);

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
  void setProperty(std::string const &name, Parameters::Ptr const &value);
  Parameters::Ptr getProperty(std::string const &name);

  /** Is it a scalar */
  virtual bool scalar() { return false; }

  /** Is it an array */
  virtual bool array() { return false; }

  /// Checks whether another type can be assigned as this type
  bool accepts(Type::Ptr const &other) const;


  /// Base type for any type
  static Type::Ptr const & any();

  /**
   * Creates a new type
   * @param type The typename
   * @param _parent The parent type (or null pointer)
   */
  Type(TypeName const &type, std::shared_ptr<Type> _parent = nullptr,
       bool predefined = false, bool canIgnore = false);

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
  std::unordered_map<std::string, std::shared_ptr<Parameters>> _properties;


  std::string _description;
  bool _predefined;
  bool _canIgnore;
  bool _placeholder = false;
};

class SimpleType : public Type {
  ValueType _valueType;
 public:
  SimpleType(TypeName const &tname, ValueType valueType, bool canIgnore = false);
  inline ValueType valueType() { return _valueType; }
  virtual bool scalar() override { return true; }
};

class ArrayType : public Type {
  Type::Ptr _componentType;
public:
  ArrayType(Type::Ptr const & componentType);
  virtual bool array() override { return true; }
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
