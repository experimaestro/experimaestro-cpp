
#include <sstream>
#include <iostream>
#include <unordered_set>
#include <fstream>


#include <xpm/common.hpp>
#include <xpm/xpm.hpp>
#include <xpm/register.hpp>
#include <xpm/value.hpp>
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
const std::string KEY_TYPE = "$type";
const std::string KEY_TASK = "$task";

const std::string KEY_PATH = "$path";
const std::string KEY_VALUE = "$value";
const std::string KEY_IGNORE = "$ignore";
const std::string KEY_DEFAULT = "$default";
const std::string KEY_RESOURCE = "$resource";

static const auto RESTRICTED_KEYS = std::unordered_set<std::string> {KEY_TYPE, KEY_TASK, KEY_VALUE, KEY_DEFAULT};

const TypeName STRING_TYPE("string");
const TypeName BOOLEAN_TYPE("boolean");
const TypeName INTEGER_TYPE("integer");
const TypeName REAL_TYPE("real");
const TypeName ARRAY_TYPE("array");
const TypeName ANY_TYPE("any");

const TypeName PATH_TYPE("path");
const TypeName RESOURCE_TYPE("resource");

static const std::unordered_set<TypeName> IGNORED_TYPES = {PATH_TYPE, RESOURCE_TYPE};

sealed_error::sealed_error() : exception("Object is sealed: cannot modify") {}
argument_error::argument_error(const std::string &message) : exception(message) {}
cast_error::cast_error(const std::string &message) : exception(message) {}
not_implemented_error::not_implemented_error(const std::string &message,
                                             const std::string &file, int line) : exception(
    "Not implemented: " + message + ", file " + file + ":" + std::to_string(line)) {}



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

Object::Object() : _flags(0), _type(AnyType) {
}

Object::Object(std::map<std::string, std::shared_ptr<Object>> &map)
    : _flags(0), _content(map) {
}

Object::Object(Object const &other) : _flags(other._flags), _content(other._content) {
}

void Object::fill(Register &xpmRegister, nlohmann::json const &jsonValue) {
  for (json::const_iterator it = jsonValue.begin(); it != jsonValue.end(); ++it) {
    if (it.key() == KEY_VALUE) {
      // already handled
    } else if (it.key() == KEY_TYPE) {
      _type = xpmRegister.getType(TypeName((std::string const &) it.value()));
    } else if (it.key() == KEY_DEFAULT) {
      if (!it.value().is_boolean())
         throw std::runtime_error("Default flag is not boolean in object");
      set(Flag::DEFAULT, it.value());
    } else if (it.key() == KEY_TASK) {
      _task = xpmRegister.getTask(it.value(), true);
    } else {
      set(it.key(), createFromJson(xpmRegister, it.value()));
    }
  }
}

std::shared_ptr<Object> Object::createFromJson(Register &xpmRegister, nlohmann::json const &jsonValue) {
  switch (jsonValue.type()) {

    // --- Object
    case nlohmann::json::value_t::object: {
      std::shared_ptr<Object> object;

      auto _key = jsonValue.find(KEY_VALUE);
      std::shared_ptr<Type> type;
      if (jsonValue.count(KEY_TYPE) > 0) {
        auto typeName = TypeName((std::string const &) jsonValue[KEY_TYPE]);
        type = xpmRegister.getType(typeName);
        if (!type) {
          LOGGER->debug("Could not find type {} in registry", typeName);
        }
      }

      if (_key != jsonValue.end()) {
        // Infer type from value
        object = createFromJson(xpmRegister, *_key);
        if (type) object = dynamic_cast<Value&>(*object).cast(type);
      } else {
        if (type) {
          LOGGER->debug("Creating object of type {} using type->create()", *type);
          object = type->create(xpmRegister.objectFactory());
        } else {
          LOGGER->debug("Creating object of unknown type default object factory");
          object = xpmRegister.objectFactory()->create();
        }
      }

      object->fill(xpmRegister, jsonValue);

      return object;
    }


    // --- Array

    case nlohmann::json::value_t::array: {
      auto array = std::make_shared<Array>();
      for (json::const_iterator it = jsonValue.begin(); it != jsonValue.end(); ++it) {
        array->add(createFromJson(xpmRegister, *it));
      }
      return array;
    }

    // --- Simple type

    case nlohmann::json::value_t::null:
    case nlohmann::json::value_t::discarded:return std::make_shared<Value>();

    case nlohmann::json::value_t::string:return std::make_shared<Value>((std::string const &) jsonValue);

    case nlohmann::json::value_t::boolean:return std::make_shared<Value>((bool) jsonValue);

    case nlohmann::json::value_t::number_integer:
    case nlohmann::json::value_t::number_unsigned:return std::make_shared<Value>((long) jsonValue);

    case nlohmann::json::value_t::number_float:{
      // Try first as integer
      if (std::trunc((double)jsonValue) == (double)jsonValue) {
        return std::make_shared<Value>((long) jsonValue);
      }

      return std::make_shared<Value>((double) jsonValue);
    }
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
Path Object::asPath() const {
  throw cast_error("Cannot convert to path");
}

// Convert to JSON
json Object::toJson() {
  // No content
  if (_content.empty() && !_task && (!_type || dynamic_cast<Value *>(this)) && !get(Flag::DEFAULT)) {
    return nullptr;
  }

  // We have some values
  json o = json::object();
  for (auto const &entry: _content) {
    o[entry.first] = entry.second->toJson();
  }

  if (get(Flag::DEFAULT))
    o[KEY_DEFAULT] = true;
  if (_type) {
    o[KEY_TYPE] = _type->typeName().toString();
  }

  if (_task) {
    o[KEY_TASK] = _task->identifier().toString();
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

  d.updateDigest("task");
  if (_task) {
    d.updateDigest(_task->identifier().toString());
  } else {
    d.updateDigest(0);
  }

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
  if (get(Flag::SEALED)) {
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
  if (get(Flag::SEALED)) return;

  for (auto &item: _content) {
    item.second->seal();
  }

  set(Flag::SEALED, true);
}

bool Object::isSealed() const {
  return get(Flag::SEALED);
}

bool Object::isDefault() const {
  return get(Flag::DEFAULT);
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

void Object::submit(bool send,
                    std::shared_ptr<rpc::Launcher> const &launcher,
                    std::shared_ptr<rpc::LauncherParameters> const &launcherParameters) {
  if (!_task) {
    throw exception("No underlying task for this object: cannot run");
  }
  return _task->submit(shared_from_this(), send, launcher, launcherParameters);
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

bool Object::equals(Object const &other) const {
  NOT_IMPLEMENTED();
}

void Object::validate(bool generate) {
  LOGGER->debug("Validating (generate={})", generate);

  if (!get(Flag::SEALED)) set(Flag::VALIDATED, false);

  // (1) Validate the object arguments
  if (!get(Flag::VALIDATED)) {
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
              LOGGER->debug("Setting default value for {}...", argument.name());
              auto value = argument.defaultValue()->copy();
              value->set(Flag::DEFAULT, true);
              set(argument.name(), value);
            }
          }
        } else {
          // Sets the value
          auto value = get(argument.name());
          LOGGER->debug("Checking value of {} [type {} vs {}]...", argument.name(), *argument.type(), *value->type());

          // If the value is default, add a flag
          if (argument.defaultValue() && argument.defaultValue()->equals(*value)) {
            LOGGER->debug("Value is default");
            value->set(Flag::DEFAULT, true);
          } else {
            // If the value has a type, handles this
            if (value->hasKey(KEY_TYPE) && !std::dynamic_pointer_cast<Value>(value)) {
              // Create an object of the key type
              auto v = value->get(KEY_TYPE);
              auto valueType = value->type();
              if (valueType) {
                auto object = valueType->create(nullptr);
                object->setValue(value);
                LOGGER->debug("Looking at {}", entry.first);
                object->validate(generate);
              }
            }
          }

          LOGGER->debug("Validating {}...", argument.name());
          value->validate(generate);
          LOGGER->debug("Setting {}...", argument.name());
          setValue(argument.name(), value);
        }
      }
    }
    set(Flag::VALIDATED, true);
  } else {
    LOGGER->debug("Object already validated");
  }


  // (2) Generate values
  if (generate) {
    if (get(Flag::GENERATED)) {
      LOGGER->debug("Object already generated");
    } else {
      // (3) Add resource
      if (_task) {
        auto identifier = _task->getPathGenerator()->generate(*this)->asString();
        LOGGER->info("Setting resource to {}", identifier);
        set(KEY_RESOURCE, identifier);
      }

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
      set(Flag::GENERATED, true);
    }
  }
}

void Object::execute() {
  throw exception("No execute method provided in " + std::string(typeid(*this).name()));
}

Path Object::outputPath() const {
  auto resource = static_cast<Object>(*this).get(KEY_RESOURCE);
  if (!resource) {
    throw std::invalid_argument("No resource associated to this object");
  }
  return Path(resource->asString() + ".out");
}

void Object::set(Object::Flag flag, bool value) {
  if (value) _flags |= (Flags)flag;
  else _flags &= ~((Flags)flag);

  assert(get(flag) == value);
}

bool Object::get(Object::Flag flag) const {
  return (Flags)flag & _flags;
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

std::shared_ptr<Type> BooleanType = std::make_shared<SimpleType>(BOOLEAN_TYPE, ValueType::BOOLEAN);
std::shared_ptr<Type> IntegerType = std::make_shared<SimpleType>(INTEGER_TYPE, ValueType::INTEGER);
std::shared_ptr<Type> RealType = std::make_shared<SimpleType>(REAL_TYPE, ValueType::REAL);
std::shared_ptr<Type> StringType = std::make_shared<SimpleType>(STRING_TYPE, ValueType::STRING);
std::shared_ptr<Type> PathType = std::make_shared<SimpleType>(PATH_TYPE, ValueType::PATH, true);
std::shared_ptr<Type> ArrayType = std::make_shared<Type>(ARRAY_TYPE, nullptr, true);

std::shared_ptr<Type> AnyType = std::make_shared<Type>(ANY_TYPE, nullptr, true);

/** Creates an object with a given type */
std::shared_ptr<Object> Type::create(std::shared_ptr<ObjectFactory> const &defaultFactory) {
  LOGGER->debug("Creating object from type {} with {}", _type, _factory ? "a factory" : "default factory");
  const std::shared_ptr<Object> object = _factory ? _factory->create() : defaultFactory->create();
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

std::unordered_map<std::string, std::shared_ptr<Argument>> &Type::arguments() {
  return _arguments;
}

std::unordered_map<std::string, std::shared_ptr<Argument>> const &Type::arguments() const {
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
  json response = json::object();

  json jsonArguments = json::object();
  for (auto const &entry: _arguments) {
    auto const &arg = *entry.second;
    json definition = json::object();

    if (!arg.help().empty()) {
      definition["help"] = arg.help();
    }

    // Only output not required when needed
    if (!arg.required() && !arg.defaultValue()) {
      definition["required"] = false;
    }

    if (arg.generator()) {
      definition["generator"] = std::const_pointer_cast<Generator>(arg.generator())->toJson();
    }

    if (arg.defaultValue()) {
      definition["default"] = arg.defaultValue()->toJson();
    }

    if (definition.empty()) {
      definition = arg.type()->typeName().toString();
    } else {
      definition["type"] = arg.type()->typeName().toString();
    }
    jsonArguments[entry.first] = definition;
  }

  if (!jsonArguments.empty()) {
    response["arguments"] = std::move(jsonArguments);
  }

  if (!_properties.empty()) {
    json jsonProperties = json::object();
    for (auto const &entry: _properties) {
      jsonProperties[entry.first] = entry.second->toJson();
    }
    response["properties"] = std::move(jsonProperties);
  }

  if (!_description.empty()) {
    response["description"] = _description;
  }

  if (_parent) {
    response["parent"] = _parent->_type.toString();
  }
  return response.dump();
}
int Type::hash() const {
  return std::hash<Type>()(*this);
}


void Type::setProperty(std::string const &name, Object::Ptr const &value) {
  _properties[name] = value;
}

Object::Ptr Type::getProperty(std::string const &name) {
  auto it = _properties.find(name);
  if (it == _properties.end()) return nullptr;
  return it->second;
}

// ---- Generators

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
  Path p = Context::current().workdir();
  auto uuid = object.uniqueIdentifier();

  if (std::shared_ptr<Task> task = object.task()) {
    p = Path(p, {task->identifier().toString()});
  }

  p = Path(p, {uuid});

  if (!_name.empty()) {
    p = Path(p, { _name });
  }
  return std::make_shared<Value>(p);
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
