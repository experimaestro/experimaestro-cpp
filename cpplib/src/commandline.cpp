//
// Created by Benjamin Piwowarski on 09/12/2016.
//

#include <string>
#include <regex>
#include <xpm/json.hpp>
#include <xpm/commandline.hpp>
#include <xpm/xpm.hpp>
#include <xpm/value.hpp>
#include <xpm/common.hpp>
#include <xpm/context.hpp>

#include <__xpm/common.hpp>

DEFINE_LOGGER("xpm")

namespace xpm {

template<>
struct Reference<AbstractCommandComponent> {
  virtual ~Reference<AbstractCommandComponent>() {}
  virtual nlohmann::json toJson() const {
    throw std::runtime_error("Pure virtual function for " + std::string(typeid(*this).name()));
  }
};

CommandLine::CommandLine() {

}

void CommandLine::add(Command command) {
  commands.push_back(command);
}

nlohmann::json CommandLine::toJson() const {
  if (commands.size() == 1) {
    return commands[0].toJson();
  }
  auto j = nlohmann::json::array();
  for(auto &command: commands) {
    j.push_back(command.toJson());
  }
  return j;
}
void CommandLine::load(nlohmann::json const &j) {
  assert(j.is_array());

  if (!j.empty() && !j[0].is_array()) {
    // Simplified array
    Command c;
    c.load(j);
    this->commands.push_back(c);
  } else {
    for (auto &e: j) {
      Command c;
      c.load(e);
      this->commands.push_back(c);
    }
  }
}

namespace {
std::string transform(Context &context, std::string const &value) {
  static std::regex re(R"(\{\{((?:(?!\}\}).)+)\}\})");
  std::ostringstream out;
  std::sregex_iterator
      it(value.begin(), value.end(), re),
      end;
  size_t lastpos = 0;

  do {
    std::smatch match = *it;
    out << value.substr(lastpos, match.position() - lastpos)
        << context.get(match[1].str());
    lastpos = match.position() + match.length();
  } while (++it != end);

  out << value.substr(lastpos);

  std::string tvalue = out.str();
  LOGGER->debug("Transformed {} into {}", value, tvalue);
  return tvalue;

}
}

template<>
struct Reference<CommandString> : public Reference<AbstractCommandComponent> {
  std::string value;
  Reference(const std::string &value) : value(value) {}
  virtual nlohmann::json toJson() const override {
    return value;
  }
};

CommandString::CommandString(const std::string &value)
    : PimplChild(value) {
}

std::string CommandString::toString() const {
  return self().value;
}

CommandString::~CommandString() {

}

void Command::add(AbstractCommandComponent component) {
  components.push_back(component);
}


void Command::load(nlohmann::json const &j) {
  assert(j.is_array());
  for(auto &e: j) {
    if (e.is_string()) {
      components.push_back(CommandString(e.get<std::string>()));
    } else {
      std::string type = e.value("type", "");
      if (type == "content") {
        components.push_back(CommandContent(e["key"], e["content"]));
      } else if (type == "parameters") {
        components.push_back(CommandParameters());
      } else if (type == "path" || (type == "" && e.count("path"))) {
        components.push_back(CommandPath(Path(e["path"].get<std::string>())));
      } else if (type == "pathref" || (type == "" && e.count("pathref"))) {
        components.push_back(CommandPathReference(CommandPathReference(e["pathref"].get<std::string>())));
      } else {
        throw std::invalid_argument("Unknown type for command component: " + type);
      }
    }
  }
}

nlohmann::json Command::toJson() const {
  auto j = nlohmann::json::array();
  for(auto &component: this->components) {
    j.push_back(component.toJson());
  }
  return j;
}
AbstractCommandComponent::AbstractCommandComponent() {

}
AbstractCommandComponent::~AbstractCommandComponent() {

}
nlohmann::json AbstractCommandComponent::toJson() const {
  return self().toJson();
}

template<>
struct Reference<CommandContent> : public Reference<AbstractCommandComponent> {
  std::string key;
  std::string content;
  Reference(std::string const &key, std::string const &content) : key(key), content(content) {}

  virtual nlohmann::json toJson() const override {
    auto j = nlohmann::json::object();
    j["type"] = "content";
    j["key"] = key;
    j["content"] = content;
    return j;
  }
};

CommandContent::CommandContent(std::string const &key, std::string const &value) : PimplChild(key, value) {
}

std::string CommandContent::toString() const {
  return std::string();
}

CommandContent::~CommandContent() {
}


namespace {
  /// Generates the JSON that will be used to configure the task
  void fill(std::ostringstream &oss, std::shared_ptr<StructuredValue> const & conf) {
    if (conf->value().defined()) {
      // The object has one value, just use this and discards the rest
      switch(conf->value().scalarType()) {
        case ValueType::ARRAY: {
          auto & array = conf->value();
          oss << "{\"" << xpm::KEY_TYPE << "\":\"" << conf->type()->typeName().toString() << "\",\""
              << xpm::KEY_VALUE << "\": [";
          for(size_t i = 0; i < array.size(); ++i) {
            if (i > 0) oss << ", ";
          // FIXME: not implemented
            NOT_IMPLEMENTED();
            // fill(f, oss, array[i]);
          }
          oss << "]}";
          break;
        }

        case ValueType::PATH:
          oss << "{\"" << xpm::KEY_TYPE << "\":\"" << xpm::PathType->typeName().toString() << "\",\""
              << xpm::KEY_VALUE << "\": \"";
          // FIXME: not implemented
          NOT_IMPLEMENTED();
          // f.add(oss.str());
          oss.str("");
          // f.add(rpc::Path::toPath(conf->value().asPath().toString()));
          oss << "\"}";
          break;

        default:
         oss << conf->value().toJson();
         break;
      }
    } else {
      // No simple value: output the structure

      oss << "{";
      bool first = true;
      if (conf->type()) {
        oss << "\"" << KEY_TYPE << "\": \"" << conf->type()->typeName() << "\"";
        first = false;
      }

      for (auto type = conf->type(); type; type = type->parentType()) {
        for (auto entry: type->arguments()) {
          Argument &argument = *entry.second;
          if (first) first = false;
          else oss << ',';
          oss << "\"" << entry.first << "\":";
          
          if (conf->hasKey(argument.name())) {
            // FIXME: not implemented
            NOT_IMPLEMENTED();
            // fill(f, oss, conf->get(argument.name()));
          } else {
            oss << "null";
          }
        }
      }

      oss << "}";
    }
  }
}

template<>
struct Reference<CommandParameters> : public Reference<AbstractCommandComponent> {
  virtual nlohmann::json toJson() const override {
    return { {"type", "parameters"} };
  }
};

CommandParameters::~CommandParameters() {

}
CommandParameters::CommandParameters() {
}


// --- CommandPathReference

template<>
struct Reference<CommandPathReference> : public Reference<AbstractCommandComponent> {
  std::string key;
  Reference(const std::string &key) : key(key) {}

  virtual nlohmann::json toJson() const override {
    auto j = nlohmann::json::object();
    j["pathref"] = key;
    return j;
  }
};

CommandPathReference::CommandPathReference(std::string const &key) : PimplChild(key) {

}
CommandPathReference::~CommandPathReference() {
}

std::string CommandPathReference::toString() const {
  return "pathref(" + self().key + ")";
}



// --- CommandPath

template<>
struct Reference<CommandPath> : public Reference<AbstractCommandComponent> {
  Path path;
  Reference(const Path &path) : path(path) {}
  virtual nlohmann::json toJson() const override {
    auto j = nlohmann::json::object();
    j["path"] = path.toString();
    return j;
  }
};

CommandPath::CommandPath(Path path) : PimplChild(path) {

}
CommandPath::~CommandPath() {
}

std::string CommandPath::toString() const {
  return self().path.toString();
}
void CommandPath::path(Path path) {
  self().path = path;
}

}
