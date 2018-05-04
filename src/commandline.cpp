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


void CommandPart::addDependency(Job & job) {
}

void CommandPart::forEach(std::function<void(CommandPart &)> f) {
  f(*this);
}


// ---- Abstract command

AbstractCommandComponent::AbstractCommandComponent() {

}
AbstractCommandComponent::~AbstractCommandComponent() {

}
nlohmann::json AbstractCommandComponent::toJson() const {
  return toJson();
}


// --- Command line

CommandLine::CommandLine() {

}

void CommandLine::add(ptr<Command> const & command) {
  commands.push_back(command);
}

nlohmann::json CommandLine::toJson() const {
  if (commands.size() == 1) {
    return commands[0]->toJson();
  }

  auto j = nlohmann::json::array();
  for(auto &command: commands) {
    j.push_back(command->toJson());
  }
  return j;
}
void CommandLine::load(nlohmann::json const &j) {
  assert(j.is_array());

  if (!j.empty() && !j[0].is_array()) {
    // Simplified array
    auto c = mkptr<Command>();
    c->load(j);
    this->commands.push_back(c);
  } else {
    for (auto &e: j) {
      auto c = mkptr<Command>();
      c->load(e);
      this->commands.push_back(c);
    }
  }
}

void CommandLine::forEach(std::function<void(CommandPart &)> f) {
  CommandPart::forEach(f);
  for(auto & c : commands) {
    c->forEach(f);
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

nlohmann::json CommandString::toJson() const {
  return value;
}

CommandString::CommandString(const std::string &value)
    : value(value) {
}

std::string CommandString::toString() const {
  return value;
}

CommandString::~CommandString() {

}

void Command::add(ptr<AbstractCommandComponent> const & component) {
  components.push_back(component);
}

void Command::forEach(std::function<void(CommandPart &)> f) {
  CommandPart::forEach(f);
  for(auto & c : components) {
    c->forEach(f);
  }
}


void Command::load(nlohmann::json const &j) {
  assert(j.is_array());
  for(auto &e: j) {
    if (e.is_string()) {
      components.push_back(mkptr<CommandString>(e.get<std::string>()));
    } else {
      std::string type = e.value("type", "");
      if (type == "content") {
        components.push_back(mkptr<CommandContent>(e["key"], e["content"]));
      } else if (type == "parameters") {
        components.push_back(mkptr<CommandParameters>());
      } else if (type == "path" || (type == "" && e.count("path"))) {
        components.push_back(mkptr<CommandPath>(Path(e["path"].get<std::string>())));
      } else if (type == "pathref" || (type == "" && e.count("pathref"))) {
        components.push_back(mkptr<CommandPathReference>(CommandPathReference(e["pathref"].get<std::string>())));
      } else {
        throw std::invalid_argument("Unknown type for command component: " + type);
      }
    }
  }
}

nlohmann::json Command::toJson() const {
  auto j = nlohmann::json::array();
  for(auto &component: this->components) {
    j.push_back(component->toJson());
  }
  return j;
}



// ---- Command content

CommandContent::CommandContent(std::string const &key, std::string const &value) : key(key), content(value) {
}

nlohmann::json CommandContent::toJson() const {
    auto j = nlohmann::json::object();
    j["type"] = "content";
    j["key"] = key;
    j["content"] = content;
    return j;
  }

std::string CommandContent::toString() const {
  return std::string();
}

CommandContent::~CommandContent() {
}



// --- CommandParameters


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
} // end unnamed ns


CommandParameters::~CommandParameters() {

}
CommandParameters::CommandParameters() {
}

nlohmann::json CommandParameters::toJson() const {
  return { {"type", "parameters"} };
}

void CommandParameters::setValue(ptr<StructuredValue> const & value) {
  this->value = value;
}

void CommandParameters::addDependencies(Job & job) {
  if (!this->value) throw exception("Cannot set dependencies since value is null");

  this->value->addDependencies(job, false);
}



// --- CommandPathReference

CommandPathReference::CommandPathReference(std::string const &key) : key(key) {

}
CommandPathReference::~CommandPathReference() {
}

nlohmann::json CommandPathReference::toJson() const {
  auto j = nlohmann::json::object();
  j["pathref"] = key;
  return j;
}

std::string CommandPathReference::toString() const {
  return "pathref(" + key + ")";
}

// --- CommandPath

CommandPath::CommandPath(Path path) : _path(path) {
}

CommandPath::~CommandPath() {
}

nlohmann::json CommandPath::toJson() const {
  auto j = nlohmann::json::object();
  j["path"] = _path.toString();
  return j;
}

std::string CommandPath::toString() const {
  return _path.toString();
}
void CommandPath::path(Path path) {
  path = path;
}

namespace {
  NamedPipeRedirections EMPTY_REDIRECTIONS;
}

NamedPipeRedirections &CommandContext::getNamedRedirections(CommandPart & key,
  bool create) {
    auto x = namedPipeRedirectionsMap.find(key);
    if (x != namedPipeRedirectionsMap.end()) {
      return x->second;
    }

    if (!create)
      return EMPTY_REDIRECTIONS;

    return namedPipeRedirectionsMap[key] = NamedPipeRedirections();
}

} // end xpm ns
