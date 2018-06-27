//
// Created by Benjamin Piwowarski on 19/01/2017.
//

#include <regex>
#include <xpm/xpm.hpp>
#include <xpm/type.hpp>
#include <xpm/value.hpp>
#include <xpm/common.hpp>
#include <yaml-cpp/yaml.h>
#include <__xpm/common.hpp>

typedef std::string stdstring;
using nlohmann::json;

namespace xpm {

typedef Value::Array ValueArray;

const Value Value::NONE(ValueType::NONE);

bool Value::equals(Value const &b) const {
  if (scalarType() != b._scalarType) return false;
  switch (_scalarType) {
    case ValueType::PATH:
    case ValueType::STRING:return _value.string == b._value.string;

    case ValueType::INTEGER:return _value.integer == b._value.integer;

    case ValueType::REAL:return _value.real == b._value.real;

    case ValueType::BOOLEAN:return _value.boolean == b._value.boolean;
    
    case ValueType::ARRAY: {
      if (_value.array.size() != b._value.array.size()) 
        return false;
      for(size_t i = 0; i < _value.array.size(); ++i) {
        if (!_value.array[i]->equals(*b._value.array[i]))
          return false;
      }
      return true;
    }

    case ValueType::NONE:
      return true;

    case ValueType::UNSET:
      throw std::runtime_error("equals: unset has no type");
  }

  throw std::out_of_range("Scalar type is not known (comparing)");
}

Value Value::cast(Type::Ptr const &type) {
  auto *simpleType = dynamic_cast<SimpleType*>(type.get());
  if (!simpleType) {
    throw std::runtime_error("Cannot cast value to " + type->toString());
  }

  switch (simpleType->valueType()) {
    case ValueType::PATH: return Value(this->asPath());
    case ValueType::STRING: return Value(this->asString());
    case ValueType::INTEGER: return Value(this->asInteger());
    case ValueType::BOOLEAN: return Value(this->asBoolean());
    case ValueType::REAL: return Value(this->asReal());
    case ValueType::ARRAY: return Value(this->asArray());
    case ValueType::NONE: return *this; // no change
    case ValueType::UNSET:throw std::runtime_error("cast: unset has no type");
  }

  throw std::out_of_range("Scalar type is not known (casting)");
}

namespace {
  static const std::regex RE_INTEGER(R"(\d+)");
  static const std::regex RE_REAL(
      R"([+-]?(?:0|[1-9]\d*)(?:\.\d*)?(?:[eE][+\-]?\d+)?)");

}

Value Value::fromYAML(YAML::Node const &node) {
  switch (node.Type()) {
  case YAML::NodeType::Sequence: {
    NOT_IMPLEMENTED();
    // Array a;
    // for(auto const & sn: node) {
    // }
    // break;
  }

  case YAML::NodeType::Null:
    return NONE;

  case YAML::NodeType::Scalar: {
    std::string s = node.Scalar();
    if (node.Tag() == "!") {
      // A string
      return s;
    } else if (node.Tag() == "?") {
      // boolean
      if (s == "Y" || s == "true" || s == "Yes" || s == "ON")
        return Value(true);
      if (s == "N" || s == "false" || s == "No" || s == "OFF")
        return Value(false);

      // Integer
      if (std::regex_match(s, RE_INTEGER)) {
        return Value(std::atol(s.c_str()));
      }

      if (std::regex_match(s, RE_REAL)) {
        return Value(std::atof(s.c_str()));
      }

      return s;
    }
  }

  default:
    throw argument_error("Cannot convert YAML to value: not a scalar");
  }
}

Value Value::fromString(std::string const & s, ptr<Type> const & hint) {
  // Just a string
  if (hint == AnyType) {
    return Value(s);
  }

  if (hint == PathType) {
    return Value(Path(s));
  }

  if (hint == IntegerType) {
    if (std::regex_match(s, RE_INTEGER)) {
      return Value(std::atol(s.c_str()));
    }
    throw argument_error(s + " cannot be interpreted as an integer");
  }

  if (hint == BooleanType) {
    if (s == "Y" || s == "true" || s == "Yes" || s == "ON")
      return Value(true);
    if (s == "N" || s == "false" || s == "No" || s == "OFF")
      return Value(false);
    throw argument_error(s + " cannot be interpreted as a boolean");
  }

  if (hint == RealType) {
    if (std::regex_match(s, RE_REAL)) {
      return Value(std::atof(s.c_str()));
    }
    throw argument_error(s + " cannot be interpreted as a real");
  }


  throw argument_error("Type " + hint->name().toString() + " is not a scalar type");
}


Value::~Value() {
  switch (_scalarType) {
    case ValueType::STRING: 
      _value.string.~stdstring();
      break;
    case ValueType::ARRAY: 
      _value.array.~ValueArray();
      break;
    default:
      // Do nothing for other values
      break;
  }
  _scalarType = ValueType::UNSET;
}

Value::Union::~Union() {
  // Does nothing: handled by Scalar
}

Value::Union::Union() {
  // Does nothing: handled by Scalar
}

Value::Value() : _scalarType(ValueType::UNSET) {
}

Value::Value(Array const & array) : _scalarType(ValueType::ARRAY) {
  // placement new
  new(&_value.array) Array(array);
}

Value::Value(Array && array) {
  // placement new
  new(&_value.array) Array(std::move(array));
}

Value::Value(ValueType scalarType) : _scalarType(scalarType) {
}

Value::Value(double value) : _scalarType(ValueType::REAL) {
  _value.real = value;
}

Value::Value(long value) : _scalarType(ValueType::INTEGER) {
  _value.integer = value;
}

Value::Value(int value) : _scalarType(ValueType::INTEGER) {
  _value.integer = value;
}

Value::Value(bool value) : _scalarType(ValueType::BOOLEAN) {
  _value.boolean = value;
}

Value::Value(std::string const &value) : _scalarType(ValueType::STRING) {
  // placement new
  new(&_value.string) std::string(value);
}

Value::Value(Path const &path) : _scalarType(ValueType::PATH) {
  new(&_value.string) std::string(path.toString());
}

Value::Value(Value const &other) : _scalarType(other._scalarType) {
  switch (_scalarType) {
    case ValueType::NONE:break;

    case ValueType::REAL:
    _value.real = other._value.real;
      break;

    case ValueType::INTEGER:
      _value.integer = other._value.integer;
      break;

    case ValueType::BOOLEAN:
      _value.boolean = other._value.boolean;
      break;

    case ValueType::PATH:
    case ValueType::STRING:
      new(&_value.string) std::string(other._value.string);
      break;

    case ValueType::ARRAY:

      break;
    default:throw std::out_of_range("Scalar type is not known (copying)");
  }
}


Value::Value(Register & xpmRegister, nlohmann::json const &jsonValue) {
  switch(jsonValue.type()) {
    case nlohmann::json::value_t::null:
    case nlohmann::json::value_t::discarded:
      _scalarType = ValueType::NONE;
      break;


    case nlohmann::json::value_t::array: {
      new(&_value.array) ValueArray();
      for (json::const_iterator it = jsonValue.begin(); it != jsonValue.end(); ++it) {
        _value.array.push_back(std::make_shared<Parameters>(xpmRegister, *it));
      }      
      _scalarType = ValueType::ARRAY;
      break;
    }

    case nlohmann::json::value_t::string:
      new(&_value.string) std::string();
      _value.string = jsonValue;
      _scalarType = ValueType::STRING;
      break;

    case nlohmann::json::value_t::boolean:
      _value.boolean = jsonValue;
      _scalarType = ValueType::BOOLEAN;
      break;

    case nlohmann::json::value_t::number_integer:
    case nlohmann::json::value_t::number_unsigned:
      _value.integer = jsonValue;
      _scalarType = ValueType::INTEGER;
      break;

    case nlohmann::json::value_t::number_float:{
      // Try first as integer
      if (std::trunc((double)jsonValue) == (double)jsonValue) {
        _value.integer = jsonValue;
        _scalarType = ValueType::INTEGER;
      } else {
        _value.real = jsonValue;
        _scalarType = ValueType::REAL;
      }
      break;
    }

    default:
      throw exception("unhanlded JSON type for a Value");
  }
}

ptr<Type> Value::type() const {
  switch (_scalarType) {
    case ValueType::NONE:
    case ValueType::UNSET:
      return AnyType;

    case ValueType::REAL:
      return RealType;

    case ValueType::INTEGER:
      return IntegerType;

    case ValueType::BOOLEAN:
      return BooleanType;

    case ValueType::PATH:
      return PathType;

    case ValueType::STRING:
      return StringType;

    case ValueType::ARRAY: {
      ptr<Type> type;
      for(auto const &v: _value.array) {
        if (!type) type = v->type();
        else {
          type = Type::lca(type, v->type());
        }
      }
      
      return mkptr<ArrayType>(type ? type : AnyType);
    }
  }

      throw exception("unhanlded type for a Value");
}


Value &Value::operator=(Value const &other) {
  this->~Value();

  _scalarType = other._scalarType;
  switch (_scalarType) {
    case ValueType::NONE:
    case ValueType::UNSET:
      break;
    
    case ValueType::REAL:_value.real = other._value.real;
      break;

    case ValueType::INTEGER:_value.integer = other._value.integer;
      break;

    case ValueType::BOOLEAN:_value.boolean = other._value.boolean;
      break;

    case ValueType::ARRAY:
        new(&_value.array) Value::Array(other._value.array);
        break;

    case ValueType::PATH:
    case ValueType::STRING:new(&_value.string) std::string(other._value.string);
      break;
  }

  return *this;
}

long Value::asInteger() const {
  switch (_scalarType) {
    case ValueType::NONE: 
    case ValueType::UNSET: 
      throw cast_error("cannot convert none/unset " + std::to_string(_value.real) + " to integer");

    case ValueType::REAL:
      if (_value.real == (int)_value.real) {
        return (int)_value.real;
      }
      throw cast_error("cannot convert real " + std::to_string(_value.real) + " to integer");


    case ValueType::INTEGER:return _value.integer;

    case ValueType::BOOLEAN: return _value.boolean ? 1 : 0;

    case ValueType::STRING: throw cast_error("cannot convert string to integer");
    case ValueType::PATH: throw cast_error("cannot convert path to integer");
    default:throw std::out_of_range("Scalar type is not known (converting to integer)");
  }

}
double Value::asReal() const {
  switch (_scalarType) {
    case ValueType::NONE:
    case ValueType::UNSET: 
      throw cast_error("cannot convert none/unset " + std::to_string(_value.real) + " to real");

    case ValueType::REAL: return _value.real;
    case ValueType::INTEGER:return _value.integer;
    case ValueType::BOOLEAN: return _value.boolean ? 1 : 0;
    case ValueType::PATH:
    case ValueType::STRING: return !_value.string.empty();
    case ValueType::ARRAY: 
      if (_value.array.size() != 1)
        throw std::out_of_range("Cannot convert arrays which are not singleton");
      else
        return _value.array[0]->asReal();

  }
  throw std::out_of_range("Scalar type is not known (converting to real)");
}

Path Value::asPath() const {
  switch (_scalarType) {
    case ValueType::NONE:
    case ValueType::REAL:
    case ValueType::INTEGER:
    case ValueType::BOOLEAN:
    case ValueType::UNSET:
      throw cast_error("Cannot convert value into path");
    case ValueType::ARRAY:
      if (_value.array.size() != 1)
        throw std::out_of_range("Cannot convert arrays which are not singleton");
      else
        return _value.array[0]->asPath();
    case ValueType::PATH:
    case ValueType::STRING: return Path(_value.string);
      break;
  }
  throw std::out_of_range("Scalar type is not known (converting to real)");

}


bool Value::asBoolean() const {
  switch (_scalarType) {
    case ValueType::NONE:return false;

    case ValueType::REAL: return _value.real;
      break;

    case ValueType::INTEGER:return _value.integer;
      break;

    case ValueType::BOOLEAN: return _value.boolean;
      break;

    case ValueType::ARRAY:
      return !_value.array.empty();

    case ValueType::PATH:
    case ValueType::STRING: return !_value.string.empty();
      break;

    default:throw std::out_of_range("Scalar type is not known (converting to boolean)");
  }
}

Value::Array Value::asArray() const {
  switch (_scalarType) {
    case ValueType::NONE:
      return Value::Array();

    case ValueType::ARRAY: 
      return _value.array;

    default: {
      Value::Array array;
      array.push_back(std::make_shared<Parameters>(*this));
      return array;
    }
  }
}


std::string Value::asString() const {
  switch (_scalarType) {
    case ValueType::UNSET:
    case ValueType::NONE:
      throw cast_error("Cannot convert none/unset to string");

    case ValueType::REAL: return std::to_string(_value.real);
      break;

    case ValueType::INTEGER:return std::to_string(_value.integer);
      break;

    case ValueType::BOOLEAN: return std::to_string(_value.boolean);
      break;

    case ValueType::ARRAY:
      if (_value.array.size() != 1)
        throw std::out_of_range("Cannot convert arrays which are not singleton");
      else
        return _value.array[0]->asString();

    case ValueType::PATH:
    case ValueType::STRING: return _value.string;
      break;
  }
  throw std::out_of_range("Scalar type is not known (converting to string)");

}

json Value::toJson() const {
  switch (scalarType()) {
    case ValueType::NONE: return nullptr;

    case ValueType::STRING:return json(_value.string);

    case ValueType::INTEGER:return json(_value.integer);

    case ValueType::REAL:return json(_value.real);

    case ValueType::BOOLEAN:return json(_value.boolean);

    case ValueType::PATH: return json {{KEY_VALUE, _value.string}, {KEY_TYPE, PATH_TYPE.toString()}};

    case ValueType::ARRAY: {
      nlohmann::json array = {};
      for(auto const &v: _value.array) {
          array.push_back(v->toJson());
      }
      return array;
    }

    case ValueType::UNSET:throw std::runtime_error("to json: unset has no type");
  }
  throw std::out_of_range("Scalar type is not known (converting to json)");
}

void Value::updateDigest(Digest &d) const {

  d.updateDigest(scalarType());

  // Hash value
  switch (_scalarType) {
    case ValueType::UNSET:
    case ValueType::NONE:
      // No value content for these types
      break;

    case ValueType::BOOLEAN:
      d.updateDigest(_value.boolean);
      break;

    case ValueType::INTEGER:
      d.updateDigest(_value.integer);
      break;
    case ValueType::REAL:
      d.updateDigest(_value.real);
      break;

    case ValueType::PATH:
    case ValueType::STRING:
      d.updateDigest(_value.string);
      break;

    case ValueType::ARRAY:
      d.updateDigest(_value.array.size());
      for(auto const & element : _value.array) {
        d.updateDigest(*element);
      }
  }
}

bool Value::defined() const {
  return _scalarType != ValueType::UNSET;
}

bool Value::null() const {
  return _scalarType == ValueType::NONE;
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

Path Value::getPath() const {
  if (_scalarType != ValueType::PATH) throw std::runtime_error("Value is not a path");
  return Path(_value.string);
}

std::string const &Value::getString() {
  if (_scalarType != ValueType::STRING) throw std::runtime_error("Value is not a string");
  return _value.string;
}

void Value::push_back(ptr<Parameters> const &element) {
  if (_scalarType != ValueType::ARRAY) throw std::runtime_error("Value is not an array: cannot push an element");
  _value.array.push_back(element);
}

size_t Value::size() const {
  if (_scalarType != ValueType::ARRAY) throw std::runtime_error("Value is not an array: cannot get the size");
  return _value.array.size();
}

ptr<Parameters> &Value::operator[](const size_t index) {
  if (_scalarType != ValueType::ARRAY) throw std::runtime_error("Value is not an array: cannot get an element");
  return _value.array[index];
}

}
