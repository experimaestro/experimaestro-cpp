#ifndef EXPERIMAESTRO_XPM_HPP
#define EXPERIMAESTRO_XPM_HPP

#include <memory>
#include <string>
#include <map>
#include <vector>
#include <unordered_map>
#include <functional>
#include <cstdint>

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
  struct CommandContext;

  class ScalarParameters;
  class MapParameters;
  class ArrayParameters;
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
  virtual void setValue(std::string const &name, std::shared_ptr<Parameters> const & value) = 0;

  /** Run (if this is a task) */
  virtual void run();

  /** Initialize the object */
  virtual void init();
};


/**
 * Parameters.
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
  static std::shared_ptr<Parameters> create(Register &xpmRegister, nlohmann::json const &jsonValue);
#endif

  /// Destructor
  virtual ~Parameters();

  /// Returns true if objects are equal
  virtual bool equals(Parameters const &other) const = 0;

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
  virtual std::shared_ptr<Parameters> copy() = 0;

  /**
   * Compute the a digest for this configuration
   */
  std::array<unsigned char, DIGEST_LENGTH> digest() const;

  /// Create objects
  virtual std::shared_ptr<Object> createObjects(xpm::Register &xpmRegister);

  /// Output JSON
  NOSWIG(virtual void outputJson(std::ostream &out, CommandContext & context) const = 0);

  /// Convert to map or throw an exception
  std::shared_ptr<MapParameters> asMap();
  bool isMap() const;
  
  /// Convert to array  or throw an exception
  std::shared_ptr<ArrayParameters> asArray();
  bool isArray() const;
  
  /// Convert to scalar or throw an exception
  std::shared_ptr<ScalarParameters> asScalar();
  bool isScalar() const;
  

protected:
  /// For each child callback
  virtual void foreachChild(std::function<void(std::shared_ptr<Parameters> const &)> f);

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

  friend class MapParameters;
};

class MapParameters : public Parameters {
public:
  MapParameters();
  virtual ~MapParameters();

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
  std::shared_ptr<Parameters> set(const std::string &key, std::shared_ptr<Parameters> const &);

  /// Get access to one value (if map)
  std::shared_ptr<Parameters> get(const std::string &key);

  /** Set type */
  void type(std::shared_ptr<Type> const &type);
  virtual std::shared_ptr<Type> type() const override;

  virtual bool equals(Parameters const &other) const override;
  NOSWIG(virtual void outputJson(std::ostream &out, CommandContext & context) const override);

  virtual nlohmann::json toJson() const override;

  virtual std::shared_ptr<Object> createObjects(xpm::Register &xpmRegister) override;
  virtual void updateDigest(Digest & digest) const override;
  virtual std::shared_ptr<Parameters> copy() override;
  virtual void addDependencies(Job & job, bool skipThis) override;
  
  /// Get generating job
  std::shared_ptr<Job> const & job() const;

  /// Set generating job
  void job( std::shared_ptr<Job> const & _job);

protected:
  virtual void _validate() override;
  virtual void foreachChild(std::function<void(std::shared_ptr<Parameters> const &)> f) override;
  virtual void _generate(GeneratorContext &context) override;

private:
  /** Sets a value for the associated object (if any)  */
  void setObjectValue(std::string const &name, std::shared_ptr<Parameters> const &value);

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
  std::map<std::string, std::shared_ptr<Parameters>> _map;
  friend class Parameters;
};

class ArrayParameters : public Parameters {
public:
  ArrayParameters();
  virtual ~ArrayParameters();

  /// Returns the size of the array or the map
  size_t size() const;

  /// Append an element to the array
  void push_back(std::shared_ptr<Parameters> const & parameters);

  /// Get an element of the array
  std::shared_ptr<Parameters> operator[](size_t index);

  virtual bool equals(Parameters const &other) const override;
  NOSWIG(virtual void outputJson(std::ostream &out, CommandContext & context) const override);
  virtual nlohmann::json toJson() const override;
  virtual void updateDigest(Digest & digest) const override;
  virtual std::shared_ptr<Parameters> copy() override;
  virtual std::shared_ptr<Type> type() const override;

protected:
  virtual void _validate() override;
  virtual void foreachChild(std::function<void(std::shared_ptr<Parameters> const &)> f) override;

private:
  /// Type of the object
  std::shared_ptr<Type> _type;

  std::vector<std::shared_ptr<Parameters>> _array;
  friend class Parameters;
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

  Argument &ignored(bool required);
  bool ignored() const;

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
  std::shared_ptr<Type> _type;

  /// Help string (in Markdown syntax)
  std::string _help;

  /// Required
  bool _required;

  /// Ignore
  bool _ignored;

  /// Default value
  std::shared_ptr<Parameters> _defaultValue;

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
