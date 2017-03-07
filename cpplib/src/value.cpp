//
// Created by Benjamin Piwowarski on 19/01/2017.
//

#include <xpm/xpm.hpp>
#include <xpm/value.hpp>
#include <xpm/common.hpp>
#include "private.hpp"

typedef std::string stdstring;
using nlohmann::json;

namespace xpm {


bool Value::equals(Value const &b) const {
  if (scalarType() != b._scalarType) return false;
  switch (_scalarType) {
    case ValueType::PATH:
    case ValueType::STRING:return _value.string == b._value.string;

    case ValueType::INTEGER:return _value.integer == b._value.integer;

    case ValueType::REAL:return _value.real == b._value.real;

    case ValueType::BOOLEAN:return _value.boolean == b._value.boolean;

    case ValueType::NONE:throw std::runtime_error("none has no type");
  }

  throw std::out_of_range("Scalar type is not known (comparing)");
}

std::shared_ptr<Object> Value::cast(Type::Ptr const &type) {
  if (!type || type == this->type()) return this->shared_from_this();

  auto *simpleType = dynamic_cast<SimpleType*>(type.get());
  if (!simpleType) {
    throw std::runtime_error("Cannot value cast to " + type->toString());
  }

  switch (simpleType->valueType()) {
    case ValueType::PATH: return std::make_shared<Value>(this->asPath());
    case ValueType::STRING: return std::make_shared<Value>(this->asString());
    case ValueType::INTEGER: return std::make_shared<Value>(this->asInteger());
    case ValueType::REAL: return std::make_shared<Value>(this->asReal());

    case ValueType::NONE:throw std::runtime_error("none has no type");
  }

  throw std::out_of_range("Scalar type is not known (comparing)");

}



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

Value::Value(Path const &path) {
  new(&_value.string) std::string(path.toString());
  _scalarType = ValueType::PATH;
  _type = PathType;
}

Value::Value(Value const &other) : Object(other), _scalarType(other._scalarType) {
  _type = other._type;
  switch (_scalarType) {
    case ValueType::NONE:break;

    case ValueType::REAL:_value.real = other._value.real;
      break;

    case ValueType::INTEGER:_value.integer = other._value.integer;
      break;

    case ValueType::BOOLEAN:_value.boolean = other._value.boolean;
      break;

    case ValueType::PATH:
    case ValueType::STRING:new(&_value.string) std::string(other._value.string);
      break;
    default:throw std::out_of_range("Scalar type is not known (copying)");
  }
}


Value &Value::operator=(Value const &other) {
  this->~Value();

  _scalarType = other._scalarType;
  _type = other._type;
  switch (_scalarType) {
    case ValueType::NONE:break;
    case ValueType::REAL:_value.real = other._value.real;
      break;
    case ValueType::INTEGER:_value.integer = other._value.integer;
      break;
    case ValueType::BOOLEAN:_value.boolean = other._value.boolean;
      break;
    case ValueType::PATH:
    case ValueType::STRING:new(&_value.string) std::string(other._value.string);
      break;
  }

  return *this;
}


bool Value::equals(Object const &other) const {
  if (Value const *otherValue = dynamic_cast<Value const *>(&other)) {
    return equals(dynamic_cast<Value const &>(*otherValue));
  }
  return false;
}

long Value::asInteger() {
  switch (_scalarType) {
    case ValueType::NONE:return false;

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
double Value::asReal() {
  switch (_scalarType) {
    case ValueType::NONE:return false;

    case ValueType::REAL: return _value.real;
    case ValueType::INTEGER:return _value.integer;
    case ValueType::BOOLEAN: return _value.boolean ? 1 : 0;
    case ValueType::PATH:
    case ValueType::STRING: return !_value.string.empty();
  }
  throw std::out_of_range("Scalar type is not known (converting to real)");
}

Path Value::asPath() const {
  switch (_scalarType) {
    case ValueType::NONE:
    case ValueType::REAL:
    case ValueType::INTEGER:
    case ValueType::BOOLEAN:
      throw cast_error("Cannot convert value into path");
    case ValueType::PATH:
    case ValueType::STRING: return Path(_value.string);
      break;
  }
  throw std::out_of_range("Scalar type is not known (converting to real)");

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

    case ValueType::PATH:
    case ValueType::STRING: return !_value.string.empty();
      break;

    default:throw std::out_of_range("Scalar type is not known (converting to boolean)");
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

    case ValueType::PATH:
    case ValueType::STRING: return _value.string;
      break;
  }
  throw std::out_of_range("Scalar type is not known (converting to string)");

}

json Value::jsonValue() const {
  switch (scalarType()) {
    case ValueType::STRING:return json(_value.string);

    case ValueType::INTEGER:return json(_value.integer);

    case ValueType::REAL:return json(_value.real);

    case ValueType::BOOLEAN:return json(_value.boolean);

    case ValueType::PATH: return json {{KEY_VALUE, _value.string}, {KEY_TYPE, PATH_TYPE.toString()}};

    case ValueType::NONE:throw std::runtime_error("none has no type");
  }
  throw std::out_of_range("Scalar type is not known (converting to json)");
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
    case ValueType::PATH:
    case ValueType::STRING:d.updateDigest(_value.string);
      break;
  }

  return d.get();
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

Path Value::getPath() const {
  if (_scalarType != ValueType::PATH) throw std::runtime_error("Value is not a path");
  return Path(_value.string);
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
}