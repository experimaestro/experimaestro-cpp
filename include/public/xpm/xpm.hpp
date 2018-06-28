#ifndef EXPERIMAESTRO_XPM_HPP
#define EXPERIMAESTRO_XPM_HPP

#include <memory>
#include <string>
#include <map>
#include <vector>
#include <unordered_map>
#include <functional>
#include <cstdint>

#include <xpm/scalar.hpp>

namespace xpm { 
  // Forward declarations
  class Value;
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
  struct CommandContext;

  class ScalarValue;
  class MapValue;
  class ArrayValue;
}

#include <xpm/json.hpp>
#include <xpm/commandline.hpp>

namespace xpm {

/// Useful for digest and type
enum class ParametersTypes : uint8_t {
  MAP = 0, ARRAY = 1, SCALAR = 2
};

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
  virtual void setValue(std::string const &name, std::shared_ptr<Value> const & value) = 0;

  /** Run (if this is a task) */
  virtual void run();

  /** Initialize the object */
  virtual void init();
};


/**
 * Value.
 * 
 * A configuration can have:
 * 
 * - subconfigurations accessible through a dictionary
 * - a value (optional)
 * - an object
 */
class Value NOSWIG(: public std::enable_shared_from_this<Value>) {
public:
  typedef uint8_t Flags;
  enum class Flag : Flags {
    SEALED = 1, 
    DEFAULT = 2, 
    VALIDATED = 4, 
    GENERATED = 8, //< 
    IGNORE = 16 //< This structured value should be ignored
  };

  typedef std::shared_ptr<Value> Ptr;

  /// Default constructor
  Value();

#ifndef SWIG
  /// Constructor from JSON
  static std::shared_ptr<Value> create(Register &xpmRegister, nlohmann::json const &jsonValue);
#endif

  /// Destructor
  virtual ~Value();

  /// Returns true if objects are equal
  virtual bool equals(Value const &other) const = 0;

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
   */
  virtual nlohmann::json toJson() const;

  /**
   *  Whether this element can be ignored for digest computation
   */
  bool canIgnore();


  /**
   * Add dependencies to a given job
   * @param job Job for which the dependencies have to be added
   * @param skipThis True if skipping this object
   */
  virtual void addDependencies(Job & job, bool skipThis);

  /**
   * Validate values
   */
  void validate();

  /**
   * Generate values (default values, and generators)
   */
  void generate(GeneratorContext & context);

  /** Get type */
  virtual std::shared_ptr<Type> type() const = 0;

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
  virtual std::shared_ptr<Value> copy() = 0;

  /**
   * Compute the a digest for this configuration
   */
  std::array<unsigned char, DIGEST_LENGTH> digest() const;

  /// Create objects
  virtual std::shared_ptr<Object> createObjects(xpm::Register &xpmRegister);

  /// Output JSON
  NOSWIG(virtual void outputJson(std::ostream &out, CommandContext & context) const = 0);

  /// Convert to map or throw an exception
  std::shared_ptr<MapValue> asMap();
  bool isMap() const;
  
  /// Convert to array  or throw an exception
  std::shared_ptr<ArrayValue> asArray();
  bool isArray() const;
  
  /// Convert to scalar or throw an exception
  std::shared_ptr<ScalarValue> asScalar();
  bool isScalar() const;

  /// Returns the tags (throw an error if a tag already exists)  
  std::map<std::string, Scalar> tags() const;

protected:
  /// Retrieve tags in this value or its descendants
  virtual void retrieveTags(std::map<std::string, Scalar> &tags) const;

  /// For each child callback
  virtual void foreachChild(std::function<void(std::shared_ptr<Value> const &)> f);

  /// Update digest
  virtual void updateDigest(Digest & digest) const = 0;

  /// Set flag
  void set(Flag flag, bool value);

  /// Get flag
  bool get(Flag flag) const;

  /// Validate this map
  virtual void _validate();

  /// Generate
  virtual void _generate(GeneratorContext &context);

  /// Whether this value is sealed or not
  Flags _flags;

  friend class ObjectFactory;
  friend struct Helper;
  friend class Task;
  friend struct Digest;
  friend class Register;

  friend class MapValue;
};

class MapValue : public Value {
public:
  MapValue();
  virtual ~MapValue();

  /** Sets the task */
  void task(std::shared_ptr<Task> const &task);

  /** Sets the object */
  void object(std::shared_ptr<Object> const &object);

  /** Sets the object */
  std::shared_ptr<Object> object();

  /** Sets the task */
  std::shared_ptr<Task> task();


  /// Checks whether a key exists
  bool hasKey(std::string const &key) const;

  /// Set one value (if map)
  std::shared_ptr<Value> set(const std::string &key, std::shared_ptr<Value> const &);

  /// Get access to one value (if map)
  std::shared_ptr<Value> get(const std::string &key);

  /** Set type */
  void type(std::shared_ptr<Type> const &type);
  virtual std::shared_ptr<Type> type() const override;

  virtual bool equals(Value const &other) const override;
  NOSWIG(virtual void outputJson(std::ostream &out, CommandContext & context) const override);

  virtual nlohmann::json toJson() const override;

  virtual std::shared_ptr<Object> createObjects(xpm::Register &xpmRegister) override;
  virtual void updateDigest(Digest & digest) const override;
  virtual std::shared_ptr<Value> copy() override;
  virtual void addDependencies(Job & job, bool skipThis) override;
  
  /// Get generating job
  std::shared_ptr<Job> const & job() const;

  /// Set generating job
  void job( std::shared_ptr<Job> const & _job);

protected:
  virtual void _validate() override;
  virtual void foreachChild(std::function<void(std::shared_ptr<Value> const &)> f) override;
  virtual void _generate(GeneratorContext &context) override;
private:
  /** Sets a value for the associated object (if any)  */
  void setObjectValue(std::string const &name, std::shared_ptr<Value> const &value);

  /// Type of the object
  std::shared_ptr<Type> _type;

  /**
   * Job associated with this map
   */
  std::shared_ptr<Job> _job;

  /// Associated object, if any
  std::shared_ptr<Object> _object;

  /// Associated task, if any
  std::shared_ptr<Task> _task;

  /// The content
  std::map<std::string, std::shared_ptr<Value>> _map;
  friend class Value;
};

class ArrayValue : public Value {
public:
  ArrayValue();
  virtual ~ArrayValue();

  /// Returns the size of the array or the map
  size_t size() const;

  /// Append an element to the array
  void push_back(std::shared_ptr<Value> const & parameters);

  /// Get an element of the array
  std::shared_ptr<Value> operator[](size_t index);

  virtual bool equals(Value const &other) const override;
  NOSWIG(virtual void outputJson(std::ostream &out, CommandContext & context) const override);
  virtual nlohmann::json toJson() const override;
  virtual void updateDigest(Digest & digest) const override;
  virtual std::shared_ptr<Value> copy() override;
  virtual std::shared_ptr<Type> type() const override;

protected:
  virtual void _validate() override;
  virtual void foreachChild(std::function<void(std::shared_ptr<Value> const &)> f) override;

private:
  /// Type of the object
  std::shared_ptr<Type> _type;

  std::vector<std::shared_ptr<Value>> _array;
  friend class Value;
};



/// A scalar value
class ScalarValue : public Value {
public:
  virtual ~ScalarValue() = default;
  /// Constructs from value
  ScalarValue(Scalar const & v);

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

  void set(bool value);
  void set(long value);
  void set(std::string const & value, bool typeHint = false);
  void set(YAML::Node const &node);

  /// Returns true if the value is defined
  bool hasValue() const;

  /// Returns true if the value is defined and null
  bool null() const;

  /// Tag this value
  void tag(std::string const &name);

  nlohmann::json toJson() const override;
  ScalarType valueType() const;

  virtual bool equals(Value const &other) const override;
  NOSWIG(virtual void outputJson(std::ostream &out, CommandContext & context) const override);
  virtual void updateDigest(Digest & digest) const override;
  
  virtual std::shared_ptr<Value> copy() override;
  virtual std::shared_ptr<Type> type() const override;

protected:

  virtual void retrieveTags(std::map<std::string, Scalar> &tags) const override;

private:
  /// The associated value
  Scalar _value;

  /// The tag name if any
  std::string _tag;

  friend class Value;
};






// ---
// --- Type and parser
// ---

#ifndef SWIG

struct GeneratorLock {
  GeneratorLock(GeneratorContext * context, Value * configuration);
  inline operator bool() { return true; }
  GeneratorContext * context;
};

class GeneratorContext {
public:
  std::vector<Value *> stack;
  Workspace & workspace;
  
  GeneratorContext(Workspace & ws);
  GeneratorContext(Workspace & ws, std::shared_ptr<Value> const &sv);
  inline GeneratorLock enter(Value * configuration) {
    return GeneratorLock(this, configuration);
  }
};
#endif

/**
 * Generator for values
 */
class Generator {
 public:
  virtual std::shared_ptr<Value> generate(GeneratorContext const &context) = 0;
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
  virtual std::shared_ptr<Value> generate(GeneratorContext const &context);
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

  Argument &ignored(bool required);
  bool ignored() const;

  Argument &defaultValue(std::shared_ptr<Value> const &defaultValue);
  std::shared_ptr<Value> defaultValue() const;

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
  std::shared_ptr<Type> _type;

  /// Help string (in Markdown syntax)
  std::string _help;

  /// Required
  bool _required;

  /// Ignore
  bool _ignored;

  /// Default value
  std::shared_ptr<Value> _defaultValue;

  /// A generator
  std::shared_ptr<Generator> _generator;
};




// --- Useful functions

/**
 * Report progress when running a task
 *
 * It is also used to setup a notification thread that
 * will ping the server at regular interval to signal
 * that the job is still alive.
 */
void progress(float percentage);


} // endns: xpm

#endif // EXPERIMAESTRO_XPM_HPP
