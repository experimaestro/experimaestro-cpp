#include <sstream>
#include <iostream>
#include <unordered_set>
#include <fstream>
#include <typeinfo>

// Demangle
#include <cxxabi.h>

#include <xpm/common.hpp>
#include <xpm/xpm.hpp>
#include <xpm/register.hpp>
#include <xpm/value.hpp>
#include <xpm/task.hpp>
#include <xpm/workspace.hpp>
#include <__xpm/common.hpp>

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

/// Type of the object
const std::string KEY_TYPE = "$type";

/// Task that generated it
const std::string KEY_TASK = "$task";

/// Job information
const std::string KEY_JOB = "$job";

/// Value
const std::string KEY_VALUE = "$value";

static const auto RESTRICTED_KEYS = std::unordered_set<std::string> {KEY_TYPE, KEY_TASK, KEY_VALUE, KEY_JOB};

const TypeName STRING_TYPE("string");
const TypeName BOOLEAN_TYPE("boolean");
const TypeName INTEGER_TYPE("integer");
const TypeName REAL_TYPE("real");
const TypeName ARRAY_TYPE("array");
const TypeName ANY_TYPE("any");
const TypeName PATH_TYPE("path");
const ptr<Object> NULL_OBJECT;

static const std::unordered_set<TypeName> IGNORED_TYPES = {PATH_TYPE};

sealed_error::sealed_error() : exception("Object is sealed: cannot modify") {}
argument_error::argument_error(const std::string &message) : exception(message) {}
cast_error::cast_error(const std::string &message) : exception(message) {}
assertion_error::assertion_error(const std::string &message) : exception(message) {}
io_error::io_error(const std::string &message) : exception(message) {}
not_implemented_error::not_implemented_error(const std::string &message,
                                             const std::string &file, int line) : exception(
    "Not implemented: " + message + ", file " + file + ":" + std::to_string(line)) {}


template<typename T>
std::string demangle(T const & t) {
  int status;
  char * demangled = abi::__cxa_demangle(typeid(t).name(),0,0,&status);
  std::string r = demangled;
  free(demangled);
  return r;
}

void Outputable::output(std::ostream &out) const {
  out << "Object of type " << demangle(this) << std::endl;
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

Object::~Object() {}
void Object::setValue(std::string const &name, std::shared_ptr<StructuredValue> const & value) {}

void Object::run() {
  throw assertion_error("Object is not a task: cannot run it!");
}

StructuredValue::~StructuredValue() {
}
StructuredValue::StructuredValue(Value const &v) : _flags(0) {
  _value = v;
  _type = v.type();
} 

StructuredValue::StructuredValue() : _flags(0), _type(AnyType) {
}

StructuredValue::StructuredValue(std::map<std::string, ptr<StructuredValue>> &map)
    : _flags(0), _content(map) {
}

class DummyJob : public Job {
public:
  DummyJob(nlohmann::json const & j) : Job(Path((std::string const &)(j["locator"])), nullptr) {
  }
  virtual void run() override { throw cast_error("This is dummy job - it cannot be run!"); }
};

StructuredValue::StructuredValue(Register &xpmRegister, nlohmann::json const &jsonValue) 
  : _flags(0) {
  switch (jsonValue.type()) {

    // --- Object
    case nlohmann::json::value_t::object: {
      // (1) Get the type of the object
      ptr<Type> type;
      if (jsonValue.count(KEY_TYPE) > 0) {
        auto typeName = TypeName((std::string const &) jsonValue[KEY_TYPE]);
        _type = xpmRegister.getType(typeName);
        if (!_type) {
          _type = std::make_shared<Type>(typeName);
          _type->placeholder(true);
          xpmRegister.addType(_type);
          LOGGER->warn("Could not find type '{}' in registry: using undefined type", typeName);
        }
      }
      
      // (2) Fill from JSON
      for (json::const_iterator it = jsonValue.begin(); it != jsonValue.end(); ++it) {
        if (it.key() == KEY_VALUE) {
            // Infer type from value
            _value = Value(xpmRegister, it.value());
            if (_type) _value = _value.cast(_type);
        } else if (it.key() == KEY_TYPE) {
          // ignore
        } else if (it.key() == KEY_JOB) {
          job(mkptr<DummyJob>(it.value()));
        } else if (it.key() == KEY_TASK) {
          _task = xpmRegister.getTask(it.value(), true);
        } else {
          set(it.key(), std::make_shared<StructuredValue>(xpmRegister, it.value()));
        }
      }

      LOGGER->debug("Got an object of type {}", _type ? _type->toString() : "?");
      break;
    }

    default: {
      _value = Value(xpmRegister, jsonValue);
    }
  }
}

// Convert to JSON
json StructuredValue::toJson() {
  // No content
  if (_content.empty() && !_task && !type() && !get(Flag::DEFAULT)) {
    return nullptr;
  }

  // We have some values
  json o = json::object();
  for (auto const &entry: _content) {
    o[entry.first] = entry.second->toJson();
  }

  if (_type) {
    o[KEY_TYPE] = _type->typeName().toString();
  }

  if (_task) {
    o[KEY_TASK] = _task->identifier().toString();
  }

  if (_value.defined()) {
    o[KEY_VALUE] = _value.toJson();
  }

  return o;
}

ptr<StructuredValue> StructuredValue::copy() {
  auto sv = mkptr<StructuredValue>();
  sv->_job =_job;
  sv->_object =_object;
  sv->_task =_task;
  sv->_value =_value;
  sv->_flags =_flags;
  sv->_content =_content;

  return sv;
}

std::string StructuredValue::toJsonString() {
  return toJson().dump();
}

void Digest::updateDigest(StructuredValue const & sv) {
  updateDigest(sv.type()->typeName().toString());

  updateDigest("task");
  if (sv._task) {
    updateDigest(sv._task->identifier().toString());
  } else {
    updateDigest(0);
  }

  if (sv.value().defined()) {
    // If there is a value, ignore the rest
    // of the structure
    updateDigest(0);
    sv.value().updateDigest(*this);
  } else {
    updateDigest(1);
    for (auto &item: sv._content) {
      auto const &key = item.first;

      if (item.second->canIgnore()) {
        // Remove keys that can be ignored (e.g. paths)
        continue;
      }

      // Update digest with key
      updateDigest(key);

      // Update digest with *value digest* (this allows digest caching)
      updateDigest(item.second->digest());
    }
  }

}

/// Internal digest function
std::array<unsigned char, DIGEST_LENGTH> StructuredValue::digest() const {
  Digest d;
  d.updateDigest(*this);
  return d.get();
};




ptr<Type> StructuredValue::type() const {
  if (_type) return _type;
  if (_value.defined()) return _value.type();
  return AnyType;
}

bool StructuredValue::hasKey(std::string const &key) const {
  return _content.find(key) != _content.end();
}

ptr<StructuredValue> StructuredValue::set(const std::string &key, ptr<StructuredValue> const &value) {
  if (get(Flag::SEALED)) {
    throw sealed_error();
  }

  if (RESTRICTED_KEYS.count(key) > 0)
    throw argument_error("Cannot access directly to " + key);

  auto it = _content.find(key);
  _content[key] = value;

  // Set default / ignore
  auto itA = type()->arguments().find(key);
  if (itA != type()->arguments().end()) {
    auto & argument = *itA->second;
    if (argument.defaultValue() && argument.defaultValue()->equals(*value)) {
      LOGGER->debug("Value is default");
      value->set(Flag::DEFAULT, true);
    }
    if (argument.ignore()) {
      value->set(Flag::IGNORE, true);
    }
  }

  // And for the object
  setValue(key, value);

  return it == _content.end() ? nullptr : it->second;
}

ptr<StructuredValue> StructuredValue::get(const std::string &key) {
  auto value = _content.find(key);
  if (value == _content.end()) throw std::out_of_range(key + " is not defined for object");
  return value->second;
}

void StructuredValue::seal() {
  if (get(Flag::SEALED)) return;

  for (auto &item: _content) {
    item.second->seal();
  }

  set(Flag::SEALED, true);
}

bool StructuredValue::isSealed() const {
  return get(Flag::SEALED);
}

bool StructuredValue::isDefault() const {
  return get(Flag::DEFAULT);
}

bool StructuredValue::ignore() const {
  return get(Flag::IGNORE);
}

bool StructuredValue::canIgnore() {
  // If the ignore flag is set
  if (ignore()) {
    return true;
  }

  // If the type is ignorable
  if (type()->canIgnore()) {
    return true;
  }

  // Is the value a default value?
  if (isDefault())
    return true;

  return false;
}

std::string StructuredValue::uniqueIdentifier() const {
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

std::map<std::string, ptr<StructuredValue>> const &StructuredValue::content() {
  return _content;
}

/** Get type */
void StructuredValue::type(ptr<Type> const &type) {
  _type = type;
}

void StructuredValue::object(ptr<Object> const &object) {
  _object = object;
}

ptr<Object> StructuredValue::object() {
  return _object;
}

void StructuredValue::task(ptr<Task> const &task) {
  _task = task;
}

ptr<Task> StructuredValue::task() {
  return _task;
}

void StructuredValue::configure(Workspace & ws) {
  GeneratorContext context(ws);
  generate(context);
  validate();
  seal();
}

ptr<Object> StructuredValue::createObjects(xpm::Register &xpmRegister) {
  // Don't create an object for values
  if (_value.defined()) return nullptr;

  // Create for descendants
  for(auto &kv: _content) {
    kv.second->createObjects(xpmRegister);
  }

  // Create for ourselves
  _object = xpmRegister.createObject(shared_from_this());
  if (_object) {
     for(auto &kv: _content) {
       setValue(kv.first, kv.second);
     }
  }

  return _object;
}

void StructuredValue::addDependencies(Job & job,  bool skipThis) {
  // Stop here
  if (canIgnore())
    return;

  if (_job) {
    LOGGER->info("Found dependency resource {}", _job);
    job.addDependency(_job->createDependency());
  } else {
    for (auto &entry: _content) {
      entry.second->addDependencies(job, false);
    }
  }
}

bool StructuredValue::equals(StructuredValue const &other) const {
  if (_value.defined()) {
    return _value.equals(other._value);
  }

  if (other._value.defined()) return false;

  // TODO: implement for deeper structures
  NOT_IMPLEMENTED();
}

void StructuredValue::generate(GeneratorContext & context) {
  if (auto A = context.enter(this)) {
    // Already generated
    if (get(Flag::GENERATED)) {
      LOGGER->debug("Object already generated");
      return;
    }

    // Check if we can modify this object
    if (isSealed()) {
      throw exception("Cannot generate values within a sealed object");
    }

    // (2) Generate values
    LOGGER->debug("Generating values...");
    for (auto type = _type; type; type = type->parentType()) {
      for (auto entry: type->arguments()) {
        Argument &argument = *entry.second;
        auto generator = argument.generator();

        if (!hasKey(argument.name())) {
          if (generator) {
          auto generated = generator->generate(context);
          LOGGER->debug("Generating value for {}", argument.name());
          set(argument.name(), generated);
          } else if (argument.defaultValue()) {
            LOGGER->debug("Setting default value for {}...", argument.name());
            auto value = argument.defaultValue()->copy();
            value->set(Flag::DEFAULT, true);
            value->set(Flag::IGNORE, argument.ignore());
            set(argument.name(), value);
          } else if (!argument.required()) {
            // Set value null
            auto value = std::make_shared<StructuredValue>(Value::NONE);
            value->set(Flag::DEFAULT, true);
            setValue(argument.name(), nullptr);
          }
        } else {
          // Generate sub-structures
          _content[argument.name()]->generate(context);
      }
    }
    
      set(Flag::GENERATED, true);
    }
  }
}

void StructuredValue::validate() {
  if (get(Flag::VALIDATED)) return;

  if (!get(Flag::SEALED)) set(Flag::VALIDATED, false);

  // Loop over the whole hierarchy
  for (auto type = _type; type; type = type->parentType()) {
    LOGGER->debug("Looking at type {} [{} arguments]", type->typeName(), type->arguments().size());

    // Loop over all the arguments
    for (auto entry: type->arguments()) {
      auto &argument = *entry.second;
      LOGGER->debug("Looking at argument {}", argument.name());

      if (_content.count(argument.name()) == 0) {
        LOGGER->debug("No value provided...");
        // No value provided, and no generator
        if (argument.required()) {
          throw argument_error(
              "Argument " + argument.name() + " was required but not given for " + this->type()->toString());
        }
      } else {
        // Sets the value
        auto value = get(argument.name());
        LOGGER->debug("Checking value of {} [type {} vs {}]...", argument.name(), *argument.type(), *value->type());

        // Check if the declared type corresponds to the value type
        if (!entry.second->type()->accepts(value->type())) {
          throw argument_error(
              "Argument '" + argument.name() + "' type is " + value->type()->toString() 
              + ", but requested type was " + entry.second->type()->toString());
        }

        LOGGER->debug("Validating {}...", argument.name());
        value->validate();
      }
    }
  }
  set(Flag::VALIDATED, true);
}

void StructuredValue::setValue(std::string const &name, ptr<StructuredValue> const &value) {
  if (_object) {
    _object->setValue(name, value);
  }
}

void StructuredValue::set(StructuredValue::Flag flag, bool value) {
  if (value) _flags |= (Flags)flag;
  else _flags &= ~((Flags)flag);

  assert(get(flag) == value);
}

bool StructuredValue::get(StructuredValue::Flag flag) const {
  return (Flags)flag & _flags;
}


std::shared_ptr<Job> const & StructuredValue::job() const { 
  return _job; 
}

void StructuredValue::job( std::shared_ptr<Job> const & _job) { 
  this->_job = _job; 
}



/// Returns the string
std::string StructuredValue::asString() const {
  if (!_value.defined()) {
    throw argument_error("Cannot convert value : value undefined");
  }
  return _value.asString();
}

/// Returns the string
bool StructuredValue::asBoolean() const {
  if (!_value.defined()) {
    throw argument_error("Cannot convert value : value undefined");
  }
  return _value.asBoolean();
}

/// Returns an integer
long StructuredValue::asInteger() const {
  if (!_value.defined()) {
    throw argument_error("Cannot convert value : value undefined");
  }
  return _value.asInteger();
}

/// Returns an integer
double StructuredValue::asReal() const {
  if (!_value.defined()) {
    throw argument_error("Cannot convert value : value undefined");
  }
  return _value.asReal();
}

/// Returns a path
Path StructuredValue::asPath() const {
  if (!_value.defined()) {
    throw argument_error("Cannot convert value : value undefined");
  }
  return _value.asPath();
}

/// Returns as array
Value::Array StructuredValue::asArray() const {
  if (!_value.defined()) {
    throw argument_error("Cannot convert value : value undefined");
  }
  return _value.asArray();
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

bool Argument::ignore() const { return _ignore; }

Argument &Argument::ignore(bool ignore) {
  _ignore = ignore;
  return *this;
}


const std::string &Argument::help() const {
  return _help;
}
Argument &Argument::help(const std::string &help) {
  _help = help;
  return *this;
}

Argument &Argument::defaultValue(ptr<StructuredValue> const &defaultValue) {
  _defaultValue = defaultValue;
  _required = false;
  return *this;
}
ptr<StructuredValue> Argument::defaultValue() const { return _defaultValue; }

ptr<Generator> Argument::generator() { return _generator; }
ptr<Generator> const &Argument::generator() const { return _generator; }
Argument &Argument::generator(ptr<Generator> const &generator) {
  _generator = generator;
  return *this;
}

ptr<Type> const &Argument::type() const { return _type ? _type : AnyType; }
Argument &Argument::type(ptr<Type> const &type) {
  _type = type;
  return *this;
}


// ---- Type

SimpleType::SimpleType(TypeName const &tname, ValueType valueType, bool canIgnore)
      : Type(tname, AnyType, true, canIgnore), _valueType(valueType) {}

ptr<Type> AnyType = std::make_shared<Type>(ANY_TYPE, nullptr, true);
ptr<Type> BooleanType = std::make_shared<SimpleType>(BOOLEAN_TYPE, ValueType::BOOLEAN);
ptr<Type> IntegerType = std::make_shared<SimpleType>(INTEGER_TYPE, ValueType::INTEGER);
ptr<Type> RealType = std::make_shared<SimpleType>(REAL_TYPE, ValueType::REAL);
ptr<Type> StringType = std::make_shared<SimpleType>(STRING_TYPE, ValueType::STRING);
ptr<Type> PathType = std::make_shared<SimpleType>(PATH_TYPE, ValueType::PATH, true);
ptr<Type> ArrayType = std::make_shared<Type>(ARRAY_TYPE, nullptr, true, false, true);



Type::Type(TypeName const &type, ptr<Type> parent, bool predefined, bool canIgnore, bool isArray) :
    _type(type), _parent(parent), _predefined(predefined), _canIgnore(canIgnore), _isArray(isArray) {
 }

Type::~Type() {}


void Type::addArgument(ptr<Argument> const &argument) {
  _arguments[argument->name()] = argument;
}

std::unordered_map<std::string, ptr<Argument>> &Type::arguments() {
  return _arguments;
}

std::unordered_map<std::string, ptr<Argument>> const &Type::arguments() const {
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

bool Type::isArray() const { return _isArray; }

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


void Type::setProperty(std::string const &name, StructuredValue::Ptr const &value) {
  _properties[name] = value;
}

StructuredValue::Ptr Type::getProperty(std::string const &name) {
  auto it = _properties.find(name);
  if (it == _properties.end()) return nullptr;
  return it->second;
}


bool Type::accepts(Type::Ptr const &other) const {
  // Go up
  if (_type == ANY_TYPE) return true;

  for(auto current = other; current; current = current->parentType()) {
    if (current->_type == _type) return true;
  }

  return false;

}


// ---- Generators

GeneratorContext::GeneratorContext(Workspace & ws, ptr<StructuredValue> const &sv) : workspace(ws) {
  stack.push_back(sv.get());
}
GeneratorContext::GeneratorContext(Workspace & ws) : workspace(ws) {
}

GeneratorLock::GeneratorLock(GeneratorContext * context, StructuredValue *sv) : context(context) {
  context->stack.push_back(sv);
}

const std::string PathGenerator::TYPE = "path";

ptr<Generator> Generator::createFromJSON(nlohmann::json const &j) {
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

ptr<StructuredValue> PathGenerator::generate(GeneratorContext const &context) {
  Path p = context.workspace.jobsdir();
  auto uuid = context.stack[0]->uniqueIdentifier();

  if (ptr<Task> task = context.stack[0]->task()) {
    p = Path(p, {task->identifier().toString()});
  }

  p = Path(p, {uuid});

  if (!_name.empty()) {
    p = Path(p, { _name });
  }
  return std::make_shared<StructuredValue>(Value(p));
}

PathGenerator::PathGenerator(std::string const &name) : _name(name) {

}

} // xpm namespace
