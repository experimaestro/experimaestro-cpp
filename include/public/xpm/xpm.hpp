
#ifndef EXPERIMAESTRO_XPM_HPP
#define EXPERIMAESTRO_XPM_HPP

#include <memory>
#include <string>
#include <map>
#include <vector>
#include <unordered_map>
#include <functional>

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

private:
  /** Sets a value for the associated object (if any)  */
  void setObjectValue(std::string const &name, std::shared_ptr<Parameters> const &value);

  /// Set flag
  void set(Flag flag, bool value);

  /// Get flag
  bool get(Flag flag) const;

  /// For each child callback
  void foreachChild(std::function<void(std::shared_ptr<Parameters> const &)> f);

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

}

#endif // EXPERIMAESTRO_XPM_HPP
