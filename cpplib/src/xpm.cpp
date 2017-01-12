
#include <sstream>
#include <iostream>
#include <unordered_set>
#include <fstream>

#include <openssl/sha.h>

#include <xpm/common.hpp>
#include <xpm/xpm.hpp>
#include <xpm/context.hpp>
#include <xpm/rpc/client.hpp>
#include "private.hpp"
#include <spdlog/fmt/ostr.h>

DEFINE_LOGGER("xpm")

using nlohmann::json;

/// Format a string
//template<typename ... Args>
//std::string stringFormat(const std::string &format, Args ... args) {
//  size_t size = snprintf(nullptr, 0, format.c_str(), args ...) + 1; // Extra space for '\0'
//  std::unique_ptr<char[]> buf(new char[size]);
//  snprintf(buf.get(), size, format.c_str(), args ...);
//  return std::string(buf.get(), buf.get() + size - 1); // We don't want the '\0' inside
//}


namespace xpm {

// Type and task should be included
static const std::string KEY_TYPE = "$type";
static const std::string KEY_TASK = "$task";

static const std::string KEY_PATH = "$path";
static const std::string KEY_VALUE = "$value";
static const std::string KEY_IGNORE = "$ignore";
static const std::string KEY_DEFAULT = "$default";
static const std::string KEY_RESOURCE = "$resource";

static const auto RESTRICTED_KEYS = std::unordered_set<std::string> { KEY_TYPE, KEY_TASK, KEY_VALUE, KEY_DEFAULT };

static const TypeName STRING_TYPE("string");
static const TypeName BOOLEAN_TYPE("boolean");
static const TypeName INTEGER_TYPE("integer");
static const TypeName REAL_TYPE("real");
static const TypeName ARRAY_TYPE("array");
static const TypeName ANY_TYPE("any");

static const TypeName PATH_TYPE("path");
static const TypeName RESOURCE_TYPE("resource");

static const std::unordered_set<TypeName> IGNORED_TYPES = {PATH_TYPE, RESOURCE_TYPE};

sealed_error::sealed_error() : exception("Object is sealed: cannot modify") {}
argument_error::argument_error(const std::string &message) : exception(message) {}
cast_error::cast_error(const std::string &message) : exception(message) {}
not_implemented_error::not_implemented_error(const std::string &message,
                                             const std::string &file, int line) : exception(
    "Not implemented: " + message + ", file " + file + ":" + std::to_string(line)) {}

  std::ostream& operator<<(std::ostream& os, const TypeName& c) {
      return os << c.toString();
  }
  std::ostream& operator<<(std::ostream& os, const Type& c) {
      return os << c.toString();
  }


namespace {

struct Digest {
  SHA_CTX context;

  Digest() {
    if (!SHA1_Init(&context)) {
      throw std::runtime_error("Error while initializing SHA-1");
    }
  }
  template<typename T>
  void updateDigest(T const &value) {
    static_assert(std::is_pod<T>::value, "Expected a POD value");

    if (!SHA1_Update(&context, &value, sizeof(T))) {
      throw std::runtime_error("Error while computing SHA-1");
    }
  }

  void updateDigest(std::string const &value) {
    if (!SHA1_Update(&context, value.c_str(), value.size())) {
      throw std::runtime_error("Error while computing SHA-1");
    }
  }

  std::array<unsigned char, SHA_DIGEST_LENGTH> get() {
    std::array<unsigned char, SHA_DIGEST_LENGTH> md;
    if (!SHA1_Final(md.__elems_, &context)) {
      throw std::runtime_error("Error while retrieving SHA-1");
    }
    return md;
  }
};
}

struct Helper {
  static bool equals(Value const &a, Value const &b) {
    if (a._scalarType != b._scalarType) return false;
    switch (a._scalarType) {
      case ValueType::STRING:return a._value.string == b._value.string;

      case ValueType::INTEGER:return a._value.integer == b._value.integer;

      case ValueType::REAL:return a._value.real == b._value.real;

      case ValueType::BOOLEAN:return a._value.boolean == b._value.boolean;

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

std::string TypeName::localName() const {
  const auto i = name.rfind(".");
  if (i == std::string::npos) return name;
  return name.substr(i + 1);
}

// ---
// --- Structured value
// ---

Object::Object() : _sealed(false), _default(false), _type(AnyType) {
}

Object::Object(std::map<std::string, std::shared_ptr<Object>> &map)
    : _sealed(false), _default(false), _content(map) {
}

Object::Object(Object const &other) : _sealed(other._sealed), _default(other._default), _content(other._content) {
}

Object::~Object() {}



std::shared_ptr<Object> Object::createFromJson(Register &xpmRegister, nlohmann::json const &jsonValue) {
  switch (jsonValue.type()) {
    case nlohmann::json::value_t::object: {
      std::shared_ptr<Object> object;

      auto _key = jsonValue.find(KEY_VALUE);
      if (_key != jsonValue.end()) {
        object = createFromJson(xpmRegister, *_key);
      } else {
        std::shared_ptr<Type> type;
        if (jsonValue.count(KEY_TYPE) > 0) {
          auto typeName = TypeName((std::string const &) jsonValue[KEY_TYPE]);
          type = xpmRegister.getType(typeName);
          if (!type) {
            LOGGER->debug("Could not find type {} in registry", typeName);
          }
        }

        if (type) {
          LOGGER->debug("Creating object of type {} using type->create()", *type);
          object = type->create();
        } else {
          object = std::make_shared<Object>();
        }
      }

      for (json::const_iterator it = jsonValue.begin(); it != jsonValue.end(); ++it) {
        if (it.key() == KEY_VALUE) {
          // already handled
        } else if (it.key() == KEY_TYPE) {
          object->_type = xpmRegister.getType(TypeName((std::string const &)it.value()));
        } else if (it.key() == KEY_DEFAULT) {
          object->_default = it.value();
        } else {
          object->set(it.key(), createFromJson(xpmRegister, it.value()));
        }
      }

      return object;
    }

    case nlohmann::json::value_t::array: {
      auto array = std::make_shared<Array>();
      for (json::const_iterator it = jsonValue.begin(); it != jsonValue.end(); ++it) {
        array->add(createFromJson(xpmRegister, *it));
      }
      return array;
    }

    case nlohmann::json::value_t::null:
    case nlohmann::json::value_t::discarded:return std::make_shared<Value>();

    case nlohmann::json::value_t::string:return std::make_shared<Value>((std::string const &) jsonValue);

    case nlohmann::json::value_t::boolean:return std::make_shared<Value>((bool) jsonValue);

    case nlohmann::json::value_t::number_integer:
    case nlohmann::json::value_t::number_unsigned:return std::make_shared<Value>((long) jsonValue);

    case nlohmann::json::value_t::number_float:return std::make_shared<Value>((double) jsonValue);
  }
}

std::string Object::asString() {
  throw cast_error("Cannot convert to string");
}
bool Object::asBoolean() {
  throw cast_error("Cannot convert to boolean");
}
long Object::asInteger() {
  throw cast_error("Cannot convert to integer");
}
double Object::asReal() {
  throw cast_error("Cannot convert to real");
}

// Convert to JSON
json Object::toJson() {
  // No content
  if (_content.empty() && !_task && (!_type || dynamic_cast<Value*>(this)) && !_default) {
    return nullptr;
  }

  // We have some values
  json o = json::object();
  for (auto const &entry: _content) {
    o[entry.first] = entry.second->toJson();
  }

  if (_default)
    o[KEY_DEFAULT] = true;
  if (_type) {
    o[KEY_TYPE] = _type->typeName().toString();
  }

  return o;
}

std::shared_ptr<Object> Object::copy() {
  return std::make_shared<Object>(*this);
}

std::string Object::toJsonString() {
  return toJson().dump();
}

void Object::setValue(std::shared_ptr<Object> const &value) {
 NOT_IMPLEMENTED();
}

/// Internal digest function
std::array<unsigned char, DIGEST_LENGTH> Object::digest() const {
  Digest d;

  // Signal a full object

  for (auto &item: _content) {
    auto const &key = item.first;

    if (key[0] == '$' && key != KEY_TYPE && key != KEY_TASK) {
      // Skip all keys begining by "$s" but $type and $task
      continue;
    }

    if (item.second->canIgnore()) {
      // Remove keys that can be ignored (e.g. paths)
      continue;
    }

    // Update digest with key
    d.updateDigest(key);

    // Update digest with *value digest* (this allows digest caching)
    d.updateDigest(item.second->digest());
  }

  return d.get();
};

std::shared_ptr<Type> Object::type() {
  if (_type) return _type;
  return AnyType;
}

bool Object::hasKey(std::string const &key) const {
  return _content.find(key) != _content.end();
}

std::shared_ptr<Object> Object::set(const std::string &key, std::shared_ptr<Object> const &value) {
  if (_sealed) {
    throw sealed_error();
  }

  if (RESTRICTED_KEYS.count(key) > 0)
    throw argument_error("Cannot access directly to " + key);

  auto it = _content.find(key);
  _content[key] = value;

  // And for the object
  setValue(key, value);

  return it == _content.end() ? nullptr : it->second;
}

std::shared_ptr<Object> Object::get(const std::string &key) {
  auto value = _content.find(key);
  if (value == _content.end()) throw std::out_of_range(key + " is not defined for object");
  return value->second;
}

void Object::seal() {
  if (_sealed) return;

  _sealed = true;
  for (auto &item: _content) {
    item.second->seal();
  }
}

bool Object::isSealed() const {
  return _sealed;
}

bool Object::isDefault() const {
  return _default;
}

bool Object::canIgnore() {
  if (type()->canIgnore()) {
    return true;
  }

  // Is the ignore flag set?
  auto it = _content.find(KEY_IGNORE);
  if (it != _content.end()) {
    return it->second->asBoolean();
  }

  // Is the value a default value?
  if (isDefault())
    return true;

  return false;
}

std::string Object::uniqueIdentifier() const {
  // Compute the digest
  auto array = digest();

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

std::map<std::string, std::shared_ptr<Object>> const &Object::content() {
  return _content;
}

/** Get type */
void Object::type(std::shared_ptr<Type> const &type) {
  _type = type;
}

void Object::task(std::shared_ptr<Task> const &task) {
  _task = task;
}

std::shared_ptr<Task> Object::task() {
  return _task;
}

void Object::submit() {
  if (!_task) {
    throw exception("No underlying task for this object: cannot run");
  }
  return _task->submit(shared_from_this());
}

void Object::configure() {
  validate();
  seal();
}

void Object::findDependencies(std::vector<std::shared_ptr<rpc::Dependency>> &dependencies) {
  // Stop here
  if (canIgnore())
    return;

  if (hasKey(KEY_RESOURCE)) {
    std::string rid = get(KEY_RESOURCE)->asString();
    LOGGER->info("Found dependency {}", rid);
    dependencies.push_back(std::make_shared<rpc::ReadWriteDependency>(rid));
  } else {
    for (auto &entry: _content) {
      entry.second->findDependencies(dependencies);
    }
  }
}

bool Object::equals(Object const &other) {
  NOT_IMPLEMENTED();
}

void Object::validate() {
  LOGGER->debug("Validating");

  // (1) Validate the object arguments
  for (auto entry: _type->arguments()) {
    auto &argument = *entry.second;

    if (_content.count(argument.name()) == 0) {
      LOGGER->debug("No value provided...");
      // No value provided
      if (argument.required() && !argument.generator()) {
        throw argument_error(
            "Argument " + argument.name() + " was required but not given for " + this->type()->toString());
      } else {
        LOGGER->warn("Setting default value for {}...", argument.name());
        if (argument.defaultValue()) {
          auto value = argument.defaultValue()->copy();
          value->_default = true;
          set(argument.name(), value);
        } else {
          set(argument.name(), std::make_shared<Object>());
        }
      }
    } else {
      // Sets the value
      LOGGER->debug("Checking value of {}...", argument.name());
      auto value = get(argument.name());

      if (argument.defaultValue() && argument.defaultValue()->equals(*value)) {
        LOGGER->debug("Value is default");
        _default = true;
      } else {
        if (value->hasKey(KEY_TYPE) && !std::dynamic_pointer_cast<Value>(value)) {
          auto v = value->get(KEY_TYPE);
          auto valueType = value->type();
          if (valueType) {
            auto object = valueType->create();
            object->setValue(value);
            LOGGER->debug("Looking at {}", entry.first);
            object->validate();
          }
        }
      }
      LOGGER->debug("Setting...");
      setValue(argument.name(), value);
    }
  }

  // (2) Generate values
  LOGGER->debug("Generating values...");
  for (auto entry: _type->arguments()) {
    Argument &argument = *entry.second;
    Generator *generator = argument.generator();

    if (!hasKey(argument.name()) && generator) {
      LOGGER->debug("Generating value...");
      set(argument.name(), generator->generate(*this));
    }
  }

  // (3) Add resource
  if (_task) {
    LOGGER->info("Setting resource to {}", PathGenerator::SINGLETON.generate(*this)->asString());
    set(KEY_RESOURCE, PathGenerator::SINGLETON.generate(*this));
  }
}

void Object::execute() {
  throw exception("No execute method provided");
}

typedef std::string stdstring;

Value::~Value() {
  switch (_scalarType) {
    case ValueType::STRING:_value.string.~stdstring();
      break;
    default:
      // Do nothing for other values
      break;
  }
  _scalarType = ValueType::NONE;
}

Value::Union::~Union() {
  // Does nothing: handled by Scalar
}

Value::Union::Union() {
  // Does nothing: handled by Scalar
}



Value::Value() : _scalarType(ValueType::NONE) {
}

Value::Value(double value) : Object(), _scalarType(ValueType::REAL) {
  _value.real = value;
  _type = RealType;
}

Value::Value(long value) : _scalarType(ValueType::INTEGER) {
  _value.integer = value;
  _type = IntegerType;
}

Value::Value(int value) : _scalarType(ValueType::INTEGER) {
  _value.integer = value;
  _type = IntegerType;
}

Value::Value(bool value) : _scalarType(ValueType::BOOLEAN) {
  _value.boolean = value;
  _type = BooleanType;
}

Value::Value(std::string const &value) : _scalarType(ValueType::STRING) {
  // placement new
  new(&_value.string) std::string(value);
  _type = StringType;
}

Value::Value(Value const &other) : Object(other), _scalarType(other._scalarType) {
  switch (_scalarType) {
    case ValueType::NONE:break;

    case ValueType::REAL:_value.real = other._value.real;
      break;

    case ValueType::INTEGER:_value.integer = other._value.integer;
      break;

    case ValueType::BOOLEAN:_value.boolean = other._value.boolean;
      break;

    case ValueType::STRING:new(&_value.string) std::string(other._value.string);
      break;

  }
}

bool Value::equals(Object const &other) {
  if (Value const *otherValue = dynamic_cast<Value const*>(&other)) {
    return Helper::equals(*this, *otherValue);
  }
  return false;
}

long Value::asInteger() {
  switch (_scalarType) {
    case ValueType::NONE:return false;

    case ValueType::REAL: throw cast_error("cannot convert real to integer");
      break;

    case ValueType::INTEGER:return _value.integer;
      break;

    case ValueType::BOOLEAN: return _value.boolean ? 1 : 0;
      break;

    case ValueType::STRING: throw cast_error("cannot convert real to integer");
      break;
  }
}
double Value::asReal() {
  switch (_scalarType) {
    case ValueType::NONE:return false;

    case ValueType::REAL: return _value.real;
      break;

    case ValueType::INTEGER:return _value.integer;
      break;

    case ValueType::BOOLEAN: return _value.boolean ? 1 : 0;
      break;

    case ValueType::STRING: return !_value.string.empty();
      break;
  }}

bool Value::asBoolean() {
  switch (_scalarType) {
    case ValueType::NONE:return false;

    case ValueType::REAL: return _value.real;
      break;

    case ValueType::INTEGER:return _value.integer;
      break;

    case ValueType::BOOLEAN: return _value.boolean;
      break;

    case ValueType::STRING: return !_value.string.empty();
      break;
  }
}

std::string Value::asString() {
  switch (_scalarType) {
    case ValueType::NONE:return "";

    case ValueType::REAL: return std::to_string(_value.real);
      break;

    case ValueType::INTEGER:return std::to_string(_value.integer);
      break;

    case ValueType::BOOLEAN: return std::to_string(_value.boolean);
      break;

    case ValueType::STRING: return _value.string;
      break;
  }
}

json Value::jsonValue() const {
  switch (scalarType()) {
    case ValueType::STRING:return json(_value.string);

    case ValueType::INTEGER:return json(_value.integer);

    case ValueType::REAL:return json(_value.real);

    case ValueType::BOOLEAN:return json(_value.boolean);

    case ValueType::NONE:throw std::runtime_error("none has no type");
  }
}

json Value::toJson() {
  json j = Object::toJson();
  if (j.is_null())
    return jsonValue();

  j[KEY_VALUE] = jsonValue();
  return j;
}


std::array<unsigned char, DIGEST_LENGTH> Value::digest() const {
  Digest d;

  d.updateDigest(scalarType());

  // Hash value
  switch (_scalarType) {
    case ValueType::NONE:break;
    case ValueType::BOOLEAN:d.updateDigest(_value.boolean);
      break;
    case ValueType::INTEGER:d.updateDigest(_value.integer);
      break;
    case ValueType::REAL:d.updateDigest(_value.real);
      break;
    case ValueType::STRING:d.updateDigest(_value.string);
      break;
  }

  return d.get();
}

Value &Value::operator=(Value const &other) {
  this->~Value();

  _scalarType = other._scalarType;
  switch (_scalarType) {
    case ValueType::NONE:break;
    case ValueType::REAL:_value.real = other._value.real;
      break;
    case ValueType::INTEGER:_value.integer = other._value.integer;
      break;
    case ValueType::BOOLEAN:_value.boolean = other._value.boolean;
      break;
    case ValueType::STRING:new(&_value.string) std::string(other._value.string);
      break;
  }

  return *this;
}

bool Value::defined() const {
  return _scalarType != ValueType::NONE;
}

ValueType const Value::scalarType() const {
  return _scalarType;
}

bool Value::getBoolean() const {
  if (_scalarType != ValueType::BOOLEAN) throw std::runtime_error("Value is not a boolean");
  return _value.boolean;
}

double Value::getReal() const {
  if (_scalarType != ValueType::REAL) throw std::runtime_error("Value is not a boolean");
  return _value.real;
}

long Value::getInteger() const {
  if (_scalarType != ValueType::INTEGER) throw std::runtime_error("Value is not a boolean");
  return _value.integer;
}

std::string const &Value::getString() {
  if (_scalarType != ValueType::STRING) throw std::runtime_error("Value is not a string");
  return _value.string;
}

void Value::findDependencies(std::vector<std::shared_ptr<rpc::Dependency>> &dependencies) {}

std::shared_ptr<Object> Value::copy() {
  return std::make_shared<Value>(*this);
}

Array::~Array() {}

std::array<unsigned char, DIGEST_LENGTH> Array::digest() const {
  Digest d;
  for (auto x: _array) {
    auto xDigest = x->digest();
    d.updateDigest(xDigest);
  }
  return d.get();
}

void Array::add(std::shared_ptr<Object> const &element) {
  _array.push_back(element);
}

std::shared_ptr<Object> Array::copy() {
  return std::make_shared<Array>(*this);
}

// ---
// --- Task
// ---


Argument::Argument(std::string const &name) : _name(name), _required(true) {
}

Argument::Argument() : _name(), _required(true) {
}

std::string const &Argument::name() const {
  return _name;
}
Argument &Argument::name(std::string const &name) {
  _name = name;
  return *this;
}

bool Argument::required() const { return _required; }

Argument & Argument::required(bool required) {
  _required = required;
  return *this;
}
const std::string &Argument::help() const {
  return _help;
}
Argument & Argument::help(const std::string &help) {
  _help = help;
  return *this;
}

Argument & Argument::defaultValue(std::shared_ptr<Object> const &defaultValue) {
  _defaultValue = defaultValue;
  _required = false;
  return *this;
}
std::shared_ptr<Object> Argument::defaultValue() const { return _defaultValue; }

Generator *Argument::generator() { return _generator; }
Argument & Argument::generator(Generator *generator) { _generator = generator; return *this; }

std::shared_ptr<Type> const &Argument::type() const { return _type; }
Argument & Argument::type(std::shared_ptr<Type> const &type) { _type = type; return *this; }



// ---- Type


std::shared_ptr<Type> BooleanType = std::make_shared<Type>(BOOLEAN_TYPE, nullptr, true);
std::shared_ptr<Type> IntegerType = std::make_shared<Type>(INTEGER_TYPE, nullptr, true);
std::shared_ptr<Type> RealType = std::make_shared<Type>(REAL_TYPE, nullptr, true);
std::shared_ptr<Type> StringType = std::make_shared<Type>(STRING_TYPE, nullptr, true);
std::shared_ptr<Type> AnyType = std::make_shared<Type>(ANY_TYPE, nullptr, true);
std::shared_ptr<Type> PathType = std::make_shared<Type>(PATH_TYPE, nullptr, true);
std::shared_ptr<Type> ArrayType = std::make_shared<Type>(ARRAY_TYPE, nullptr, true);

/** Creates an object with a given type */
std::shared_ptr<Object> Type::create() {
  LOGGER->debug("Creating object from type {} with {}", _type, _factory ? "a factory" : "NO factory");
  const std::shared_ptr<Object> object = _factory ? _factory->create() : std::make_shared<Object>();
  object->type(shared_from_this());
  return object;
}

Type::Type(TypeName const &type, std::shared_ptr<Type> parent, bool predefined, bool canIgnore) :
    _type(type), _parent(parent), _predefined(predefined), _canIgnore(canIgnore) {}

Type::~Type() {}

void Type::objectFactory(std::shared_ptr<ObjectFactory> const &factory) {
  _factory = factory;
}

std::shared_ptr<ObjectFactory> const &Type::objectFactory() {
  return _factory;
}

void Type::addArgument(std::shared_ptr<Argument> const &argument) {
  _arguments[argument->name()] = argument;
}

std::map<std::string, std::shared_ptr<Argument>> &Type::arguments() {
  return _arguments;
}

std::map<std::string, std::shared_ptr<Argument>> const &Type::arguments() const {
  return _arguments;
}

TypeName const &Type::typeName() const { return _type; }

/// Return the type
std::string Type::toString() const { return "type(" + _type.toString() + ")"; }

/// Predefined types
bool Type::predefined() const { return _predefined; }

std::string Type::toJson() const {
  json response;

  json jsonArguments = json::object();
  for (auto const &entry: _arguments) {
    auto const &arg = *entry.second;
    json definition = {
        {"help", arg.help()},
        {"required", arg.required()},
        {"type", arg.type()->typeName().toString()},
    };

    if (arg.defaultValue())
      definition["default"] = arg.defaultValue()->toJson();

    jsonArguments[entry.first] = definition;
  }

  response["arguments"] = jsonArguments;
  response["type"] = typeName().toString();
  if (_parent) {
    response["parent"] = _parent->_type.toString();
  }
  return response.dump();
}
int Type::hash() const {
  return std::hash<Type>()(*this);
}

// ---- Generators


PathGenerator PathGenerator::SINGLETON = PathGenerator();
PathGenerator &pathGenerator = PathGenerator::SINGLETON;

std::shared_ptr<Object> PathGenerator::generate(Object &object) {
  auto uuid = object.uniqueIdentifier();

  // We have a task, so we use it
  std::shared_ptr<Task> task = object.task();
  if (task) {
    std::string localName = task->identifier().localName();
    auto p = Path(Context::current().workdir(), {task->identifier().toString(), uuid, localName});
    return std::make_shared<Value>(p.toString());
  }

  auto p = Path(Context::current().workdir(), {uuid});
  return std::make_shared<Value>(p.toString());
}

// ---- Task

Task::Task(std::shared_ptr<Type> const &type) : _identifier(type->typeName()), _type(type) {
}

TypeName Task::typeName() const { return _type->typeName(); }

void Task::submit(std::shared_ptr<Object> const &object) const {
  // Find dependencies
  std::vector<std::shared_ptr<rpc::Dependency>> dependencies;
  object->findDependencies(dependencies);

  // Validate and seal the task object
  object->validate();
  object->seal();

  // Get generated directory as locator
  auto locator = rpc::Path::toPath(PathGenerator::SINGLETON.generate(*object)->asString());

  // Prepare the command line
  CommandContext context;
  context.parameters = object->toJsonString();
  auto command = _commandLine.rpc(context);

  // Add dependencies
  LOGGER->info("Adding {} dependencies", dependencies.size());
  for (auto dependency: dependencies) {
    LOGGER->info("Adding dependency {}", dependency->identifier());
    command->add_dependency(dependency);
  }

  auto task = std::make_shared<rpc::CommandLineTask>(locator);
  task->taskId(object->task()->identifier().toString());
  task->command(command);
  task->submit();
}

void Task::commandline(CommandLine command) {
  _commandLine = command;
}

TypeName const &Task::identifier() const {
  return _identifier;
}

void Task::objectFactory(std::shared_ptr<ObjectFactory> const &factory) {
  _factory = factory;
}

std::shared_ptr<Object> Task::create() {
  if (!_factory) {
    throw argument_error("Task has no factory");
  }
  auto object = _factory->create();
  object->type(_type);
  object->task(this->shared_from_this());
  LOGGER->debug("Setting task to {}", _identifier);
  return object;
}

void Task::execute(std::shared_ptr<Object> const &object) const {
  object->configure();
  object->execute();
}

// ---- REGISTER

std::shared_ptr<Object> ObjectFactory::create() {
  auto object = _create();
  object->_register = _register;
  return object;
}

ObjectFactory::ObjectFactory(std::shared_ptr<Register> const &theRegister) : _register(theRegister) {

}

Register::Register() {
  addType(IntegerType);
  addType(RealType);
  addType(StringType);
  addType(BooleanType);
  addType(PathType);
}
Register::~Register() {}

void Register::addType(std::shared_ptr<Type> const &type) {
  _types[type->typeName()] = type;
}

void Register::addTask(std::shared_ptr<Task> const &task) {
  _tasks[task->identifier()] = task;
}

std::shared_ptr<Task> Register::getTask(TypeName const &typeName) {
  auto it = _tasks.find(typeName);
  if (it != _tasks.end()) {
    return it->second;
  }
  return nullptr;
}

std::shared_ptr<Type> Register::getType(TypeName const &typeName) {
  auto it = _types.find(typeName);
  if (it != _types.end()) {
    return it->second;
  }
  return nullptr;
}

// Find a type given a type name
std::shared_ptr<Type> Register::getType(std::shared_ptr<Object> const &object) {
  return object->type();
}

std::shared_ptr<Object> Register::build(std::shared_ptr<Object> const &value) {
  std::shared_ptr<Type> objectType = value->type();
  LOGGER->debug("Building object with type {}", *objectType);

  // Create the object
  std::cerr << "Creating object..." << std::endl;
  auto object = objectType ? objectType->create() : std::make_shared<Object>();

  std::cerr << "Created object... " << object.get() << std::endl;
  object->setValue(value);

  if (!object) {
    throw std::runtime_error("Object of type " + objectType->toString() + " was not created");
  }

  // Loop over all the type hierarchy
  for (auto type = objectType; type; type = type->_parent) {

    // Loop over the arguments
    for (auto entry: type->arguments()) {
      auto key = entry.first;

      // Check required argument
      auto const hasKey = value->hasKey(key);
      if (!hasKey && entry.second->required()) {
        throw argument_error("Argument " + key + " was required but not provided");
      }

      // Build subtype
      if (hasKey) {
        std::cerr << "Building " << key << std::endl;
        auto subvalue = build(value->get(key));

        // Set argument
        std::cerr << "Setting " << key << std::endl;
        object->set(key, subvalue);
      } else {
        auto defaultValue = entry.second->defaultValue();
        if (defaultValue) {
          object->set(key, defaultValue->copy());
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
      if (!type.second->predefined()) {
        if (!first) std::cout << ","; else first = false;
        std::cout << type.second->toJson() << std::endl;
      }
    }
    std::cout << "]" << std::endl;

    std::cout << R"("tasks": [)" << std::endl;
    first = true;
    for (auto const &type: _types) {
      if (!type.second->predefined()) {
        if (!first) std::cout << ","; else first = false;
        std::cout << type.second->toJson() << std::endl;
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
    auto value = Object::createFromJson(*this, j);

    // Run the task
    task->execute(value);

    return;

  }

  throw argument_error("Unexpected command: " + args[0]);
}

std::shared_ptr<Object> Register::build(std::string const &value) {
  return Object::createFromJson(*this, json::parse(value));
}

void Register::parse(int argc, const char **argv) {
  std::vector<std::string> args;
  for(int i = 1; i < argc; ++i) {
    args.emplace_back(std::string(argv[argc]));
  }
  parse(args);
}

} // xpm namespace
