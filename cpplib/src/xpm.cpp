
#include <sstream>
#include <functional>
#include <iostream>

#include <xpm/xpm.h>
#include <xpm/json.hpp>

using nlohmann::json;

namespace xpm {

static const char *const VALUE_TYPE = "$type";

sealed_error::sealed_error() {}
argument_error::argument_error(const std::string &message) : exception(message) {}

struct ToJson {
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

  static json convert(StructuredValue const &sv) {
    json o = json::object();
    for(auto const &entry: sv._content) {
      o[entry.first] = convert(*entry.second);
    }
    return o;

  }
};

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
// --- Objects
// ---

const TypeName ANY_TYPE("any");

StructuredValue::StructuredValue() : _sealed(false), _scalar() {
  std::cerr << "[create] Structured value " << this << std::endl;
}
StructuredValue::StructuredValue(Value &&scalar) : _sealed(false), _scalar(scalar) {
  std::cerr << "[create] Structured value " << this << std::endl;
}
StructuredValue::StructuredValue(Value const &scalar) : _sealed(false), _scalar(scalar) {
  std::cerr << "[create] Structured value " << this << std::endl;
}
//StructuredValue::~StructuredValue() {
//  std::cerr << "[destroying] Structured value " << this << std::endl;
//}


bool StructuredValue::hasValue() const {
  return _scalar.defined();
}

void StructuredValue::type(TypeName const &typeName) {
  (*this)[VALUE_TYPE] = std::make_shared<StructuredValue>(Value(typeName.toString()));
}

TypeName StructuredValue::type() const {
  auto value_ptr = _content.find(VALUE_TYPE);
  if (value_ptr != _content.end()) {
    auto _object = value_ptr->second;
    if (_object->hasValue()) {
      return TypeName(_object->value().toString());
    }
  }

  if (_scalar.defined()) {
    return _scalar.type();
  }

  // Type not defined
  return ANY_TYPE;
}

bool StructuredValue::hasKey(std::string const &key) const {
  return _content.find(key) != _content.end();
}

std::shared_ptr<StructuredValue> &StructuredValue::operator[](const std::string &key) {
  return _content[key];
}

std::shared_ptr<StructuredValue const> StructuredValue::operator[](const std::string &key) const {
  auto value = _content.find(key);
  if (value == _content.end()) throw std::out_of_range(key + " is not defined for object");
  return value->second;
}

Value StructuredValue::value() const {
  return _scalar;
}

void StructuredValue::value(Value const &scalar) {
  if (_sealed) throw sealed_error();
  _scalar = scalar;
}

void StructuredValue::seal() {
  throw std::runtime_error("not implemented");
}

bool StructuredValue::isSealed() const {
  return _sealed;
}

std::string StructuredValue::toJson() const {
  return ToJson::convert(*this).dump();
}

std::shared_ptr<StructuredValue> parse(json const &jsonValue) {
  std::shared_ptr<StructuredValue> value = std::make_shared<StructuredValue>();
  switch (jsonValue.type()) {
    case
      nlohmann::json::value_t::null:break;

    case
      nlohmann::json::value_t::discarded:break;

    case
      nlohmann::json::value_t::object:
      for (json::const_iterator it = jsonValue.begin(); it != jsonValue.end(); ++it) {
        (*value)[it.key()] = parse(it.value());
      }
      break;

    case
      nlohmann::json::value_t::array: {
      ValueArray array;
      for (size_t i = 0; i < jsonValue.size(); ++i) {
        array[i] = parse(jsonValue[i]);
      }
      value->value(std::move(array));
      break;
    }

    case
      nlohmann::json::value_t::string: {
      std::string s = jsonValue;
      value->value(s);
    }
      break;

    case
      nlohmann::json::value_t::boolean:
      value
          ->value((
                      bool) jsonValue);
      break;

    case
      nlohmann::json::value_t::number_integer:
    case
      nlohmann::json::value_t::number_unsigned:
      value
          ->value((
                      long) jsonValue); // TODO: check this is OK

    case
      nlohmann::json::value_t::number_float:break;
  }
  return
      value;
}

std::shared_ptr<StructuredValue> StructuredValue::parse(std::string const &jsonString) {
  return xpm::parse(json::parse(jsonString));
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

Value::Value(std::shared_ptr<Object> const &object) : _type(ValueType::OBJECT) {
  new(&_value.object) std::shared_ptr<Object>(object);
  _value.object = object;
}

Value::Value(ValueArray &&value) : _type(ValueType::ARRAY) {
  new(&_value.array) ValueArray(value);
  _value.array = value;
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

static const TypeName STRING_TYPE("string");
static const TypeName BOOLEAN_TYPE("boolean");
static const TypeName INTEGER_TYPE("integer");
static const TypeName REAL_TYPE("real");
static const TypeName PATH_TYPE("path");
static const TypeName ARRAY_TYPE("array");

TypeName const &Value::type() const {
  switch (_type) {
    case ValueType::STRING:return STRING_TYPE;

    case ValueType::INTEGER:return INTEGER_TYPE;

    case ValueType::REAL:return REAL_TYPE;

    case ValueType::BOOLEAN:return BOOLEAN_TYPE;

    case ValueType::ARRAY: return ARRAY_TYPE;

    case ValueType::OBJECT: return _value.object->type()->typeName();

    case ValueType::NONE:throw std::runtime_error("none has no type");
  }
}

bool Value::defined() const {
  return _type != ValueType::NONE;
}

ValueType const Value::scalarType() const {
  return _type;
}




// ---
// --- Task
// ---

Argument::Argument(std::string const &name) : _name(name), _type(AnyType), _required(true) {}

void Argument::required(bool required) {
  _required = required;
}
const std::string &Argument::help() const {
  return _help;
}
void Argument::help(const std::string &help) {
  _help = help;
}

const std::shared_ptr<Type> BooleanType = std::make_shared<Type>(BOOLEAN_TYPE);
const std::shared_ptr<Type> IntegerType = std::make_shared<Type>(INTEGER_TYPE);
const std::shared_ptr<Type> RealType = std::make_shared<Type>(REAL_TYPE);
const std::shared_ptr<Type> StringType = std::make_shared<Type>(STRING_TYPE);
const std::shared_ptr<Type> AnyType = std::make_shared<Type>(ANY_TYPE);
const std::shared_ptr<Type> PathType = std::make_shared<Type>(PATH_TYPE);

Type::Type(TypeName const &type, std::shared_ptr<Type> const &parent)
    : _type(type), _parent(parent) {}

Type::~Type() {}

void Type::addArgument(std::shared_ptr<Argument> const &argument) {
  arguments[argument->name()] = argument;
}

std::string Type::toJson() {
  json response;

  json jsonArguments = json::object();
  for (auto const &entry: arguments) {
    Argument const &arg = *entry.second;
    json definition = {
        {"help", arg.help()},
        {"required", arg.required()},
        {"type", arg.type() ? arg.type()->typeName().toString() : ANY_TYPE.toString()},
    };

    if (arg.defaultValue().defined())
      definition["default"] = ToJson::convert(arg.defaultValue());

    jsonArguments[entry.first] = definition;
  }

  response["arguments"] = jsonArguments;
  response["type"] = typeName().toString();
  if (_parent) {
    response["parent"] = _parent->typeName().toString();
  }
  return response.dump();
}
int Type::hash() const {
  return std::hash<Type>()(*this);
}

// ---- Generators

StructuredValue::Ptr PathGenerator::generate(StructuredValue &object) {
  return nullptr;
}

const PathGenerator PathGenerator::SINGLETON = PathGenerator();
const PathGenerator &pathGenerator = PathGenerator::SINGLETON;


// ---- Object

Object::Object() : _type(AnyType) {
  _value = std::make_shared<StructuredValue>();
  std::cerr << "[created] Object " << this << std::endl;
}

Object::~Object() {
  std::cerr << "[destroying] Object " << this << std::endl;
}

/** Sets the structured value */
void Object::setValue(StructuredValue::Ptr value) {
  _value = value;
}

void Object::set(std::string const &key, StructuredValue::Ptr const &value) {
  (*_value)[key] = value;
  setValue(key, value);
}

std::shared_ptr<StructuredValue const> Object::getValue() const {
  return _value;
}
std::string Object::toString() const {
  return "Object of type " + _type->toString();
}

std::string Object::json() const {
  return _value->toJson();
}


// ---- Task

Task::Task(std::shared_ptr<Type> const &type) : _type(type) {
}


// ---- REGISTER

Register::Register() {
  addType(IntegerType);
  addType(RealType);
  addType(StringType);
  addType(BooleanType);
}
Register::~Register() {}

void Register::addType(std::shared_ptr<Type> const &type) {
  _types[type->typeName()] = type;
}

void Register::addTask(std::shared_ptr<Task> const &task) {
  _tasks[task->typeName()] = task;
}

std::shared_ptr<Task> Register::getTask(TypeName const &typeName) const {
  auto it = _tasks.find(typeName);
  if (it != _tasks.end()) {
    return it->second;
  }
  return nullptr;
}

std::shared_ptr<Type> Register::getType(TypeName const &typeName) const {
  auto it = _types.find(typeName);
  if (it != _types.end()) {
    return it->second;
  }
  return nullptr;
}

// Find a type given a type name
std::shared_ptr<Type> Register::getType(std::shared_ptr<Object const> const &object) const {
  return object->type();
}

std::shared_ptr<Object> Register::build(std::shared_ptr<StructuredValue> const &value) const {
  auto const &objectTypeName = value->type();
  const std::shared_ptr<Type> &objectType = getType(objectTypeName);
  std::cerr << "Building object with type " << objectTypeName.toString()
            << " / " << (objectType ? "OK" : "not registered!") << std::endl;

  // Create the object
  std::cerr << "Creating object..." << std::endl;
  auto object = objectType ? objectType->create() : std::make_shared<Object>();
  if (!object) object = std::make_shared<Object>();

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
      auto const hasKey = value->hasKey(key);
      if (!hasKey && entry.second->required()) {
        throw argument_error("Argument " + key + " was required but not provided");
      }

      // Build subtype
      if (hasKey) {
        std::cerr << "Building " << key << std::endl;
        auto subvalue = build((*value)[key]);

        // Set argument
        std::cerr << "Setting " << key << std::endl;
        object->set(key, subvalue);
      } else {
        auto scalar = entry.second->defaultValue();
        if (scalar.defined()) {
          object->set(key, std::make_shared<StructuredValue>(scalar));
        }
      }
    }
  }

  return object;
}

} // xpm namespace
