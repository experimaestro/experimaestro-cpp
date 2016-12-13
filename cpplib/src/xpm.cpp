
#include <sstream>
#include <iostream>
#include <unordered_set>
#include <fstream>

#include <openssl/sha.h>

#include <xpm/common.hpp>
#include <xpm/xpm.hpp>
#include <xpm/json.hpp>
#include <xpm/filesystem.hpp>
#include <xpm/context.hpp>
#include <xpm/rpc/objects.hpp>
#include <xpm/rpc/client.hpp>
#include "private.hpp"

DEFINE_LOGGER("xpm")

using nlohmann::json;
namespace {

/// Format a string
//template<typename ... Args>
//std::string stringFormat(const std::string &format, Args ... args) {
//  size_t size = snprintf(nullptr, 0, format.c_str(), args ...) + 1; // Extra space for '\0'
//  std::unique_ptr<char[]> buf(new char[size]);
//  snprintf(buf.get(), size, format.c_str(), args ...);
//  return std::string(buf.get(), buf.get() + size - 1); // We don't want the '\0' inside
//}

template<typename T>
void updateDigest(SHA_CTX &context, T const &value) {
  static_assert(std::is_pod<T>::value, "Expected a POD value");

  if (!SHA1_Update(&context, &value, sizeof(T))) {
    throw std::runtime_error("Error while computing SHA-1");
  }
}

void updateDigest(SHA_CTX &context, std::string const &value) {
  if (!SHA1_Update(&context, value.c_str(), value.size())) {
    throw std::runtime_error("Error while computing SHA-1");
  }
}
}

namespace xpm {

static const std::string ARGUMENT_TYPE = "$type";
static const std::string ARGUMENT_PATH = "$path";
static const std::string ARGUMENT_VALUE = "$value";
static const std::string ARGUMENT_IGNORE = "$ignore";
static const char *const DEFAULT_KEY = "$default";

static const TypeName STRING_TYPE("string");
static const TypeName BOOLEAN_TYPE("boolean");
static const TypeName INTEGER_TYPE("integer");
static const TypeName REAL_TYPE("real");
static const TypeName ARRAY_TYPE("array");

static const TypeName PATH_TYPE("path");
static const TypeName RESOURCE_TYPE("resource");

static const std::unordered_set<TypeName> IGNORED_TYPES = {PATH_TYPE, RESOURCE_TYPE};

sealed_error::sealed_error() : exception("Object is sealed: cannot modify") {}
argument_error::argument_error(const std::string &message) : exception(message) {}

struct Helper {
  static json convert(Value const &value) {
    switch (value.scalarType()) {
      case ValueType::STRING:return json(value._value.string);

      case ValueType::INTEGER:return json(value._value.integer);

      case ValueType::REAL:return json(value._value.real);

      case ValueType::BOOLEAN:return json(value._value.boolean);

      case ValueType::ARRAY: throw exception();

      case ValueType::OBJECT: throw exception();

      case ValueType::NONE:throw std::runtime_error("none has no type");
    }
  }

  static void updateDigest(SHA_CTX &context, Value value);

  static Value toValue(json const &j) {
    switch (j.type()) {
      case nlohmann::json::value_t::null: return Value();

      case nlohmann::json::value_t::object:throw std::runtime_error("Unexpected JSON: object as a " + ARGUMENT_VALUE);

      case nlohmann::json::value_t::discarded: throw std::runtime_error("Unexpected JSON: discarded value");

      case nlohmann::json::value_t::array: {
        ValueArray array;
        for (size_t i = 0; i < j.size(); ++i) {
          array[i] = StructuredValue::parse(j[i]);
        }
        return Value(std::move(array));
      }

      case nlohmann::json::value_t::string:return Value((std::string const &) j);

      case nlohmann::json::value_t::boolean:return Value((bool) j);

      case nlohmann::json::value_t::number_integer:
      case nlohmann::json::value_t::number_unsigned:return Value((long) j); // TODO: check this is OK

      case nlohmann::json::value_t::number_float:return Value((double) j);
    }
  }

  static bool equals(Value const &a, Value const &b) {
    if (a._type != b._type) return false;
    switch(a._type) {
      case ValueType::STRING:return a._value.string == b._value.string;

      case ValueType::INTEGER:return a._value.integer == b._value.integer;

      case ValueType::REAL:return a._value.real == b._value.real;

      case ValueType::BOOLEAN:return a._value.boolean == b._value.boolean;

      case ValueType::ARRAY: {
        if (a._value.array.size() != b._value.array.size()) return false;
        for(size_t i = 0; i < a._value.array.size(); ++i) {
          // TODO: implement
          throw exception("Cannot compare an array: not implemented");
        }
        return true;
      }

      case ValueType::OBJECT: throw exception("Cannot compare an object");

      case ValueType::NONE:throw std::runtime_error("none has no type");
    }
  }
};

bool operator==(Value const &a, Value const &b) {
  return Helper::equals(a, b);
}

// ---
// --- Type names
// ---

TypeName::TypeName(std::string const &name) : name(name) {}

std::string TypeName::toString() const {
  return name;
}

int TypeName::hash() const {
  return (int) std::hash<std::string>{}(name);
}

TypeName TypeName::call(std::string const &localname) const {
  return TypeName(name + "." + localname);
}

// ---
// --- Structured value
// ---

struct _StructuredValue {
 public:
  /// Whether this value is sealed or not
  bool _sealed;

  /// Scalar value
  Value _value;

  /// Sub-values (map is used for sorted keys, ensuring a consistent unique identifier)
  std::map<std::string, StructuredValue> _content;

  // Construct from JSON object
  _StructuredValue(json const &jsonValue) : _StructuredValue() {
    switch (jsonValue.type()) {
      case nlohmann::json::value_t::null:break;

      case nlohmann::json::value_t::discarded:break;

      case nlohmann::json::value_t::object:
        for (json::const_iterator it = jsonValue.begin(); it != jsonValue.end(); ++it) {
          if (it.key() == ARGUMENT_VALUE) {
            _value = Helper::toValue(it.value());
          } else {
            _content[it.key()] = StructuredValue(std::make_shared<_StructuredValue>(it.value()));
          }
        }
        break;

      case nlohmann::json::value_t::array:
      case nlohmann::json::value_t::string:
      case nlohmann::json::value_t::boolean:
      case nlohmann::json::value_t::number_integer:
      case nlohmann::json::value_t::number_unsigned:
      case nlohmann::json::value_t::number_float:_value = Helper::toValue(jsonValue);
    }
  }
  _StructuredValue() : _sealed(false), _value() {
  }

  _StructuredValue(Value &&scalar) : _sealed(false), _value(scalar) {
  }

  _StructuredValue(Value const &scalar) : _sealed(false), _value(scalar) {
  }

  _StructuredValue(std::map<std::string, StructuredValue> &map) : _sealed(false), _value(), _content(map) {
  }

  // Convert to JSON
  json convert() {
    // No content
    if (_content.empty()) {
      if (_value.simple()) {
        return Helper::convert(_value);
      }
      return nullptr;
    }

    // We have some values
    json o = json::object();
    for (auto const &entry: _content) {
      o[entry.first] = entry.second._this->convert();
    }
    if (_value.simple()) {
      o[ARGUMENT_VALUE] = Helper::convert(_value);
    }
    return o;
  }

  /// Internal digest function
  std::array<unsigned char, DIGEST_LENGTH> digest() const {
    SHA_CTX context;

    if (!SHA1_Init(&context)) {
      throw std::runtime_error("Error while initializing SHA-1");
    }

    // If this is a scalar, just ignores everything below
    if (_value.simple()) {
      // Signal a scalar
      ::updateDigest(context, _value.scalarType());

      // Hash value
      Helper::updateDigest(context, _value);

    } else {
      // Signal a full object
      ::updateDigest(context, ValueType::NONE);

      for (auto &item: _content) {
        auto const &key = item.first;

        if (key[0] == '$' && key != ARGUMENT_TYPE) {
          // Skip all keys begining by "$s" but $type and $task
          continue;
        }

        if (item.second.canIgnore()) {
          // Remove keys that can be ignored (e.g. paths)
          continue;
        }

        // Update digest with key
        ::updateDigest(context, key);

        // Update digest with *value digest* (this allows digest caching)
        ::updateDigest(context, item.second._this->digest());
      }
    }

    std::array<unsigned char, SHA_DIGEST_LENGTH> md;
    if (!SHA1_Final(md.__elems_, &context)) {
      throw std::runtime_error("Error while retrieving SHA-1");
    }

    return md;
  };

  static StructuredValue fromJson(json const &j) {
    return StructuredValue(std::make_shared<_StructuredValue>(j));
  }
};

const TypeName ANY_TYPE("any");

StructuredValue::StructuredValue() : _this(std::make_shared<_StructuredValue>()) {
}
StructuredValue::StructuredValue(Value &&scalar) : _this(std::make_shared<_StructuredValue>(scalar)) {
}
StructuredValue::StructuredValue(Value const &scalar) : _this(std::make_shared<_StructuredValue>(scalar)) {
}
StructuredValue::StructuredValue(std::map<std::string, StructuredValue> &map)
    : _this(std::make_shared<_StructuredValue>(map)) {
}

StructuredValue::StructuredValue(std::shared_ptr<_StructuredValue> const &ptr) : _this(ptr) {
}

bool StructuredValue::hasValue() const {
  return _this->_value.defined();
}

void StructuredValue::type(TypeName const &typeName) {
  _this->_content[ARGUMENT_TYPE] = Value(typeName.toString());
}

TypeName StructuredValue::type() const {
  auto value_ptr = _this->_content.find(ARGUMENT_TYPE);
  if (value_ptr != _this->_content.end()) {
    auto &_object = value_ptr->second;
    if (_object.hasValue()) {
      return TypeName(_object.value().toString());
    }
  }

  if (_this->_value.defined()) {
    return _this->_value.type();
  }

  // Type not defined
  return ANY_TYPE;
}

bool StructuredValue::hasKey(std::string const &key) const {
  return _this->_content.find(key) != _this->_content.end();
}

StructuredValue &StructuredValue::operator[](const std::string &key) {
  if (_this->_sealed) {
    throw sealed_error();
  }

  if (key == ARGUMENT_VALUE) throw argument_error("Cannot access directly to " + ARGUMENT_VALUE);
  return _this->_content[key];
}

StructuredValue const StructuredValue::operator[](const std::string &key) const {
  auto value = _this->_content.find(key);
  if (value == _this->_content.end()) throw std::out_of_range(key + " is not defined for object");
  return value->second;
}

Value StructuredValue::value() const {
  return _this->_value;
}

void StructuredValue::value(Value const &scalar) {
  if (_this->_sealed) {
    throw sealed_error();
  }
  _this->_value = scalar;
}

void StructuredValue::seal() {
  if (_this->_sealed) return;

  _this->_sealed = true;
  for (auto &item: _this->_content) {
    item.second.seal();
  }
}

bool StructuredValue::isSealed() const {
  return _this->_sealed;
}

bool StructuredValue::isDefault() const {
  auto it = _this->_content.find(DEFAULT_KEY);
  if (it != _this->_content.end()) {
    if (it->second.value().scalarType() == ValueType::BOOLEAN) {
      return it->second.value().getBoolean();
    }
  }
  return false;
}

bool StructuredValue::canIgnore() const {
  if (IGNORED_TYPES.count(type()) > 0) {
    return true;
  }

  // Is the ignore flag set?
  auto it = _this->_content.find(ARGUMENT_IGNORE);
  if (it != _this->_content.end()) {
    if (it->second.value().scalarType() == ValueType::BOOLEAN) {
      return it->second.value().getBoolean();
    }
  }

  // Is the value a default value?
  if (isDefault())
    return true;

  return false;
}

std::string StructuredValue::uniqueIdentifier() const {
  // Compute the digest
  auto array = _this->digest();

  // Transform into hexadecimal string
  std::string s;
  s.reserve(2 * array.size());

  std::array<char, 3> b;
  for (size_t i = 0; i < array.size(); ++i) {
    sprintf(b.__elems_, "%02x", array[i]);
    s += b.__elems_;
  }

  return s;
}

std::string StructuredValue::toJson() const {
  return _this->convert().dump();
}

StructuredValue StructuredValue::parse(std::string const &jsonString) {
  return StructuredValue(std::make_shared<_StructuredValue>(json::parse(jsonString)));
}


// ---
// --- Scalar values
// ---

typedef std::string stdstring;

Value::~Value() {
  switch (_type) {
    case ValueType::STRING:_value.string.~stdstring();
      break;
    default:
      // Do nothing for other values
      break;
  }
  _type = ValueType::NONE;
}

Value::Union::~Union() {
  // Does nothing: handled by Scalar
}

Value::Union::Union() {
  // Does nothing: handled by Scalar
}

Value::Value() : _type(ValueType::NONE) {
}

Value::Value(double value) : _type(ValueType::REAL) {
  _value.real = value;
}

Value::Value(long value) : _type(ValueType::INTEGER) {
  _value.integer = value;
}

Value::Value(int value) : _type(ValueType::INTEGER) {
  _value.integer = value;
}

Value::Value(std::shared_ptr<Object> const &object) : _type(ValueType::OBJECT) {
  new(&_value.object) std::shared_ptr<Object>(object);
  _value.object = object;
}

Value::Value(ValueArray &&value) : _type(ValueType::ARRAY) {
  new(&_value.array) ValueArray(value);
}

Value::Value(bool value) : _type(ValueType::BOOLEAN) {
  _value.boolean = value;
}

Value::Value(std::string const &value) : _type(ValueType::STRING) {
  // placement new
  new(&_value.string) std::string(value);
}

Value::Value(Value const &other) : _type(other._type) {
  switch (_type) {
    case ValueType::NONE:break;

    case ValueType::REAL:_value.real = other._value.real;
      break;

    case ValueType::INTEGER:_value.integer = other._value.integer;
      break;

    case ValueType::BOOLEAN:_value.boolean = other._value.boolean;
      break;

    case ValueType::STRING:new(&_value.string) std::string(other._value.string);
      break;

    case ValueType::ARRAY:new(&_value.array) ValueArray(other._value.array);
      break;

    case ValueType::OBJECT:new(&_value.array) std::shared_ptr<Object>(other._value.object);
      break;
  }

}

Value &Value::operator=(Value const &other) {
  this->~Value();

  _type = other._type;
  switch (_type) {
    case ValueType::NONE:break;
    case ValueType::REAL:_value.real = other._value.real;
      break;
    case ValueType::INTEGER:_value.integer = other._value.integer;
      break;
    case ValueType::BOOLEAN:_value.boolean = other._value.boolean;
      break;
    case ValueType::STRING:new(&_value.string) std::string(other._value.string);
      break;
    case ValueType::ARRAY:new(&_value.array) ValueArray(other._value.array);
      break;
    case ValueType::OBJECT:new(&_value.object) std::shared_ptr<Object>(other._value.object);
      break;
  }

  return *this;
}

std::string Value::toString() const {
  switch (_type) {
    case ValueType::NONE:return "<none>";
    case ValueType::REAL:return std::to_string(_value.real);
    case ValueType::INTEGER:return std::to_string(_value.integer);
    case ValueType::BOOLEAN:return _value.boolean ? "true" : "false";
    case ValueType::STRING:return _value.string;
    case ValueType::ARRAY:return "[]";
    case ValueType::OBJECT:return _value.object->toString();
  }
}

TypeName const &Value::type() const {
  switch (_type) {
    case ValueType::STRING:return STRING_TYPE;

    case ValueType::INTEGER:return INTEGER_TYPE;

    case ValueType::REAL:return REAL_TYPE;

    case ValueType::BOOLEAN:return BOOLEAN_TYPE;

    case ValueType::ARRAY: return ARRAY_TYPE;

    case ValueType::OBJECT: return _value.object->type().typeName();

    case ValueType::NONE:throw std::runtime_error("none has no type");
  }
}

bool Value::defined() const {
  return _type != ValueType::NONE;
}

bool Value::simple() const {
  return _type != ValueType::NONE && _type != ValueType::OBJECT;
}

ValueType const Value::scalarType() const {
  return _type;
}

bool Value::getBoolean() const {
  if (_type != ValueType::BOOLEAN) throw std::runtime_error("Value is not a boolean");
  return _value.boolean;
}

std::string const &Value::getString() const {
  if (_type != ValueType::STRING) throw std::runtime_error("Value is not a string");
  return _value.string;
}




// ---
// --- Task
// ---

struct _Argument {
  /// The argument name
  std::string _name;

  /// The argument type
  Type _type;

  /// Help string (in Markdown syntax)
  std::string _help;

  /// Required
  bool _required;

  /// Default value
  Value _defaultValue;

  /// A generator
  Generator const *_generator;

  _Argument(std::string const &name) : _name(name), _type(AnyType), _required(true), _generator(nullptr) {

  }
};

Argument::Argument(std::string const &name) : _this(std::make_shared<_Argument>(name)) {
}

Argument::Argument() : _this(std::make_shared<_Argument>("")) {
}

std::string const &Argument::name() const {
  return _this->_name;
}

bool Argument::required() const { return _this->_required; }

void Argument::required(bool required) {
  _this->_required = required;
}
const std::string &Argument::help() const {
  return _this->_help;
}
void Argument::help(const std::string &help) {
  _this->_help = help;
}

void Argument::defaultValue(Value const &defaultValue) {
  _this->_defaultValue = defaultValue;
  if (defaultValue.simple()) _this->_required = false;
}
Value Argument::defaultValue() const { return _this->_defaultValue; }

Generator const *Argument::generator() const { return _this->_generator; }
void Argument::generator(Generator const *generator) { _this->_generator = generator; }

Type const &Argument::type() const { return _this->_type; }
void Argument::type(Type const &type) { _this->_type = type; }



// ---- Type


Type BooleanType(BOOLEAN_TYPE, nullptr, true);
Type IntegerType(INTEGER_TYPE, nullptr, true);
Type RealType(REAL_TYPE, nullptr, true);
Type StringType(STRING_TYPE, nullptr, true);
Type AnyType(ANY_TYPE, nullptr, true);
Type PathType(PATH_TYPE, nullptr, true);

struct _Type {
  const TypeName _type;
  std::shared_ptr<_Type> _parent;
  std::map<std::string, Argument> arguments;
  bool _predefined;
  std::shared_ptr<ObjectFactory> _factory;

  friend class Register;

  _Type(TypeName const &type, Type *parent, bool predefined)
      : _type(type), _parent(parent ? parent->_this : nullptr), _predefined(predefined) {}

  std::shared_ptr<Object> create() const {
    if (_factory) {
      return _factory->create();
    }
    return std::make_shared<Object>();
  }
};

/** Creates an object with a given type */
std::shared_ptr<Object> Type::create() const {
  const std::shared_ptr<Object> object = _this->create();
  object->type(*this);
  return object;
}

Type::Type(TypeName const &type, Type *parent, bool predefined) :
    _this(std::make_shared<_Type>(type, parent, predefined)) {}

Type::Type() {}
Type::~Type() {}

void Type::objectFactory(std::shared_ptr<ObjectFactory> const &factory) {
  _this->_factory = factory;
}

std::shared_ptr<ObjectFactory> const &Type::objectFactory() {
  return _this->_factory;
}

void Type::addArgument(Argument &argument) {
  _this->arguments[argument.name()] = argument;
}

std::map<std::string, Argument> &Type::arguments() {
  return _this->arguments;
}

TypeName const &Type::typeName() const { return _this->_type; }

/// Return the type
std::string Type::toString() const { return "type(" + _this->_type.toString() + ")"; }

/// Predefined types
bool Type::predefined() const { return _this->_predefined; }

std::string Type::toJson() const {
  json response;

  json jsonArguments = json::object();
  for (auto const &entry: _this->arguments) {
    Argument const &arg = entry.second;
    json definition = {
        {"help", arg.help()},
        {"required", arg.required()},
        {"type", arg.type().typeName().toString()},
    };

    if (arg.defaultValue().defined())
      definition["default"] = Helper::convert(arg.defaultValue());

    jsonArguments[entry.first] = definition;
  }

  response["arguments"] = jsonArguments;
  response["type"] = typeName().toString();
  if (_this->_parent) {
    response["parent"] = _this->_parent->_type.toString();
  }
  return response.dump();
}
int Type::hash() const {
  return std::hash<Type>()(*this);
}

// ---- Generators


const PathGenerator PathGenerator::SINGLETON = PathGenerator();
const PathGenerator &pathGenerator = PathGenerator::SINGLETON;

StructuredValue PathGenerator::generate(StructuredValue object) const {
  auto p = Path(Context::current().workdir(), {"yo", object.uniqueIdentifier()});
  return StructuredValue(p.toString());
}

// ---- Object


Object::Object() {
}

Object::~Object() {
}

/** Get type */
Type Object::type() const { return _type; }

/** Get type */
void Object::type(Type const &type) { _type = type; }

/** Sets the structured value */
void Object::setValue(StructuredValue &value) {
  _value = value;
}

void Object::set(std::string const &key, StructuredValue value) {
  // Set the value in the map
  _value[key] = value;

  // And for the object
  setValue(key, value);
}

StructuredValue const Object::getValue() const {
  return _value;
}

void Object::task(Task &task) {
  _task = task;
}

void Object::submit() {
  if (!_task) {
    throw exception("No underlying task for this object: cannot run");
  }
  return _task->submit(shared_from_this());
}

std::string Object::toString() const {
  return "Object of type " + _type.toString();
}

std::string Object::json() const {
  return _value.toJson();
}

void Object::seal() {
  _value.seal();
}

void Object::configure(StructuredValue value) {
  setValue(value);
  validate();
  seal();
}

void Object::validate() {
  // First validate
  for (auto entry: _type.arguments()) {
    const Argument &argument = entry.second;

    if (!_value.hasKey(argument.name())) {
      // No value provided
      if (argument.required() && !argument.generator()) {
        if (!_value.hasKey(argument.name())) {
          throw argument_error(
              "Argument " + argument.name() + " was required but not given for " + this->type().toString());
        }
      } else {
        LOGGER->info("Setting default value...");
        StructuredValue value(argument.defaultValue());
        value[DEFAULT_KEY] = Value(true);
        set(argument.name(), value);
      }
    } else {
      // Sets the value
      LOGGER->info("Setting value...");
      auto &value = _value[argument.name()];
      if (value.value().simple() && value.value() == argument.defaultValue()) {
        value[DEFAULT_KEY] = Value(true);
      }
      setValue(argument.name(), value);
    }
  }

  // Generate
  for (auto entry: _type.arguments()) {
    const Argument &argument = entry.second;
    Generator const *generator = argument.generator();

    if (!_value.hasKey(argument.name()) && generator) {
      LOGGER->info("Generating value...");
      set(argument.name(), generator->generate(_value));
    }
  }
}

void Object::execute() {
  LOGGER->info("No execute method provided");
}


// ---- Task

template<>
struct Reference<Task> {
  /// Task identifier
  TypeName identifier;

  /// The type for this task
  Type type;

  /// Command line
  CommandLine commandLine;

  /// The object factory
  std::shared_ptr<ObjectFactory> factory;

  Reference(const TypeName &identifier, const Type &type) : identifier(identifier), type(type) {}
};

Task::Task(Type &type) : Pimpl(type.typeName(), type) {
}

Task::Task() : Pimpl(TypeName(""), ANY_TYPE) {
}

TypeName Task::typeName() const { return self(this).type.typeName(); }

void Task::submit(std::shared_ptr<Object> const &object) {
  // Validate and seal the task object
  object->validate();
  object->seal();

  // Get generated directory as locator

  auto locator = rpc::Path::toPath(PathGenerator::SINGLETON.generate(object->getValue()).value().getString());

  // Prepare the command line
  CommandContext context;
  context.parameters = object->getValue().toString();
  rpc::CommandLineTask::submitJob(locator, self(this).commandLine.rpc(context));
}

void Task::commandline(CommandLine command) {
  self(this).commandLine = command;
}

TypeName const &Task::identifier() {
  return self(this).identifier;
}

void Task::objectFactory(std::shared_ptr<ObjectFactory> const &factory) {
  self(this).factory = factory;
}

std::shared_ptr<Object> Task::create() const {
  if (!self(this).factory) {
    throw argument_error("Task has no factory");
  }
  auto ptr = self(this).factory->create();
  ptr->type(self(this).type);
  return ptr;
}

void Task::execute(StructuredValue value) const {
  auto object = create();
  object->configure(value);
  object->execute();
}

// ---- REGISTER

Register::Register() {
  addType(IntegerType);
  addType(RealType);
  addType(StringType);
  addType(BooleanType);
}
Register::~Register() {}

void Register::addType(Type &type) {
  _types[type.typeName()] = type;
}

void Register::addTask(Task &task) {
  _tasks[task.identifier()] = task;
}

optional<Task const> Register::getTask(TypeName const &typeName) const {
  auto it = _tasks.find(typeName);
  if (it != _tasks.end()) {
    return it->second;
  }
  return optional<Task const>();
}

optional<Type const> Register::getType(TypeName const &typeName) const {
  auto it = _types.find(typeName);
  if (it != _types.end()) {
    return it->second;
  }
  return optional<Type const>();
}

// Find a type given a type name
Type Register::getType(Object const &object) const {
  return object.type();
}

std::shared_ptr<Object> Register::build(StructuredValue &value) const {
  auto const &objectTypeName = value.type();
  auto _type = getType(objectTypeName);

  std::shared_ptr<_Type> objectType = _type ? _type->_this : nullptr;
  std::cerr << "Building object with type " << objectTypeName.toString()
            << " / " << (objectType ? "OK" : "not registered!") << std::endl;

  // Create the object
  std::cerr << "Creating object..." << std::endl;
  auto object = objectType ? objectType->create() : std::make_shared<Object>();

  std::cerr << "Created object... " << object.get() << std::endl;
  object->setValue(value);

  if (!object) {
    throw std::runtime_error("Object of type " + objectTypeName.toString() + " was not created");
  }

  // Loop over all the type hierarchy
  for (auto type = objectType; type; type = type->_parent) {

    // Loop over the arguments
    for (auto entry: type->arguments) {
      auto key = entry.first;

      // Check required argument
      auto const hasKey = value.hasKey(key);
      if (!hasKey && entry.second.required()) {
        throw argument_error("Argument " + key + " was required but not provided");
      }

      // Build subtype
      if (hasKey) {
        std::cerr << "Building " << key << std::endl;
        auto subvalue = build(value[key]);

        // Set argument
        std::cerr << "Setting " << key << std::endl;
        object->set(key, subvalue);
      } else {
        auto scalar = entry.second.defaultValue();
        if (scalar.defined()) {
          object->set(key, StructuredValue(scalar));
        }
      }
    }
  }

  return object;
}

void Register::parse(std::vector<std::string> const &args) {
  if (args.size() < 1) {
    throw argument_error("Expected at least one argument (use help to get some help)");
  }

  if (args[0] == "help") {
    return;
  }

  if (args[0] == "generate") {
    std::cout << "{";
    std::cout << R"("types": [)" << std::endl;
    bool first = true;
    for (auto const &type: _types) {
      if (!type.second.predefined()) {
        if (!first) std::cout << ","; else first = false;
        std::cout << type.second.toJson() << std::endl;
      }
    }
    std::cout << "]" << std::endl;

    std::cout << R"("tasks": [)" << std::endl;
    first = true;
    for (auto const &type: _types) {
      if (!type.second.predefined()) {
        if (!first) std::cout << ","; else first = false;
        std::cout << type.second.toJson() << std::endl;
      }
    }
    std::cout << "]" << std::endl;

    std::cout << "}" << std::endl;
    return;
  }

  if (args[0] == "run") {
    // Retrieve the task
    std::string taskName = args[1];
    auto task = this->getTask(TypeName(taskName));
    if (!task) {
      throw argument_error(taskName + " is not a task");

    }

    // Retrieve the structured value
    // TODO:
    // - process other arguments (SV)
    // - process command lines
    auto stream = std::ifstream(args[2]);
    if (!stream) {
      throw argument_error(args[2] + " is not a file");
    }
    json j = json::parse(stream);
    StructuredValue value = _StructuredValue::fromJson(j);

    // Run the task
    task->execute(value);

    return;

  }

  throw argument_error("Unexpected command: " + args[0]);
}

void Helper::updateDigest(SHA_CTX &context, Value value) {
  {
    switch (value.scalarType()) {
      case ValueType::STRING: ::updateDigest(context, value._value.string);
        break;

      case ValueType::INTEGER: ::updateDigest(context, value._value.integer);
        break;

      case ValueType::REAL: ::updateDigest(context, value._value.real);
        break;

      case ValueType::BOOLEAN: ::updateDigest(context, value._value.boolean);
        break;

      case ValueType::ARRAY:
        for (const auto &x: value._value.array) {
          auto xDigest = x._this->digest();
          ::updateDigest(context, xDigest);
        }
        break;

      case ValueType::OBJECT:
      case ValueType::NONE:
        // Do nothing
        break;

    }
  }
}

} // xpm namespace
