//
// Created by Benjamin Piwowarski on 19/01/2017.
//

#include <regex>
#include <xpm/xpm.hpp>
#include <xpm/type.hpp>
#include <xpm/scalar.hpp>
#include <xpm/common.hpp>
#include <yaml-cpp/yaml.h>
#include <__xpm/common.hpp>

typedef std::string stdstring;
using nlohmann::json;

namespace xpm {

const Scalar Scalar::NONE(ScalarType::NONE);

bool Scalar::equals(Scalar const &b) const {
  if (scalarType() != b._scalarType) return false;
  switch (_scalarType) {
    case ScalarType::PATH:
    case ScalarType::STRING:return _value.string == b._value.string;

    case ScalarType::INTEGER:return _value.integer == b._value.integer;

    case ScalarType::REAL:return _value.real == b._value.real;

    case ScalarType::BOOLEAN:return _value.boolean == b._value.boolean;
    

    case ScalarType::NONE:
      return true;

    case ScalarType::UNSET:
      throw std::runtime_error("equals: unset has no type");
  }

  throw std::out_of_range("Scalar type is not known (comparing)");
}

Scalar Scalar::cast(Type::Ptr const &type) {
  auto *simpleType = dynamic_cast<SimpleType*>(type.get());
  if (!simpleType) {
    throw std::runtime_error("Cannot cast value to " + type->toString());
  }

  switch (simpleType->valueType()) {
    case ScalarType::PATH: return Scalar(this->asPath());
    case ScalarType::STRING: return Scalar(this->asString());
    case ScalarType::INTEGER: return Scalar(this->asInteger());
    case ScalarType::BOOLEAN: return Scalar(this->asBoolean());
    case ScalarType::REAL: return Scalar(this->asReal());
    case ScalarType::NONE: return *this; // no change
    case ScalarType::UNSET:throw std::runtime_error("cast: unset has no type");
  }

  throw std::out_of_range("Scalar type is not known (casting)");
}

const std::regex &re_integer() {
  static const std::regex RE_INTEGER(R"(\d+)");
  return RE_INTEGER;
}

const std::regex &re_real() {
  static const std::regex RE_REAL(
      R"([+-]?(?:0|[1-9]\d*)(?:\.\d*)?(?:[eE][+\-]?\d+)?)");
  return RE_REAL;
}


Scalar Scalar::fromYAML(YAML::Node const &node) {
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
        return Scalar(true);
      if (s == "N" || s == "false" || s == "No" || s == "OFF")
        return Scalar(false);

      // Integer
      if (std::regex_match(s, re_integer())) {
        return Scalar(std::atol(s.c_str()));
      }

      if (std::regex_match(s, re_real())) {
        return Scalar(std::atof(s.c_str()));
      }

      return s;
    }
  }

  default:
    throw argument_error("Cannot convert YAML to value: not a scalar");
  }
}

Scalar Scalar::fromString(std::string const & s, ptr<Type> const & hint) {
  // Just a string
  if (hint == AnyType) {
    return Scalar(s);
  }

  if (hint == PathType) {
    return Scalar(Path(s));
  }

  if (hint == IntegerType) {
    if (std::regex_match(s, re_integer())) {
      return Scalar(std::atol(s.c_str()));
    }
    throw argument_error(s + " cannot be interpreted as an integer");
  }

  if (hint == BooleanType) {
    if (s == "Y" || s == "true" || s == "Yes" || s == "ON")
      return Scalar(true);
    if (s == "N" || s == "false" || s == "No" || s == "OFF")
      return Scalar(false);
    throw argument_error(s + " cannot be interpreted as a boolean");
  }

  if (hint == RealType) {
    if (std::regex_match(s, re_real())) {
      return Scalar(std::atof(s.c_str()));
    }
    throw argument_error(s + " cannot be interpreted as a real");
  }


  throw argument_error("Type " + hint->name().toString() + " is not a scalar type");
}


Scalar::~Scalar() {
  switch (_scalarType) {
    case ScalarType::STRING: 
      _value.string.~stdstring();
      break;
    default:
      // Do nothing for other values
      break;
  }
  _scalarType = ScalarType::UNSET;
}

Scalar::Union::~Union() {
  // Does nothing: handled by Scalar
}

Scalar::Union::Union() {
  // Does nothing: handled by Scalar
}

Scalar::Scalar() : _scalarType(ScalarType::UNSET) {
}

Scalar::Scalar(ScalarType scalarType) : _scalarType(scalarType) {
}

Scalar::Scalar(double value) : _scalarType(ScalarType::REAL) {
  _value.real = value;
}

Scalar::Scalar(long value) : _scalarType(ScalarType::INTEGER) {
  _value.integer = value;
}

Scalar::Scalar(int value) : _scalarType(ScalarType::INTEGER) {
  _value.integer = value;
}

Scalar::Scalar(bool value) : _scalarType(ScalarType::BOOLEAN) {
  _value.boolean = value;
}

Scalar::Scalar(std::string const &value) : _scalarType(ScalarType::STRING) {
  // placement new
  new(&_value.string) std::string(value);
}

Scalar::Scalar(Path const &path) : _scalarType(ScalarType::PATH) {
  new(&_value.string) std::string(path.toString());
}

Scalar::Scalar(Scalar const &other) : _scalarType(other._scalarType) {
  switch (_scalarType) {
    case ScalarType::NONE:break;

    case ScalarType::REAL:
    _value.real = other._value.real;
      break;

    case ScalarType::INTEGER:
      _value.integer = other._value.integer;
      break;

    case ScalarType::BOOLEAN:
      _value.boolean = other._value.boolean;
      break;

    case ScalarType::PATH:
    case ScalarType::STRING:
      new(&_value.string) std::string(other._value.string);
      break;

    default:throw std::out_of_range("Scalar type is not known (copying)");
  }
}


Scalar::Scalar(nlohmann::json const &jsonValue) {
  switch(jsonValue.type()) {
    case nlohmann::json::value_t::null:
    case nlohmann::json::value_t::discarded:
      _scalarType = ScalarType::NONE;
      break;

    case nlohmann::json::value_t::string:
      new(&_value.string) std::string();
      _value.string = jsonValue;
      _scalarType = ScalarType::STRING;
      break;

    case nlohmann::json::value_t::boolean:
      _value.boolean = jsonValue;
      _scalarType = ScalarType::BOOLEAN;
      break;

    case nlohmann::json::value_t::number_integer:
    case nlohmann::json::value_t::number_unsigned:
      _value.integer = jsonValue;
      _scalarType = ScalarType::INTEGER;
      break;

    case nlohmann::json::value_t::number_float:{
      // Try first as integer
      if (std::trunc((double)jsonValue) == (double)jsonValue) {
        _value.integer = jsonValue;
        _scalarType = ScalarType::INTEGER;
      } else {
        _value.real = jsonValue;
        _scalarType = ScalarType::REAL;
      }
      break;
    }

    default:
      throw exception(fmt::format("Unhandled JSON type for a Scalar: {}", (int)jsonValue.type()));
  }
}

ptr<Type> Scalar::type() const {
  switch (_scalarType) {
    case ScalarType::NONE:
    case ScalarType::UNSET:
      return AnyType;

    case ScalarType::REAL:
      return RealType;

    case ScalarType::INTEGER:
      return IntegerType;

    case ScalarType::BOOLEAN:
      return BooleanType;

    case ScalarType::PATH:
      return PathType;

    case ScalarType::STRING:
      return StringType;

    // case ScalarType::ARRAY: {
    //   ptr<Type> type;
    //   for(auto const &v: _value.array) {
    //     if (!type) type = v->type();
    //     else {
    //       type = Type::lca(type, v->type());
    //     }
    //   }      
    // return mkptr<ArrayType>(type ? type : AnyType);
    // }
  }

  throw exception("unhanlded type for a Scalar");
}

std::string Scalar::toString() const {
  if (!defined()) return "";
  return asString();
}


Scalar &Scalar::operator=(Scalar const &other) {
  this->~Scalar();

  _scalarType = other._scalarType;
  switch (_scalarType) {
    case ScalarType::NONE:
    case ScalarType::UNSET:
      break;
    
    case ScalarType::REAL:_value.real = other._value.real;
      break;

    case ScalarType::INTEGER:_value.integer = other._value.integer;
      break;

    case ScalarType::BOOLEAN:_value.boolean = other._value.boolean;
      break;

    case ScalarType::PATH:
    case ScalarType::STRING:new(&_value.string) std::string(other._value.string);
      break;
  }

  return *this;
}

long Scalar::asInteger() const {
  switch (_scalarType) {
    case ScalarType::NONE: 
    case ScalarType::UNSET: 
      throw cast_error("cannot convert none/unset " + std::to_string(_value.real) + " to integer");

    case ScalarType::REAL:
      if (_value.real == (int)_value.real) {
        return (int)_value.real;
      }
      throw cast_error("cannot convert real " + std::to_string(_value.real) + " to integer");


    case ScalarType::INTEGER:return _value.integer;

    case ScalarType::BOOLEAN: return _value.boolean ? 1 : 0;

    case ScalarType::STRING: throw cast_error("cannot convert string to integer");
    case ScalarType::PATH: throw cast_error("cannot convert path to integer");
    default:throw std::out_of_range("Scalar type is not known (converting to integer)");
  }

}
double Scalar::asReal() const {
  switch (_scalarType) {
    case ScalarType::NONE:
    case ScalarType::UNSET: 
      throw cast_error("cannot convert none/unset " + std::to_string(_value.real) + " to real");

    case ScalarType::REAL: return _value.real;
    case ScalarType::INTEGER:return _value.integer;
    case ScalarType::BOOLEAN: return _value.boolean ? 1 : 0;
    case ScalarType::PATH:
    case ScalarType::STRING: return !_value.string.empty();
  }
  throw std::out_of_range("Scalar type is not known (converting to real)");
}

Path Scalar::asPath() const {
  switch (_scalarType) {
    case ScalarType::NONE:
    case ScalarType::REAL:
    case ScalarType::INTEGER:
    case ScalarType::BOOLEAN:
    case ScalarType::UNSET:
      throw cast_error("Cannot convert value into path");
    case ScalarType::PATH:
    case ScalarType::STRING: return Path(_value.string);
      break;
  }
  throw std::out_of_range("Scalar type is not known (converting to real)");

}


bool Scalar::asBoolean() const {
  switch (_scalarType) {
    case ScalarType::NONE:return false;

    case ScalarType::REAL: return _value.real;
      break;

    case ScalarType::INTEGER:return _value.integer;
      break;

    case ScalarType::BOOLEAN: return _value.boolean;
      break;

    case ScalarType::PATH:
    case ScalarType::STRING: return !_value.string.empty();
      break;

    default:throw std::out_of_range("Scalar type is not known (converting to boolean)");
  }
}

std::string Scalar::asString() const {
  switch (_scalarType) {
    case ScalarType::UNSET:
    case ScalarType::NONE:
      throw cast_error("Cannot convert none/unset to string");

    case ScalarType::REAL: return std::to_string(_value.real);
      break;

    case ScalarType::INTEGER:return std::to_string(_value.integer);
      break;

    case ScalarType::BOOLEAN: return std::to_string(_value.boolean);
      break;

    case ScalarType::PATH:
    case ScalarType::STRING: return _value.string;
      break;
  }
  throw std::out_of_range("Scalar type is not known (converting to string)");

}

json Scalar::toJson() const {
  switch (scalarType()) {
    case ScalarType::NONE: return nullptr;

    case ScalarType::STRING:return json(_value.string);

    case ScalarType::INTEGER:return json(_value.integer);

    case ScalarType::REAL:return json(_value.real);

    case ScalarType::BOOLEAN:return json(_value.boolean);

    case ScalarType::PATH: return json {{KEY_VALUE, _value.string}, {KEY_TYPE, PATH_TYPE.toString()}};

    case ScalarType::UNSET:throw std::runtime_error("to json: unset has no type");
  }
  throw std::out_of_range("Scalar type is not known (converting to json)");
}

void Scalar::updateDigest(Digest &d) const {

  d.updateDigest(scalarType());

  // Hash value
  switch (_scalarType) {
    case ScalarType::UNSET:
    case ScalarType::NONE:
      // No value content for these types
      break;

    case ScalarType::BOOLEAN:
      d.updateDigest(_value.boolean);
      break;

    case ScalarType::INTEGER:
      d.updateDigest(_value.integer);
      break;
    case ScalarType::REAL:
      d.updateDigest(_value.real);
      break;

    case ScalarType::PATH:
    case ScalarType::STRING:
      d.updateDigest(_value.string);
      break;

    // case ScalarType::ARRAY:
    //   d.updateDigest(_value.array.size());
    //   for(auto const & element : _value.array) {
    //     d.updateDigest(*element);
    //   }
  }
}

bool Scalar::defined() const {
  return _scalarType != ScalarType::UNSET;
}

bool Scalar::null() const {
  return _scalarType == ScalarType::NONE;
}

ScalarType const Scalar::scalarType() const {
  return _scalarType;
}

bool Scalar::getBoolean() const {
  if (_scalarType != ScalarType::BOOLEAN) throw std::runtime_error("Scalar is not a boolean");
  return _value.boolean;
}

double Scalar::getReal() const {
  if (_scalarType != ScalarType::REAL) throw std::runtime_error("Scalar is not a boolean");
  return _value.real;
}

long Scalar::getInteger() const {
  if (_scalarType != ScalarType::INTEGER) throw std::runtime_error("Scalar is not a boolean");
  return _value.integer;
}

Path Scalar::getPath() const {
  if (_scalarType != ScalarType::PATH) throw std::runtime_error("Scalar is not a path");
  return Path(_value.string);
}

std::string const &Scalar::getString() {
  if (_scalarType != ScalarType::STRING) throw std::runtime_error("Scalar is not a string");
  return _value.string;
}


}
