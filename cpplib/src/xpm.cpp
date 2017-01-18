
#include <sstream>
#include <iostream>
#include <unordered_set>
#include <fstream>

#include <openssl/sha.h>

#include <xpm/common.hpp>
#include <xpm/xpm.hpp>
#include <xpm/register.hpp>
#include <xpm/context.hpp>
#include <xpm/rpc/client.hpp>
#include "private.hpp"

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

static const auto RESTRICTED_KEYS = std::unordered_set<std::string> {KEY_TYPE, KEY_TASK, KEY_VALUE, KEY_DEFAULT};

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
    if (!SHA1_Final(md.data(), &context)) {
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

void Object::fill(Register &xpmRegister, nlohmann::json const &jsonValue) {
  for (json::const_iterator it = jsonValue.begin(); it != jsonValue.end(); ++it) {
    if (it.key() == KEY_VALUE) {
      // already handled
    } else if (it.key() == KEY_TYPE) {
      _type = xpmRegister.getType(TypeName((std::string const &) it.value()));
    } else if (it.key() == KEY_DEFAULT) {
      _default = it.value();
    } else {
      set(it.key(), createFromJson(xpmRegister, it.value()));
    }
  }
}

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

      object->fill(xpmRegister, jsonValue);

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
  if (_content.empty() && !_task && (!_type || dynamic_cast<Value *>(this)) && !_default) {
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
    sprintf(b.data(), "%02x", array[i]);
    s += b.data();
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

void Object::configure(bool generate) {
  validate(generate);
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

void Object::validate(bool generate) {
  LOGGER->debug("Validating (generate={})", generate);

  // (1) Validate the object arguments
  for (auto type = _type; type; type = type->parentType()) {
    LOGGER->debug("Looking at type {} [{} arguments]", type->typeName(), type->arguments().size());
    for (auto entry: type->arguments()) {
      auto &argument = *entry.second;
      LOGGER->debug("Looking at argument {}", argument.name());

      if (_content.count(argument.name()) == 0) {
        LOGGER->debug("No value provided...");
        // No value provided
        if (argument.required() && (!generate || !argument.generator())) {
          throw argument_error(
              "Argument " + argument.name() + " was required but not given for " + this->type()->toString());
        } else {
          if (argument.defaultValue()) {
            LOGGER->warn("Setting default value for {}...", argument.name());
            auto value = argument.defaultValue()->copy();
            value->_default = true;
            set(argument.name(), value);
          }
        }
      } else {
        // Sets the value
        LOGGER->debug("Checking value of {}...", argument.name());
        auto value = get(argument.name());

        if (argument.defaultValue() && argument.defaultValue()->equals(*value)) {
          LOGGER->debug("Value is default");
          value->_default = true;
        } else {
          if (value->hasKey(KEY_TYPE) && !std::dynamic_pointer_cast<Value>(value)) {
            auto v = value->get(KEY_TYPE);
            auto valueType = value->type();
            if (valueType) {
              auto object = valueType->create();
              object->setValue(value);
              LOGGER->debug("Looking at {}", entry.first);
              object->validate(generate);
            }
          }
        }
        LOGGER->debug("Setting...");
        setValue(argument.name(), value);
      }
    }
  }

  // (2) Generate values
  if (generate) {
    LOGGER->debug("Generating values...");
    for (auto type = _type; type; type = type->parentType()) {
      for (auto entry: type->arguments()) {
        Argument &argument = *entry.second;
        auto generator = argument.generator();

        if (!hasKey(argument.name()) && generator) {
          auto generated = generator->generate(*this);
          LOGGER->debug("Generating value for {}", argument.name());
          set(argument.name(), generated);
        }
      }
    }

    // (3) Add resource
    if (_task) {
      auto object = PathGenerator().generate(*this);
      LOGGER->info("Setting resource to {}", object->asString());
      set(KEY_RESOURCE, object);
    }
  }
}

void Object::execute() {
  throw exception("No execute method provided in " + std::string(typeid(*this).name()));
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
  if (Value const *otherValue = dynamic_cast<Value const *>(&other)) {
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
  }
}

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


Argument::Argument(std::string const &name) : _name(name), _required(true), _generator(nullptr) {
}

Argument::Argument() : Argument("") {
}

std::string const &Argument::name() const {
  return _name;
}
Argument &Argument::name(std::string const &name) {
  _name = name;
  return *this;
}

bool Argument::required() const { return _required; }

Argument &Argument::required(bool required) {
  _required = required;
  return *this;
}
const std::string &Argument::help() const {
  return _help;
}
Argument &Argument::help(const std::string &help) {
  _help = help;
  return *this;
}

Argument &Argument::defaultValue(std::shared_ptr<Object> const &defaultValue) {
  _defaultValue = defaultValue;
  _required = false;
  return *this;
}
std::shared_ptr<Object> Argument::defaultValue() const { return _defaultValue; }

std::shared_ptr<Generator> Argument::generator() { return _generator; }
std::shared_ptr<Generator> const &Argument::generator() const { return _generator; }
Argument &Argument::generator(std::shared_ptr<Generator> const &generator) {
  _generator = generator;
  return *this;
}

std::shared_ptr<Type> const &Argument::type() const { return _type; }
Argument &Argument::type(std::shared_ptr<Type> const &type) {
  _type = type;
  return *this;
}



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

void Type::parentType(Ptr const &type) {
  _parent = type;
}

Type::Ptr Type::parentType() {
  return _parent;
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

    if (arg.generator())
      definition["generator"] = std::const_pointer_cast<Generator>(arg.generator())->toJson();

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

const std::shared_ptr<PathGenerator> SIMPLEPATHGENERATOR = std::make_shared<PathGenerator>(std::string());
const std::string PathGenerator::TYPE = "path";

std::shared_ptr<Generator> Generator::createFromJSON(nlohmann::json const &j) {
  std::string type = j["type"];
  if (type == PathGenerator::TYPE) {
    return std::make_shared<PathGenerator>(j);
  }

  throw std::invalid_argument("Generator type " + type + " not recognized");
}

PathGenerator::PathGenerator(nlohmann::json const &j) : _name((std::string const &)j["name"]) {
}

nlohmann::json PathGenerator::toJson() const {
  return {
      { "type", PathGenerator::TYPE },
      { "name", _name }
  };
}

std::shared_ptr<Object> PathGenerator::generate(Object &object) {
  auto uuid = object.uniqueIdentifier();

  // We have a task, so we use it
  std::shared_ptr<Task> task = object.task();
  Path p;
  if (task) {
    std::string localName = task->identifier().localName();
    p = Path(Context::current().workdir(), {task->identifier().toString(), uuid, localName});
  } else {
    p = Path(Context::current().workdir(), {uuid});
  }

  if (!_name.empty()) {
    p = Path(p, { _name });
  }
  return std::make_shared<Value>(p.toString());
}

PathGenerator::PathGenerator(std::string const &name) : _name(name) {

}

// ---- REGISTER

std::shared_ptr<Object> ObjectFactory::create() {
  auto object = _create();
  object->_register = _register;
  return object;
}

ObjectFactory::ObjectFactory(std::shared_ptr<Register> const &theRegister) : _register(theRegister) {

}
} // xpm namespace
