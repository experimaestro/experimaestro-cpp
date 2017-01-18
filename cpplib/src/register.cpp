//
// Created by Benjamin Piwowarski on 14/01/2017.
//

#include <fstream>

#include <xpm/common.hpp>
#include <xpm/filesystem.hpp>
#include <xpm/register.hpp>

#include "private.hpp"

namespace xpm {

DEFINE_LOGGER("xpm");

using nlohmann::json;

Register::Register() {
  addType(IntegerType);
  addType(RealType);
  addType(StringType);
  addType(BooleanType);
  addType(PathType);
}
Register::~Register() {}

void Register::addType(std::shared_ptr<Type> const &type) {
  _types[type->typeName()] = type;
}

void Register::addTask(std::shared_ptr<Task> const &task) {
  _tasks[task->identifier()] = task;
}

std::shared_ptr<Task> Register::getTask(TypeName const &typeName) {
  auto it = _tasks.find(typeName);
  if (it != _tasks.end()) {
    return it->second;
  }
  return nullptr;
}

std::shared_ptr<Type> Register::getType(TypeName const &typeName) {
  auto it = _types.find(typeName);
  if (it != _types.end()) {
    return it->second;
  }
  return nullptr;
}

// Find a type given a type name
std::shared_ptr<Type> Register::getType(std::shared_ptr<Object> const &object) {
  return object->type();
}

std::shared_ptr<Object> Register::build(std::shared_ptr<Object> const &value) {
  std::shared_ptr<Type> objectType = value->type();
  LOGGER->debug("Building object with type {}", *objectType);

  // Create the object
  std::cerr << "Creating object..." << std::endl;
  auto object = objectType ? objectType->create() : std::make_shared<Object>();

  std::cerr << "Created object... " << object.get() << std::endl;
  object->setValue(value);

  if (!object) {
    throw std::runtime_error("Object of type " + objectType->toString() + " was not created");
  }

  // Loop over all the type hierarchy
  for (auto type = objectType; type; type = type->_parent) {

    // Loop over the arguments
    for (auto entry: type->arguments()) {
      auto key = entry.first;

      // Check required argument
      auto const hasKey = value->hasKey(key);
      if (!hasKey && entry.second->required()) {
        throw argument_error("Argument " + key + " was required but not provided");
      }

      // Build subtype
      if (hasKey) {
        std::cerr << "Building " << key << std::endl;
        auto subvalue = build(value->get(key));

        // Set argument
        std::cerr << "Setting " << key << std::endl;
        object->set(key, subvalue);
      } else {
        auto defaultValue = entry.second->defaultValue();
        if (defaultValue) {
          object->set(key, defaultValue->copy());
        }
      }
    }
  }

  return object;
}

void Register::parse(std::vector<std::string> const &args) {
  if (args.size() < 1) {
    throw argument_error("Expected at least one argument (use help to get some help)");
  }

  if (args[0] == "help") {
    std::cerr << "[Commands]\n";
    std::cerr << "   help" << std::endl;
    std::cerr << "   generate" << std::endl;
    std::cerr << "   run" << std::endl;

    std::cerr << std::endl;

    std::cerr << "[available tasks]\n";
    for (auto &entry: _tasks) {
      std::cerr << "   " << entry.first << std::endl;
    }
    std::cerr << std::endl;
    return;
  }

  if (args[0] == "generate") {
    generate();
    return;
  }

  if (args[0] == "run") {
    // Retrieve the task
    std::string taskName = args[1];
    auto task = this->getTask(TypeName(taskName));
    if (!task) {
      throw argument_error(taskName + " is not a task");

    }

    // Retrieve the structured value
    // TODO:
    // - process other arguments (SV)
    // - process command lines
    std::ifstream stream(args[2]);
    if (!stream) {
      throw argument_error(args[2] + " is not a file");
    }
    json j = json::parse(stream);

    auto value = task->create();
    value->fill(*this, j);

    // Run the task
    task->execute(value);

    return;

  }

  throw argument_error("Unexpected command: " + args[0]);
}
void Register::generate() const {
  std::cout << "{";
  std::cout << R"("types": [)" << std::endl;
  bool first = true;
  for (auto const &type: this->_types) {
      if (!type.second->predefined()) {
        if (!first) std::cout << ","; else first = false;
        std::cout << type.second->toJson() << std::endl;
      }
    }
  std::cout << "], " << std::endl;

  std::cout << R"("tasks": [)" << std::endl;
  first = true;
  for (auto const &type: this->_tasks) {
      std::cout << type.second->toJson() << std::endl;
    }
  std::cout << "]" << std::endl;

  std::cout << "}" << std::endl;
}

std::shared_ptr<Object> Register::build(std::string const &value) {
  return Object::createFromJson(*this, json::parse(value));
}

void Register::parse(int argc, const char **argv) {
  std::vector<std::string> args;
  for (int i = 1; i < argc; ++i) {
    args.emplace_back(std::string(argv[i]));
  }
  parse(args);
}

void Register::load(const std::string &value) {
  LOGGER->info("Loading XPM register file " + value);
  std::ifstream in(value);
  if (!in) {
    throw std::runtime_error("Register file " + value + " does not exist");
  }
  auto j = json::parse(in);
  load(j);
}

void Register::load(nlohmann::json const &j) {
  assert(j["types"].is_array());
  for (auto &e: j["types"]) {
    assert(e.is_object());
    const TypeName typeName = TypeName(e["type"].get<std::string>());
    auto typeIt = _types.find(typeName);
    Type::Ptr type;

    // Search for the type
    if (typeIt != _types.end()) {
      type = typeIt->second;
      LOGGER->debug("Using placeholder type {}", type->typeName().toString());
      if (!type->placeholder()) {
        throw std::runtime_error("Type " + type->typeName().toString() + " was already defined");
      }
      type->placeholder(false);
    } else {
      _types[typeName] = type = std::make_shared<Type>(typeName);
      type->objectFactory(_defaultObjectFactory);
    }

    // Get the parent type
    if (e.count("parent")) {
      auto parentTypeName = TypeName(e["parent"].get<std::string>());
      auto parentTypeIt = _types.find(parentTypeName);
      if (parentTypeIt == _types.end()) {
        LOGGER->debug("Creating placeholder type {} ", parentTypeName);
        auto parentType = std::make_shared<Type>(parentTypeName);
        type->parentType(parentType);
        parentType->placeholder(true);
        _types[parentTypeName] = parentType;
      } else {
        type->parentType(parentTypeIt->second);
      }
    }

    LOGGER->debug("Adding type {}", type->typeName());

    // Parse arguments
    auto arguments = e["arguments"];
    for (json::iterator it = arguments.begin(); it != arguments.end(); ++it) {
      LOGGER->debug("   Adding argument {}", it.key());
      auto a = std::make_shared<Argument>(it.key());
      const auto value = it.value();
      const auto valueTypename = value["type"].get<std::string>();
      auto valueType = getType(valueTypename);
      if (!valueType) {
        addType(valueType = std::make_shared<Type>(valueTypename));
        valueType->placeholder(true);
      }
      a->type(valueType);
      a->help(value["help"]);
      a->required(value["required"]);

      if (value.count("default")) {
        LOGGER->debug("    -> Found a default value");
        a->defaultValue(Object::createFromJson(*this, value["default"]));
      }

      if (value.count("generator")) {
        LOGGER->debug("    -> Found a generator");
        a->generator(Generator::createFromJSON(value["generator"]));
      }

      type->addArgument(a);
    }
    _types[type->typeName()] = type;
  }



  assert(j["tasks"].is_array());
  for (auto &e: j["tasks"]) {
    TypeName identifier(e["identifier"].get<std::string>());
    TypeName type(e["type"].get<std::string>());
    auto typePtr = getType(type);

    CommandLine commandLine;
    commandLine.load(e["command"]);
    auto task = std::make_shared<Task>(identifier, typePtr);
    task->commandline(commandLine);
    task->objectFactory(_defaultObjectFactory);
    addTask(task);
  }
}

void Register::load(Path const &value) {
  NOT_IMPLEMENTED();
}

void Register::objectFactory(std::shared_ptr<ObjectFactory> const &factory) {
  _defaultObjectFactory = factory;

}

std::shared_ptr<ObjectFactory> Register::objectFactory() {
  return _defaultObjectFactory;
}

}
