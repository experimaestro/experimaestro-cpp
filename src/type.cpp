#include <memory>
#include <utility>
#include <unordered_set>

#include <xpm/type.hpp>
#include <xpm/xpm.hpp>
#include <xpm/json.hpp>

namespace {
  typedef std::pair<xpm::Typename, xpm::Typename> TypenamePair;
}

namespace std {
  template<>
  struct hash<TypenamePair> {
    inline size_t operator()(TypenamePair const & pair) const { 
      return pair.first.hash() + pair.second.hash(); 
    }
  };
}

namespace {
typedef std::unordered_set<std::pair<xpm::Typename, xpm::Typename>> TypenamePairSet;

bool same(xpm::Type const & a, xpm::Type const & b) {
  return a.name() == b.name();
}

TypenamePairSet const & castable_types() {
  static std::unique_ptr<TypenamePairSet> TYPES;
  if (!TYPES) {
    TYPES = std::unique_ptr<TypenamePairSet>(new TypenamePairSet());
    TYPES->insert(TypenamePair(xpm::RealType->name(), xpm::IntegerType->name()));
    }
  return *TYPES;
}
} // end unamed ns


namespace xpm {

using nlohmann::json;
 
 




std::ostream &operator<<(std::ostream &os, const Typename &c) {
  return os << c.toString();
}
std::ostream &operator<<(std::ostream &os, const Type &c) {
  return os << c.toString();
}

 // ---- Type

SimpleType::SimpleType(Typename const &tname, ScalarType scalarType, bool canIgnore)
      : Type(tname, AnyType, true, canIgnore), _valueType(scalarType) {}

ptr<Type> AnyType = Type::any();

Type::Ptr const & Type::any() {
  static auto ANY = mkptr<Type>(ANY_TYPE, nullptr, true);
  return ANY;
}

ptr<Type> BooleanType = mkptr<SimpleType>(BOOLEAN_TYPE, ScalarType::BOOLEAN);
ptr<Type> IntegerType = mkptr<SimpleType>(INTEGER_TYPE, ScalarType::INTEGER);
ptr<Type> RealType = mkptr<SimpleType>(REAL_TYPE, ScalarType::REAL);
ptr<Type> StringType = mkptr<SimpleType>(STRING_TYPE, ScalarType::STRING);
ptr<Type> PathType = mkptr<SimpleType>(PATH_TYPE, ScalarType::PATH, true);

Type::Type(Typename const &type, ptr<Type> parent, bool predefined, bool canIgnore) :
    _type(type), _parent(parent), _predefined(predefined), _canIgnore(canIgnore) {
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

std::shared_ptr<Argument> Type::argument(std::string const & name) {
  auto it = _arguments.find(name);

  return _arguments.end() == it ? nullptr : it->second;
}


void Type::parentType(Ptr const &type) {
  _parent = type;
}

Type::Ptr Type::parentType() {
  return _parent;
}

ptr<Value> Type::create() const {
  return mkptr<MapValue>();
}


Typename const &Type::name() const { return _type; }

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
      definition = arg.type()->name().toString();
    } else {
      definition["type"] = arg.type()->name().toString();
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


void Type::setProperty(std::string const &name, Value::Ptr const &value) {
  _properties[name] = value;
}

Value::Ptr Type::getProperty(std::string const &name) {
  auto it = _properties.find(name);
  if (it == _properties.end()) return nullptr;
  return it->second;
}


bool Type::accepts(Type::Ptr const &other) const {
  // Go up
  if (_type == ANY_TYPE) return true;

  for(auto current = other; current; current = current->parentType()) {
    if (same(*current, *this)) return true;
  }

  return castable_types().find(std::pair<Typename, Typename>(name(), other->name()))
    != castable_types().end();
}

Type::Ptr Type::lca(Type::Ptr const & a, Type::Ptr const & b) {
  std::unordered_set<Typename> set;

  for(auto current = a; current; current = current->parentType()) {
    if (same(*current, *b)) return a;
    set.insert(current->name());
  }

  // Go up in b hierarchy
  for(auto current = b; current; current = current->parentType()) {
    if (set.find(current->name()) != set.end())
      return current;
  }

  // Should not happen...
  return AnyType;
}

ArrayType::ArrayType(Type::Ptr const & componentType) 
  : Type(
      componentType->name().array(), 
      componentType->parentType() ? mkptr<ArrayType>(componentType->parentType()) : nullptr
    ), _componentType(componentType) {
}
   
ptr<Value> ArrayType::create() const {
  return mkptr<ArrayValue>();
}

ptr<Value> SimpleType::create() const {
  return mkptr<ScalarValue>();
}

}