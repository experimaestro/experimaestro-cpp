//
// Created by Benjamin Piwowarski on 09/12/2016.
//

#include <string>
#include <regex>
#include <xpm/json.hpp>
#include <xpm/commandline.hpp>
#include <xpm/xpm.hpp>
#include <xpm/value.hpp>
#include <xpm/context.hpp>

#include "private.hpp"

DEFINE_LOGGER("xpm")

namespace xpm {

template<>
struct Reference<AbstractCommandComponent> {
  virtual ~Reference<AbstractCommandComponent>() {}
  virtual std::shared_ptr<rpc::AbstractCommandComponent> rpc(CommandContext &context) const {
    throw std::runtime_error("Pure virtual function for " + std::string(typeid(*this).name()));
  }
  virtual nlohmann::json toJson() const {
    throw std::runtime_error("Pure virtual function for " + std::string(typeid(*this).name()));
  }
};

CommandLine::CommandLine() {

}
std::shared_ptr<rpc::AbstractCommand> CommandLine::rpc(CommandContext &context) const {
  std::shared_ptr<rpc::Commands> rpcCommand = std::make_shared<rpc::Commands>();
  for (auto &command: commands) {
    rpcCommand->add(command.rpc(context));
  }
  return rpcCommand;
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
  virtual std::shared_ptr<rpc::AbstractCommandComponent> rpc(CommandContext &context) const override {
    return std::make_shared<rpc::CommandString>(transform(Context::current(), value));
  }
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

std::shared_ptr<rpc::Command> Command::rpc(CommandContext &context) const {
  auto rpc = std::make_shared<rpc::Command>();
  for (auto &component: components) {
    rpc->add({component.rpc(context)});
  }
  return rpc;
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
std::shared_ptr<rpc::AbstractCommandComponent> AbstractCommandComponent::rpc(CommandContext &context) const {
  return self().rpc(context);
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
  virtual std::shared_ptr<rpc::AbstractCommandComponent> rpc(CommandContext &context) const override {
    return std::make_shared<rpc::ParameterFile>(key, content);
  }

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
  void fill(rpc::ContentsFile &f, std::ostringstream &oss, std::shared_ptr<Object> const & object) {
    if (auto value = dynamic_cast<Value*>(object.get())) {
      if (value->type() == xpm::PathType) {
        oss << "{\"" << xpm::KEY_TYPE << "\":\"" << xpm::PathType->typeName().toString() << "\",\""
            << xpm::KEY_VALUE << "\": \"";
        f.add(oss.str());
        oss.str("");
        f.add(rpc::Path::toPath(value->asPath().toString()));
        oss << "\"}";
      } else {
        oss << value->jsonValue();
      }
    } else {
      oss << "{";
      bool first = true;
      if (object->type()) {
        oss << "\"" << KEY_TYPE << "\": \"" << object->type()->typeName() << "\"";
        first = false;
      }
      for(auto &entry: object->content()) {
        if (first) first = false;
        else oss << ',';
        oss << "\"" << entry.first << "\":";
        fill(f, oss, entry.second);
      }
      oss << "}";
    }
  }
}

template<>
struct Reference<CommandParameters> : public Reference<AbstractCommandComponent> {
  virtual std::shared_ptr<rpc::AbstractCommandComponent> rpc(CommandContext &context) const override {
    auto r = std::make_shared<rpc::ContentsFile>("params", ".json");
    std::ostringstream oss;
    fill(*r, oss, context.parameters);
    std::string s = oss.str();
    if (!s.empty()) {
      r->add(oss.str());
    }

    return r;
  }
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
  virtual std::shared_ptr<rpc::AbstractCommandComponent> rpc(CommandContext &commandContext) const override {
    auto context = Context::current();
    if (!context.has(key)) {
      throw std::invalid_argument("Context has no variable named [" + key + "]");
    }

    LOGGER->debug("Path ref {} is {}", key, context.get(key));
    return std::make_shared<rpc::CommandPath>(context.get(key));
  }
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
  virtual std::shared_ptr<rpc::AbstractCommandComponent> rpc(CommandContext &context) const override {
    return std::make_shared<rpc::CommandPath>(path.toString());
  }
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
