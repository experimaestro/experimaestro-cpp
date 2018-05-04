
#ifndef EXPERIMAESTRO_XPM_HPP
#define EXPERIMAESTRO_XPM_HPP

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
  class Job;
  class Task;
  class Register;
  struct Helper;
  class Resource;
  struct Digest;
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
extern ptr<Type> IntegerType;
extern ptr<Type> RealType;
extern ptr<Type> StringType;
extern ptr<Type> BooleanType;
extern ptr<Type> ArrayType;
extern ptr<Type> AnyType;
extern ptr<Type> PathType;
extern const ptr<Object> NULL_OBJECT;

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
  virtual void setValue(std::string const &name, ptr<StructuredValue> const & value) = 0;
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
class StructuredValue
#ifndef SWIG
    : public std::enable_shared_from_this<StructuredValue>
#endif
{
 public:
  typedef uint8_t Flags;
  enum class Flag : Flags {
    SEALED = 1, DEFAULT = 2, VALIDATED = 4, GENERATED = 8, IGNORE = 16
  };

  typedef ptr<StructuredValue> Ptr;

  /// Default constructor
  StructuredValue();

#ifndef SWIG
  /// Constructor from JSON
  StructuredValue(Register &xpmRegister, nlohmann::json const &jsonValue);
#endif

  /// Constructor from a map
  StructuredValue(std::map<std::string, ptr<StructuredValue>> &map);

  /// Constructs from value
  StructuredValue(Value const & v);

  /// Move constructor
  StructuredValue(StructuredValue &&other) = default;

  /// Construct from other (shallow copy)
  StructuredValue(StructuredValue const &other);

  /// Returns true if objects are equal
  bool equals(StructuredValue const &other) const;

  /// Checks whether a key exists
  bool hasKey(std::string const &key) const;

  /// Get access to one value
  ptr<StructuredValue> set(const std::string &key, ptr<StructuredValue> const &);

  /// Get access to one value
  ptr<StructuredValue> get(const std::string &key);

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
  std::map<std::string, ptr<StructuredValue>> const &content();

  /** Sets a value  */
  void setValue(std::string const &name, ptr<StructuredValue> const &value);

  /** Sets the task */
  void task(ptr<Task> const &task);

  /** Sets the object */
  void object(ptr<Object> const &object);

  /** Sets the object */
  ptr<Object> object();

  /** Sets the task */
  ptr<Task> task();

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
  ptr<Type> type() const;

  /** Get type */
  void type(ptr<Type> const &type);

  /** Configure the object (used when submitting a job)
   * <ol>
   * <li>Sets the value</li>
   * <li>Validate and generate values</li>
   * <li>Seal the object</li>
   * </ol>
   */
  void configure();

  /**
   * Copy the configuration
   */
  ptr<StructuredValue> copy();

  /**
   * Compute the a digest for this configuration
   */
  std::array<unsigned char, DIGEST_LENGTH> digest() const;

  /// Get value
  Value & value() { return _value; }

  /// Get resource
  inline ptr<Resource> const & resource() const { return _resource; }

  /// Set resource
  inline void resource( ptr<Resource> const & _resource) { this->_resource = _resource; }

 private:
  /// Create objects
  void createObjects(xpm::Register &xpmRegister);

  /// Set flag
  void set(Flag flag, bool value);

  /// Get flag
  bool get(Flag flag) const;

  /**
   * Resource identifier.
   * 
   * This field is set when the corresponding task is submitted
   */
  ptr<Resource> _resource;

  /// Associated object, if any
  ptr<Object> _object;

  /// Associated task, if any
  ptr<Task> _task;

  /// The associated value
  Value _value;

  /// Whether this value is sealed or not
  Flags _flags;

  /// Sub-values (map is used for sorted keys, necessary to compute a stable unique identifier)
  std::map<std::string, ptr<StructuredValue>> _content;


  friend class ObjectFactory;
  friend struct Helper;
  friend class Task;
  friend struct Digest;
  friend class Register;
 protected:
  /// Type of the object
  ptr<Type> _type;
};


// ---
// --- Type and parser
// ---

struct GeneratorLock {
  GeneratorLock(GeneratorContext * context, StructuredValue * configuration);
  inline operator bool() { return true; }
  GeneratorContext * context;
};

class GeneratorContext {
public:
  std::vector<StructuredValue *> stack;
  GeneratorContext();
  GeneratorContext(ptr<StructuredValue> const &sv);
  inline GeneratorLock enter(StructuredValue * configuration) {
    return GeneratorLock(this, configuration);
  }
};

/**
 * Generator for values
 */
class Generator {
 public:
  virtual ptr<StructuredValue> generate(GeneratorContext const &context) = 0;
  virtual ~Generator() {}

  static ptr<Generator> createFromJSON(nlohmann::json const &);
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
  virtual ptr<StructuredValue> generate(GeneratorContext const &context);
  virtual nlohmann::json toJson() const;
};

/**
 * Argument
 */
class Argument {
 public:
  typedef ptr<Argument> Ptr;
  Argument();
  Argument(std::string const &name);

  std::string const &name() const;
  Argument &name(std::string const &name);

  Argument &required(bool required);
  bool required() const;

  Argument &ignore(bool required);
  bool ignore() const;

  Argument &defaultValue(ptr<StructuredValue> const &defaultValue);
  ptr<StructuredValue> defaultValue() const;

  ptr<Generator> generator();
  ptr<Generator> const &generator() const;
  Argument &generator(ptr<Generator> const &generator);

  ptr<Type> const &type() const;
  Argument &type(ptr<Type> const &type);

  const std::string &help() const;
  Argument &help(const std::string &help);


 private:
  /// The argument name
  std::string _name;

  /// The argument type
  ptr<Type> _type = AnyType;

  /// Help string (in Markdown syntax)
  std::string _help;

  /// Required
  bool _required;

  /// Ignore
  bool _ignore;

  /// Default value
  ptr<StructuredValue> _defaultValue;

  /// A generator
  ptr<Generator> _generator;
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
  typedef ptr<Type> Ptr;
  /**
   * Creates a new type
   * @param type The typename
   * @param _parent The parent type (or null pointer)
   */
  Type(TypeName const &type, ptr<Type> _parent = nullptr,
       bool predefined = false, bool canIgnore = false, bool isArray = false);

  /** Type destruction */
  virtual ~Type();

  /**
   * Add new arguments for this type
   * @param argument
   */
  void addArgument(ptr<Argument> const &argument);

  /**
   * Get the arguments
   */
  std::unordered_map<std::string, ptr<Argument>> &arguments();

  /**
   * Get the arguments
   */
  std::unordered_map<std::string, ptr<Argument>> const &arguments() const;

  /// Returns the JSON string corresponding to this type
  std::string toJson() const;

  /// Returns the type name
  TypeName const &typeName() const;

  /// Return the type
  std::string toString() const;

  /// Is array
  bool isArray() const;

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
  void setProperty(std::string const &name, StructuredValue::Ptr const &value);
  StructuredValue::Ptr getProperty(std::string const &name);

  /// Checks whether another type can be assigned as this type
  bool accepts(Type::Ptr const &other) const;

 private:
  const TypeName _type;
  /**
   * Parent type
   */
  ptr<Type> _parent;

  /**
   * Argument of this type.
   */
  std::unordered_map<std::string, ptr<Argument>> _arguments;

  /**
   * Properties of this type.
   * They are useful to characterize a type when building experiments
   */
  std::unordered_map<std::string, ptr<StructuredValue>> _properties;

  std::string _description;
  bool _predefined;
  bool _canIgnore;
  bool _placeholder = false;
  bool _isArray = false;
};

class SimpleType : public Type {
  ValueType _valueType;
 public:
  SimpleType(TypeName const &tname, ValueType valueType, bool canIgnore = false)
      : Type(tname, nullptr, true, canIgnore), _valueType(valueType) {}
  inline ValueType valueType() { return _valueType; }
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
