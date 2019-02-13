#include <sstream>
#include <iostream>
#include <unordered_set>
#include <fstream>
#include <typeinfo>
#include <unordered_set>

// Demangle
#include <cxxabi.h>

#include <xpm/common.hpp>
#include <xpm/xpm.hpp>
#include <xpm/register.hpp>
#include <xpm/scalar.hpp>
#include <xpm/task.hpp>
#include <xpm/workspace.hpp>
#include <xpm/connectors/connectors.hpp>
#include <xpm/commandline.hpp>

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

/// Scalar
const std::string KEY_VALUE = "$value";

static const auto RESTRICTED_KEYS = std::unordered_set<std::string> {KEY_TYPE, KEY_TASK, KEY_VALUE, KEY_JOB};

const Typename STRING_TYPE("string");
const Typename BOOLEAN_TYPE("boolean");
const Typename INTEGER_TYPE("integer");
const Typename REAL_TYPE("real");
const Typename ANY_TYPE("any");
const Typename PATH_TYPE("path");

static const std::unordered_set<Typename> IGNORED_TYPES = {PATH_TYPE};

void Outputable::output(std::ostream &out) const {
  out << "Object of type " << demangle(this) << std::endl;
}

// ---
// --- Type names
// ---

Typename::Typename(std::string const &name) : name(name) {}
Typename::Typename(Typename const & parent, std::string const &localname) : name(parent.name + "." + localname) {
}

std::string const & Typename::toString() const {
  return name;
}

int Typename::hash() const {
  return (int) std::hash<std::string>{}(name);
}

Typename Typename::operator()(std::string const &localname) const {
  return Typename(name + "." + localname);
}

std::string Typename::localName() const {
  const auto i = name.rfind(".");
  if (i == std::string::npos) return name;
  return name.substr(i + 1);
}

Typename Typename::array() const {
  return Typename(name + "[]");
}


// ---
// --- Structured value
// ---

Object::~Object() {}
void Object::setValue(std::string const &name, std::shared_ptr<Value> const & value) {}

void Object::run() {
  throw assertion_error("Object is not a task: cannot run it!");
}

void Object::init() {
}

Value::~Value() {
}


Value::Value() : _flags(0) {
}

class DummyJob : public Job {
public:
  DummyJob(nlohmann::json const & j) : Job(Path((std::string const &)(j["locator"])), nullptr) {
  }
  virtual ~DummyJob() {}
  virtual void kill() {}
  virtual void run(std::unique_lock<std::mutex> && jobLock, std::vector<ptr<Lock>> & locks) override { 
    throw cast_error("This is dummy job - it cannot be run!"); 
  }
};


Value::Value(Value const &other) : _flags(other._flags) {

}

std::shared_ptr<Value> Value::create(Register &xpmRegister, nlohmann::json const &jsonValue) {

  switch (jsonValue.type()) {

    // --- Object
    case nlohmann::json::value_t::object: {
      std::shared_ptr<Value> p;
      std::shared_ptr<Type> _type = AnyType;

      // (1) First, get the type of the object
      if (jsonValue.count(KEY_TYPE) > 0) {
        auto typeName = Typename((std::string const &) jsonValue[KEY_TYPE]);
        _type = xpmRegister.getType(typeName);
        if (!_type) {
          _type = mkptr<Type>(typeName);
          _type->placeholder(true);
          xpmRegister.addType(_type);
          LOGGER->warn("Could not find type '{}' in registry: using undefined type", typeName);
        }
      }

      // Create the right type of object
      if (_type->scalar() || jsonValue.count(KEY_VALUE) > 0) {
        p = mkptr<ScalarValue>(Scalar());
      } else if (_type->array()) {
        auto _p = mkptr<ArrayValue>();
        _p->_ctype = dynamic_cast<ArrayType&>(*_type).componentType();
        p = _p;
      } else {
        // Otherwise, this is a map
        auto _p = mkptr<MapValue>();
        _p->type(_type);
        p = _p;
      }
      
      // (2) Fill from JSON
      for (json::const_iterator it = jsonValue.begin(); it != jsonValue.end(); ++it) {
        if (it.key() == KEY_VALUE) {
          if (!p->isScalar()) throw argument_error("Scalar cannot be a map or an array");

          // Infer type from value
          auto value = Scalar(it.value());
          ptr<Type> vtype;
          if (!value.null()) {
            vtype = value.type();
            if (!_type->accepts(vtype)) {
              try {
                LOGGER->debug("Trying to cast {} to {}", vtype->name().toString(), _type->name().toString());
                value = value.cast(_type);
              } catch(...) {
                throw argument_error(fmt::format("Incompatible types: {} (given) cannot be converted to {} (expected)", 
                  vtype->name().toString(), _type->name().toString()));
              }
            }
          } 

          p->asScalar()->_value = value;
          
        } else if (it.key() == KEY_TYPE) {
          // ignore
        } else if (it.key() == KEY_JOB) {
          if (!p) p = mkptr<MapValue>();
          p->asMap()->job(mkptr<DummyJob>(it.value()));
        } else if (it.key() == KEY_TASK) {
          if (!p) p = mkptr<MapValue>();
          p->asMap()->_type = _type;
          std::dynamic_pointer_cast<MapValue>(p)->_task = xpmRegister.getTask(it.value().get<std::string>(), true);
        } else {
          if (!p) p = mkptr<MapValue>();
          if (_type) p->asMap()->_type = _type;
          p->asMap()->set(it.key(), Value::create(xpmRegister, it.value()));
        }
      }

      LOGGER->debug("Got a value of type {}", _type ? _type->toString() : "?");
      return p;
    }

    case nlohmann::json::value_t::array: {
      auto array = mkptr<ArrayValue>();
      for(auto child: jsonValue) {
        array->push_back(create(xpmRegister, child));
      }
      return array;
    }

    default: break;
  }

  return mkptr<ScalarValue>(Scalar(jsonValue));
}

// Convert to JSON
json Value::toJson() const {
  nlohmann::json o = {};
  
  if (type()->name() != AnyType->name()) {
    o[KEY_TYPE] = type()->name().toString();
  }

  return o;
}




std::string Value::toJsonString() {
  return toJson().dump();
}


/// Internal digest function
std::array<unsigned char, DIGEST_LENGTH> Value::digest() const {
  Digest d;
  this->updateDigest(d);
  return d.get();
};


void Value::seal() {
  if (get(Flag::SEALED)) return;

  foreachChild([](auto &child) { child->seal(); });

  set(Flag::SEALED, true);
}

bool Value::isSealed() const {
  return get(Flag::SEALED);
}

bool Value::isDefault() const {
  return get(Flag::DEFAULT);
}

bool Value::ignore() const {
  return get(Flag::IGNORE);
}

bool Value::canIgnore() {
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

std::string Value::uniqueIdentifier() const {
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

void Value::configure(Workspace & ws) {
  GeneratorContext context(ws);
  generate(context);
  validate();
  seal();
}

ptr<Object> Value::createObjects(xpm::Register &xpmRegister) {
  // Create sub-objects
  foreachChild([&](ptr<Value> const &p) { p->createObjects(xpmRegister); });

  return nullptr;
}

void Value::addDependencies(Job & job,  bool skipThis) {
  // Stop here
  if (canIgnore())
    return;

  foreachChild([&](auto p) { p->addDependencies(job, false); });
}

void Value::_generate(GeneratorContext &context) {}

void Value::generate(GeneratorContext &context) {
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

    // ... generate children
    foreachChild([&](auto p) { p->generate(context); });

    // and self
    _generate(context);

    set(Flag::GENERATED, true);
  }
}

ptr<MapValue> Value::asMap() {
  auto p = std::dynamic_pointer_cast<MapValue>(this->shared_from_this());
  if (!p) throw argument_error("Cannot cast to Map");
  return p;
}

ptr<ArrayValue> Value::asArray() {
  auto p = std::dynamic_pointer_cast<ArrayValue>(this->shared_from_this());
  if (!p) throw argument_error("Cannot cast to Array");
  return p;
}

ptr<ScalarValue> Value::asScalar() {
  auto p = std::dynamic_pointer_cast<ScalarValue>(this->shared_from_this());
  if (!p) throw argument_error("Cannot cast to Scalar");
  return p;
}

bool Value::isMap() const {
  return dynamic_cast<MapValue const *>(this) != nullptr;
}

bool Value::isArray() const {
  return dynamic_cast<ArrayValue const *>(this) != nullptr;
}

bool Value::isScalar() const {
  return dynamic_cast<ScalarValue const *>(this) != nullptr;
}


void Value::validate() {
  if (get(Flag::VALIDATED)) return;
  _validate();
  set(Flag::VALIDATED, true);
}

void Value::_validate() {
}

void Value::set(Value::Flag flag, bool value) {
  if (value) _flags |= (Flags)flag;
  else _flags &= ~((Flags)flag);

  assert(get(flag) == value);
}

bool Value::get(Value::Flag flag) const {
  return ((Flags)flag) & _flags;
}


void Value::foreachChild(std::function<void(std::shared_ptr<Value> const &)> f) {}


std::map<std::string, Scalar> Value::tags() const {
  std::map<std::string, Scalar> map;
  retrieveTags(map, "");
  return map;
}

void Value::retrieveTags(std::map<std::string, Scalar> &tags, std::string const & context) const {
  const_cast<Value*>(this)->foreachChild([&tags, &context](auto c) {
    c->retrieveTags(tags, context);
  });
}


//
// --- Complex value

ComplexValue::ComplexValue() {}
ComplexValue::ComplexValue(ComplexValue const &other) 
  : Value(other), _tags(other._tags), _tagContext(other._tagContext) {}

ComplexValue::~ComplexValue() {}


void ComplexValue::retrieveTags(std::map<std::string, Scalar> &tags, std::string const & context) const  {
  auto c = _tagContext.empty() ? context : ( context + _tagContext + "/" );
  Value::retrieveTags(tags, c);

  for(auto pair: _tags) {
    auto r = tags.insert({ c + pair.first, pair.second });
    if (!r.second) throw assertion_error("Tag " + pair.first + " was present more than once in the value");
  }
  
}

void ComplexValue::setTagContext(std::string const & name) {
  _tagContext = name;
}


void ComplexValue::addTag(std::string const & name, Scalar scalar) {
  _tags[name] = scalar;
}


//
// --- Array value
//


ArrayValue::ArrayValue() : _ctype(AnyType) {}

ArrayValue::~ArrayValue() {}

void ArrayValue::updateDigest(Digest & digest) const {
  digest.updateDigest(ParametersTypes::ARRAY);
  digest.updateDigest(_array.size());
  for(auto const & c : _array) {
    c->updateDigest(digest);
  }
}

 // If array, validate the array
void ArrayValue::_validate() {
  for(size_t i = 0, N = size(); i < N; ++i) {
    try {
      (*this)[i]->validate();
    } catch(parameter_error &e) {
        throw e.addPath(fmt::format("[{}]", i));
    }
  }
}


void ArrayValue::foreachChild(std::function<void(std::shared_ptr<Value> const &)> f) {
  for(auto p: _array) {
    f(p);
  }
}

std::shared_ptr<Value> ArrayValue::operator[](size_t index) {
  return _array[index];
}


size_t ArrayValue::size() const {
  return _array.size();
}

ptr<Type> ArrayValue::type() const {
  return mkptr<ArrayType>(_ctype);
}

void ArrayValue::push_back(std::shared_ptr<Value> const & value) {
  _ctype = _array.empty() ? value->type() : Type::lca(_ctype, value->type());
  _array.push_back(value);
}

bool ArrayValue::equals(Value const & other) const {
  auto other_ = dynamic_cast<ArrayValue const *>(&other);
  if (!other_) return false;

  if (_array.size() != other_->_array.size()) 
    return false;

  for(size_t i = 0; i < size(); ++i) {
    if (!_array[i]->equals(*other_->_array[i])) {
      return false;
    }
  }
  return true;
}

void ArrayValue::outputJson(std::ostream &out, CommandContext & context) const {
  out << "[";
  for(size_t i = 0; i < size(); ++i) {
    if (i > 0) out << ", ";
    _array[i]->outputJson(out, context);
  }
  out << "]";
}

nlohmann::json ArrayValue::toJson() const {
  auto j = nlohmann::json::array();
  for(auto p: _array) {
    j.push_back(p->toJson());
  }
  return j;
}

std::shared_ptr<Value> ArrayValue::copy() {
  return mkptr<ArrayValue>(*this);
}

ArrayValue::ArrayValue(ArrayValue const &other)
  : ComplexValue(other), _ctype(other._ctype) {
  for(auto p: other._array) {
    _array.push_back(p->copy());
  }
}


//
// --- Map parameters
//

MapValue::MapValue() : _type(AnyType) {}
MapValue::MapValue(ptr<Type> const &type) : _type(type) {}

MapValue::~MapValue() {}

ptr<Type> MapValue::type() const {
  return _type;
}
void MapValue::type(ptr<Type> const & type) {
  _type = type;
}

ptr<Value> MapValue::copy() {
  return mkptr<MapValue>(*this);
}

MapValue::MapValue(MapValue const &other) : ComplexValue(other) {
  _job = other._job;
  _object = other._object;
  _task = other._task;
  _flags = other._flags;
  _type = other._type;
  for(auto item: other._map) {
    _map[item.first] = item.second->copy();
  }
}

bool MapValue::equals(Value const &other) const {
  NOT_IMPLEMENTED();
}

  
void MapValue::addDependencies(Job & job,  bool skipThis) {
  if (_job) {
    LOGGER->info("Found dependency resource {}", _job);
    job.addDependency(_job->createDependency());
  } else {
    foreachChild([&](auto p) { p->addDependencies(job, false); });
  }
}

  
ptr<Value> MapValue::get(const std::string &key) {
  auto value = _map.find(key);
  if (value == _map.end()) throw std::out_of_range(key + " is not defined for object");
  return value->second;
}

void MapValue::_validate() {
  // Loop over the whole hierarchy
  for (auto type = _type; type; type = type->parentType()) {
    LOGGER->debug("Looking at type {} [{} arguments]", type->name(), type->arguments().size());
    
    // Loop over all the arguments
    for (auto entry: type->arguments()) {
      auto &argument = *entry.second;
      LOGGER->debug("Looking at argument {}", argument.name());
      
      auto value = _map.count(argument.name()) ? get(argument.name()) : nullptr;
      
      if (!value || (value->isScalar() && value->asScalar()->null())) {
        LOGGER->debug("No value provided for {}...", argument.name());
        // No value provided, and no generator
        if (argument.required()) {
          throw parameter_error(
                                "Argument " + argument.name() + " was required but not given for " + this->type()->toString());
        }
      } else {
        // Sets the value
        LOGGER->debug("Checking value of {} [type {} vs value type {}]...", argument.name(), *argument.type(), *value->type());
        
        // Check if the declared type corresponds to the value type
        if (!entry.second->type()->accepts(value->type())) {
          throw parameter_error(
                                "type is " + value->type()->toString()
                                + ", but requested type was " + entry.second->type()->toString())
          .addPath(argument.name());
        }
        
        LOGGER->debug("Validating {}...", argument.name());
        try {
          value->validate();
        } catch(parameter_error &e) {
          throw e.addPath(argument.name());
        }
      }
    }
  }
}

void MapValue::_generate(GeneratorContext &context) {
  // ... for missing arguments
  for (auto type = _type; type; type = type->parentType()) {
    for (auto entry : type->arguments()) {
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
          value->set(Flag::IGNORE, argument.ignored());
          set(argument.name(), value);
        } else if (argument.constant()) {
          LOGGER->debug("Setting constant value for {}...", argument.name());
          auto value = argument.constant()->copy();
          value->set(Flag::DEFAULT, false);
          value->set(Flag::IGNORE, false);
          set(argument.name(), value);
        } else if (!argument.required()) {
          // Set value null
          LOGGER->debug("Setting null value for {}...", argument.name());
          auto value = mkptr<ScalarValue>(Scalar::NONE);
          value->Value::set(Flag::DEFAULT, true);
          set(argument.name(), value);
        }
      }
    }
  }
}


void MapValue::outputJson(std::ostream &out, CommandContext & context) const {
  out << "{";
  bool first = true;

  auto comma = [&first,&out] {        
    if (first) first = false;
    else out << ',';
  };

  if (type()) {
    out << "\"" << KEY_TYPE << "\": \"" << type()->name() << "\"";
    first = false;
  }

  if (job()) {
    comma();
    out << "\"" << KEY_JOB << "\": " <<  job()->toJson();
  }

  for (auto type = this->type(); type; type = type->parentType()) {
    for (auto entry: type->arguments()) {
      Argument &argument = *entry.second;
      comma();
      out << "\"" << entry.first << "\":";
      
      if (hasKey(argument.name())) {
        const_cast<MapValue*>(this)->get(argument.name())->outputJson(out, context);
      } else {
        out << "null";
      }
    }
  }

  out << "}"; 
}
    

nlohmann::json MapValue::toJson() const {
    // No content
  if (_map.empty() && !_task && !type() && !Value::get(Flag::DEFAULT)) {
        return nullptr;
    }
    
    // We have some values
    json o = json::object();
    for (auto const &entry: _map) {
        o[entry.first] = entry.second->toJson();
    }
    
    if (_type) {
        o[KEY_TYPE] = _type->name().toString();
    }
    
    if (_task) {
        o[KEY_TASK] = _task->identifier().toString();
    }
    
    return o;
}
  
void MapValue::updateDigest(Digest & digest) const {
  digest.updateDigest(type()->name().toString());

  if (_task) {
    digest.updateDigest(_task->identifier().toString());
  } else {
    digest.updateDigest(0);
  }

  for (auto &item: _map) {
    auto const &key = item.first;

    if (item.second->canIgnore()) {
      // Remove keys that can be ignored (e.g. paths)
      continue;
    }

    // Update digest with key
    digest.updateDigest(key);

    // Update digest with *value digest* (this allows digest caching)
    digest.updateDigest(item.second->digest());
  }

}

std::shared_ptr<Object> MapValue::createObjects(xpm::Register &xpmRegister) {
  Value::createObjects(xpmRegister);
  
  _object = xpmRegister.createObject(shared_from_this());
  if (!_object) throw assertion_error("Object is null");

  // Set the values
  for(auto &kv: _map) {
    LOGGER->debug("Setting value {}", kv.first);
    setObjectValue(kv.first, kv.second);
  }

  // Perform further initialization
  _object->init();
  
  return _object;
}


void MapValue::setObjectValue(std::string const &name, ptr<Value> const &value) {
  if (_object) {
    _object->setValue(name, value);
  }
}

void MapValue::foreachChild(std::function<void(std::shared_ptr<Value> const &)> f) {
  for(auto &kv: _map) {
    f(kv.second);
  }
}

std::shared_ptr<Job> const & MapValue::job() const { 
  return _job; 
}

void MapValue::job( std::shared_ptr<Job> const & _job) { 
  this->_job = _job; 
}

bool MapValue::hasKey(std::string const &key) const {
  return _map.find(key) != _map.end();
}


void MapValue::object(ptr<Object> const &object) {
  _object = object;
}

ptr<Object> MapValue::object() {
  return _object;
}

void MapValue::task(ptr<Task> const &task) {
  _task = task;
}

ptr<Task> MapValue::task() {
  return _task;
}


ptr<Value> MapValue::set(const std::string &key, ptr<Value> const &_value) {
  if (Value::get(Flag::SEALED)) {
    throw sealed_error();
  }

  auto value = _value->copy();

  if (RESTRICTED_KEYS.count(key) > 0)
    throw argument_error("Cannot access directly to " + key);

  auto it = _map.find(key);
  _map[key] = value;

  // Set default / ignore
  auto itA = type()->arguments().find(key);
  if (itA != type()->arguments().end()) {
    auto & argument = *itA->second;
    if (argument.defaultValue() && argument.defaultValue()->equals(*value)) {
      LOGGER->debug("Scalar is default");
      value->set(Flag::DEFAULT, true);
    }
    if (argument.ignored()) {
      value->set(Flag::IGNORE, true);
    }
  }

  // And for the object
  setObjectValue(key, value);

  return it == _map.end() ? nullptr : it->second;
}

// ---
// --- Scalar values
// ---


//
// --- Scalar parameters


ScalarValue::ScalarValue() {
}

ScalarValue::ScalarValue(long value) {
  _value = Scalar(value);
}

ScalarValue::ScalarValue(std::string const &value) {
  _value = Scalar(value);
}

ScalarValue::ScalarValue(Path const &value) {
  _value = Scalar(value);
}

ScalarValue::ScalarValue(bool value) {
  _value = Scalar(value);
}

ScalarValue::ScalarValue(double value) {
  _value = Scalar(value);
}


ScalarValue::ScalarValue(Scalar const &v) {
  _value = v;
} 

std::string ScalarValue::toString() const{
  return _value.asString();
} 

Scalar const & ScalarValue::value() {
  return _value;
}


/// Returns the string
std::string ScalarValue::asString() const {
  if (!_value.defined()) {
    throw argument_error("Cannot convert value : value undefined");
  }
  return _value.asString();
}

/// Returns the string
bool ScalarValue::asBoolean() const {
  if (!_value.defined()) {
    throw argument_error("Cannot convert value : value undefined");
  }
  return _value.asBoolean();
}

/// Returns an integer
long ScalarValue::asInteger() const {
  if (!_value.defined()) {
    throw argument_error("Cannot convert value : value undefined");
  }
  return _value.asInteger();
}

/// Returns an integer
double ScalarValue::asReal() const {
  if (!_value.defined()) {
    throw argument_error("Cannot convert value : value undefined");
  }
  return _value.asReal();
}

/// Returns a path
Path ScalarValue::asPath() const {
  if (!_value.defined()) {
    throw argument_error("Cannot convert value : value undefined");
  }
  return _value.asPath();
}

ScalarType ScalarValue::valueType() const {
  return _value.scalarType();
}


nlohmann::json ScalarValue::toJson() const {
  return _value.toJson();
}

bool ScalarValue::hasValue() const {
  return _value.defined();
}

bool ScalarValue::null() const {
  return _value.null();
}

void ScalarValue::set(YAML::Node const &node) {
  _value = Scalar::fromYAML(node);
}


void ScalarValue::set(bool value) {
  _value = Scalar(value);
}

void ScalarValue::set(long value) {
  _value = Scalar(value);
}

std::shared_ptr<Type> ScalarValue::type() const {
  return _value.type();
}

void ScalarValue::set(std::string const & value, ptr<Type> const & typeHint) {
  _value = Scalar::fromString(value, typeHint);
}

bool ScalarValue::equals(Value const &other) const {
  auto other_ = dynamic_cast<ScalarValue const *>(&other);
  if (!other_) return false;
  return _value.equals(other_->_value);
}

void ScalarValue::outputJson(std::ostream &out, CommandContext & context) const {
  switch(_value.scalarType()) {
    case ScalarType::PATH:
      out << "{\"" << xpm::KEY_TYPE << "\":\"" << xpm::PathType->name().toString() << "\",\""
          << xpm::KEY_VALUE << "\": \"";
      out << context.connector.resolve(asPath());
      out << "\"}";
      break;

    default:
      out << toJson();
      break;
  }
}

void ScalarValue::updateDigest(Digest & digest) const {
  _value.updateDigest(digest);
}

std::shared_ptr<Value> ScalarValue::copy() {
  return mkptr<ScalarValue>(*this);
}

ScalarValue::ScalarValue(ScalarValue const &other) 
  : Value(other), _tag(other._tag) {
  _value = other._value;
}


/// Tag this value
void ScalarValue::tag(std::string const &name) {
  _tag = name;
}

void ScalarValue::retrieveTags(std::map<std::string, Scalar> &tags, std::string const & context) const {
  if (_tag.empty()) return;
  auto r = tags.insert({ context + _tag, _value });
  if (!r.second) throw assertion_error("Tag " + _tag + " was present more than once in the value");
}

// ---
// --- Task
// ---


Argument::Argument(std::string const &name) : _name(name), _type(AnyType), _required(true), _ignored(false),  _constant(false), _generator(nullptr) {
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

bool Argument::ignored() const { return _ignored; }

Argument &Argument::ignored(bool ignored) {
  _ignored = ignored;
  return *this;
}

ptr<Value> Argument::constant() const { 
  return _constant ? _value : nullptr; 
}

Argument &Argument::constant(ptr<Value> const &constant) {
  _constant = (bool)constant;
  _value = constant;
  return *this;
}


const std::string &Argument::help() const {
  return _help;
}
Argument &Argument::help(const std::string &help) {
  _help = help;
  return *this;
}

Argument &Argument::defaultValue(ptr<Value> const &defaultValue) {
  _value = defaultValue;
  _required = false;
  _constant = false;
  return *this;
}
ptr<Value> Argument::defaultValue() const { 
  if (_constant) return nullptr;
  return _value; 
}

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



// ---- Generators

GeneratorContext::GeneratorContext(Workspace & ws, ptr<Value> const &sv) : workspace(ws) {
  stack.push_back(sv.get());
}
GeneratorContext::GeneratorContext(Workspace & ws) : workspace(ws) {
}

GeneratorLock::GeneratorLock(GeneratorContext * context, Value *sv) : context(context) {
  context->stack.push_back(sv);
}

const std::string PathGenerator::TYPE = "path";

ptr<Generator> Generator::createFromJSON(nlohmann::json const &j) {
  std::string type = j["type"];
  if (type == PathGenerator::TYPE) {
    return mkptr<PathGenerator>(j);
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

ptr<Value> PathGenerator::generate(GeneratorContext const &context) {
  Path p = context.workspace.jobsdir();
  auto uuid = context.stack[0]->uniqueIdentifier();

  if (ptr<Task> task = context.stack[0]->asMap()->task()) {
    p = Path(p, {task->identifier().toString()});
  }

  p = Path(p, {uuid});

  if (!_name.empty()) {
    p = Path(p, { _name });
  }
  return mkptr<ScalarValue>(Scalar(p));
}

PathGenerator::PathGenerator(std::string const &name) : _name(name) {

}

} // xpm namespace
