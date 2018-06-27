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

const Typename STRING_TYPE("string");
const Typename BOOLEAN_TYPE("boolean");
const Typename INTEGER_TYPE("integer");
const Typename REAL_TYPE("real");
const Typename ANY_TYPE("any");
const Typename PATH_TYPE("path");

static const std::unordered_set<Typename> IGNORED_TYPES = {PATH_TYPE};


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

Typename::Typename(std::string const &name) : name(name) {}

std::string Typename::toString() const {
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
void Object::setValue(std::string const &name, std::shared_ptr<Parameters> const & value) {}

void Object::run() {
  throw assertion_error("Object is not a task: cannot run it!");
}

void Object::init() {
}

Parameters::~Parameters() {
}


Parameters::Parameters() : _flags(0), _type(AnyType) {
}

class DummyJob : public Job {
public:
  DummyJob(nlohmann::json const & j) : Job(Path((std::string const &)(j["locator"])), nullptr) {
  }
  virtual ~DummyJob() {}
  virtual void run() override { throw cast_error("This is dummy job - it cannot be run!"); }
};

std::shared_ptr<Parameters> Parameters::create(Register &xpmRegister, nlohmann::json const &jsonValue) {

  switch (jsonValue.type()) {

    // --- Object
    case nlohmann::json::value_t::object: {
      std::shared_ptr<Parameters> p;
      std::shared_ptr<Type> _type;

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
      
      // (2) Fill from JSON
      for (json::const_iterator it = jsonValue.begin(); it != jsonValue.end(); ++it) {
        if (it.key() == KEY_VALUE) {
          if (p) throw argument_error("Value cannot be something else");

          // Infer type from value
          auto value = Value(it.value());
          ptr<Type> vtype;
          if (!value.null()) {
            vtype = value.type();
            if (!_type->accepts(vtype)) {
              try {
                LOGGER->debug("Trying to cast {} to {}", vtype->name().toString(), _type->name().toString());
                value = value.cast(_type);
                vtype = value.type();
              } catch(...) {
                throw argument_error(fmt::format("Incompatible types: {} (given) cannot be converted to {} (expected)", 
                  vtype->name().toString(), _type->name().toString()));
              }
            }
          } else {
            vtype = _type;
          }
          p = mkptr<ScalarParameters>(value);
          p->_type = vtype;
          
        } else if (it.key() == KEY_TYPE) {
          // ignore
        } else if (it.key() == KEY_JOB) {
          job(mkptr<DummyJob>(it.value()));
        } else if (it.key() == KEY_TASK) {
          if (!p) p = mkptr<MapParameters>();
          p->_type = _type;
          std::dynamic_pointer_cast<MapParameters>(p)->_task = xpmRegister.getTask(it.value(), true);
        } else {
          if (!p) p = mkptr<MapParameters>();
          p->_type = _type;
          std::dynamic_pointer_cast<MapParameters>(p)->set(it.key(), Parameters::create(xpmRegister, it.value()));
        }
      }

      LOGGER->debug("Got an object of type {}", _type ? _type->toString() : "?");
      return p ? p : mkptr<MapParameters>();
    }

    default: break;
  }
    
  return mkptr<ScalarParameters>(Value(jsonValue));
}

// Convert to JSON
json Parameters::toJson() const {
  nlohmann::json o = {};
  
  if (_type) {
    o[KEY_TYPE] = _type->name().toString();
  }

  return o;
}




std::string Parameters::toJsonString() {
  return toJson().dump();
}


/// Internal digest function
std::array<unsigned char, DIGEST_LENGTH> Parameters::digest() const {
  Digest d;
  d.updateDigest(*this);
  return d.get();
};



ptr<Type> Parameters::type() const {
  return _type;
}


void Parameters::seal() {
  if (get(Flag::SEALED)) return;

  foreachChild([](auto &child) { child->seal(); });

  set(Flag::SEALED, true);
}

bool Parameters::isSealed() const {
  return get(Flag::SEALED);
}

bool Parameters::isDefault() const {
  return get(Flag::DEFAULT);
}

bool Parameters::ignore() const {
  return get(Flag::IGNORE);
}

bool Parameters::canIgnore() {
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

std::string Parameters::uniqueIdentifier() const {
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

/** Get type */
void Parameters::type(ptr<Type> const &type) {
  _type = type;
}

void Parameters::configure(Workspace & ws) {
  GeneratorContext context(ws);
  generate(context);
  validate();
  seal();
}

ptr<Object> Parameters::createObjects(xpm::Register &xpmRegister) {
  // Create sub-objects
  foreachChild([&](ptr<Parameters> const &p) { p->createObjects(xpmRegister); });

  return nullptr;
}

void Parameters::addDependencies(Job & job,  bool skipThis) {
  // Stop here
  if (canIgnore())
    return;

  if (_job) {
    LOGGER->info("Found dependency resource {}", _job);
    job.addDependency(_job->createDependency());
  } else {
    foreachChild([&](auto p) { p->addDependencies(job, false); });
  }
}

void Parameters::_generate(GeneratorContext &context) {}

void Parameters::generate(GeneratorContext &context) {
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

void Parameters::_validate() {
  if (get(Flag::VALIDATED)) return;

  if (auto this_ = dynamic_cast<ArrayParameters*>(this)) {
   
  } else if (auto this_ = dynamic_cast<MapParameters*>(this)) {
    // Loop over the whole hierarchy
    for (auto type = _type; type; type = type->parentType()) {
      LOGGER->debug("Looking at type {} [{} arguments]", type->name(), type->arguments().size());

      // Loop over all the arguments
      for (auto entry: type->arguments()) {
        auto &argument = *entry.second;
        LOGGER->debug("Looking at argument {}", argument.name());

        auto value = _map.count(argument.name()) ? get(argument.name()) : nullptr;

        if (!value || value->null()) {
          LOGGER->debug("No value provided for {}...", argument.name());
          // No value provided, and no generator
          if (argument.required()) {
            throw parameter_error(
                "Argument " + argument.name() + " was required but not given for " + this->type()->toString());
          }
        } else {
          // Sets the value
          LOGGER->debug("Checking value of {} [type {} vs {}]...", argument.name(), *argument.type(), *value->type());

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
  set(Flag::VALIDATED, true);
}

void Parameters::set(Parameters::Flag flag, bool value) {
  if (value) _flags |= (Flags)flag;
  else _flags &= ~((Flags)flag);

  assert(get(flag) == value);
}

bool Parameters::get(Parameters::Flag flag) const {
  return ((Flags)flag) & _flags;
}


void Parameters::foreachChild(std::function<void(std::shared_ptr<Parameters> const &)> f) {}

//
// --- Array parameters
//

void ArrayParameters::updateDigest(Digest & digest) const {
  digest.updateDigest(ParametersTypes::ARRAY);
}

 // If array, validate the array
void ArrayParameters::_validate() {
  for(size_t i = 0, N = size(); i < N; ++i) {
    try {
      (*this)[i]->validate();
    } catch(parameter_error &e) {
        throw e.addPath(fmt::format("[{}]", i));
    }
  }
}


void ArrayParameters::foreachChild(std::function<void(std::shared_ptr<Parameters> const &)> f) {
  for(auto p: _array) {
    f(p);
  }
}

std::shared_ptr<Parameters> ArrayParameters::operator[](size_t index) {
  return _array[index];
}


size_t ArrayParameters::size() const {
  return _array.size();
}

void ArrayParameters::push_back(std::shared_ptr<Parameters> const & parameters) {
  _array.push_back(parameters);
  _type = Type::lca(_type, parameters->type());
}

bool ArrayParameters::equals(Parameters const & other) const {
  auto other_ = dynamic_cast<ArrayParameters const *>(&other);
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

void ArrayParameters::outputJson(std::ostream &out, CommandContext & context) const {
  out << "{\"" << xpm::KEY_TYPE << "\":\"" << type()->name().toString() << "\",\""
      << xpm::KEY_VALUE << "\": [";
  for(size_t i = 0; i < size(); ++i) {
    if (i > 0) out << ", ";
    fill(context, out, (*conf)[i]);
  }
  out << "]}";
}

nlohmann::json ArrayParameters::toJson() const {
  auto j = nlohmann::json::array();
  for(auto p: _array) {
    j.push_back(p->toJson());
  }
  return j;
}

std::shared_ptr<Parameters> ArrayParameters::copy() {
  auto sv = mkptr<ArrayParameters>();
  for(auto p: _array) {
    sv->_array.push_back(p->copy());
  }
  return sv;
}

//
// --- Map parameters
//

ptr<Parameters> MapParameters::copy() {
  auto sv = mkptr<MapParameters>();
  sv->_job = _job;
  sv->_object = _object;
  sv->_task = _task;
  sv->_flags = _flags;
  sv->_type = _type;
  sv->_map =_map;

  return sv;
}

  
ptr<Parameters> MapParameters::get(const std::string &key) {
  auto value = _map.find(key);
  if (value == _map.end()) throw std::out_of_range(key + " is not defined for object");
  return value->second;
}

void MapParameters::_generate(GeneratorContext &context) {
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
        } else if (!argument.required()) {
          // Set value null
          LOGGER->debug("Setting null value for {}...", argument.name());
          auto value = mkptr<Parameters>(Value::NONE);
          value->set(Flag::DEFAULT, true);
          set(argument.name(), value);
        }
      }
    }
  }
}


void MapParameters::outputJson(std::ostream &out, CommandContext & context) const {
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
    out << "\"$job\": " <<  job()->toJson() << std::endl;
  }

  for (auto type = type(); type; type = type->parentType()) {
    for (auto entry: type->arguments()) {
      Argument &argument = *entry.second;
      comma();
      out << "\"" << entry.first << "\":";
      
      if (hasKey(argument.name())) {
        fill(context, out, get(argument.name()));
      } else {
        out << "null";
      }
    }
  }

  out << "}"; 
}
    

nlohmann::json MapParameters::toJson() const {
    // No content
    if (_map.empty() && !_task && !type() && !get(Flag::DEFAULT)) {
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
  
void MapParameters::updateDigest(Digest & sv) const {
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

std::shared_ptr<Object> MapParameters::createObjects(xpm::Register &xpmRegister) {
  _object = xpmRegister.createObject(shared_from_this());
  if (_object) {
    // Set the values
    for(auto &kv: _map) {
      LOGGER->debug("Setting value {}", kv.first);
      setObjectValue(kv.first, kv.second);
    }
  }
  
  // Perform further initialization
  _object->init();
  return _object;
}


void MapParameters::setObjectValue(std::string const &name, ptr<Parameters> const &value) {
  if (_object) {
    _object->setValue(name, value);
  }
}

void MapParameters::foreachChild(std::function<void(std::shared_ptr<Parameters> const &)> f) {
  for(auto &kv: _map) {
    f(kv.second);
  }
}

std::shared_ptr<Job> const & MapParameters::job() const { 
  return _job; 
}

void MapParameters::job( std::shared_ptr<Job> const & _job) { 
  this->_job = _job; 
}

bool MapParameters::hasKey(std::string const &key) const {
  return _map.find(key) != _map.end();
}


void MapParameters::object(ptr<Object> const &object) {
  _object = object;
}

ptr<Object> MapParameters::object() {
  return _object;
}

void MapParameters::task(ptr<Task> const &task) {
  _task = task;
}

ptr<Task> MapParameters::task() {
  return _task;
}


ptr<Parameters> MapParameters::set(const std::string &key, ptr<Parameters> const &value) {
  if (get(Flag::SEALED)) {
    throw sealed_error();
  }

  if (RESTRICTED_KEYS.count(key) > 0)
    throw argument_error("Cannot access directly to " + key);

  auto it = _map.find(key);
  _map[key] = value;

  // Set default / ignore
  auto itA = type()->arguments().find(key);
  if (itA != type()->arguments().end()) {
    auto & argument = *itA->second;
    if (argument.defaultValue() && argument.defaultValue()->equals(*value)) {
      LOGGER->debug("Value is default");
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
// --- Task
// ---


Argument::Argument(std::string const &name) : _name(name), _type(AnyType), _required(true), _ignored(false), _generator(nullptr) {
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


const std::string &Argument::help() const {
  return _help;
}
Argument &Argument::help(const std::string &help) {
  _help = help;
  return *this;
}

Argument &Argument::defaultValue(ptr<Parameters> const &defaultValue) {
  _defaultValue = defaultValue;
  _required = false;
  return *this;
}
ptr<Parameters> Argument::defaultValue() const { return _defaultValue; }

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

GeneratorContext::GeneratorContext(Workspace & ws, ptr<Parameters> const &sv) : workspace(ws) {
  stack.push_back(sv.get());
}
GeneratorContext::GeneratorContext(Workspace & ws) : workspace(ws) {
}

GeneratorLock::GeneratorLock(GeneratorContext * context, Parameters *sv) : context(context) {
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

ptr<Parameters> PathGenerator::generate(GeneratorContext const &context) {
  Path p = context.workspace.jobsdir();
  auto uuid = context.stack[0]->uniqueIdentifier();

  if (ptr<Task> task = context.stack[0]->task()) {
    p = Path(p, {task->identifier().toString()});
  }

  p = Path(p, {uuid});

  if (!_name.empty()) {
    p = Path(p, { _name });
  }
  return mkptr<Parameters>(Value(p));
}

PathGenerator::PathGenerator(std::string const &name) : _name(name) {

}

} // xpm namespace
