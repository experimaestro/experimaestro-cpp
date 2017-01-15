//
// Created by Benjamin Piwowarski on 09/12/2016.
//

#include <string>
#include <xpm/json.hpp>
#include <xpm/commandline.hpp>

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
  auto j = nlohmann::json::array();
  for(auto &command: commands) {
    j.push_back(command.toJson());
  }
  return j;
}
void CommandLine::load(nlohmann::json const &j) {
  assert(j.is_array());
  for(auto &e: j) {
    Command c;
    c.load(e);
    this->commands.push_back(c);
  }
}

template<>
struct Reference<CommandString> : public Reference<AbstractCommandComponent> {
  std::string value;
  Reference(const std::string &value) : value(value) {}
  virtual std::shared_ptr<rpc::AbstractCommandComponent> rpc(CommandContext &context) const override {
    return std::make_shared<rpc::CommandString>(value);
  }
  virtual nlohmann::json toJson() const override {
    return value;
  }
};

CommandString::CommandString(const std::string &value)
    : PimplChild(value) {
}

std::string CommandString::toString() const {
  return self(this).value;
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
      std::string type = e["type"];
      if (type == "content") {
        components.push_back(CommandContent(e["key"], e["content"]));
      } else if (type == "parameters") {
        components.push_back(CommandParameters());
      } else if (type == "path") {
        components.push_back(CommandPath(Path(e["path"].get<std::string>())));
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
  return self(this).rpc(context);
}
AbstractCommandComponent::~AbstractCommandComponent() {

}
nlohmann::json AbstractCommandComponent::toJson() const {
  return self(this).toJson();
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


template<>
struct Reference<CommandParameters> : public Reference<AbstractCommandComponent> {
  virtual std::shared_ptr<rpc::AbstractCommandComponent> rpc(CommandContext &context) const override {
    return std::make_shared<rpc::ParameterFile>("params.json", context.parameters);
  }
  virtual nlohmann::json toJson() const override {
    return { {"type", "parameters"} };
  }
};

CommandParameters::~CommandParameters() {

}
CommandParameters::CommandParameters() {
}

template<>
struct Reference<CommandPath> : public Reference<AbstractCommandComponent> {
  Path path;
  Reference(const Path &path) : path(path) {}
  virtual std::shared_ptr<rpc::AbstractCommandComponent> rpc(CommandContext &context) const override {
    return std::make_shared<rpc::CommandPath>(path.toString());
  }
  virtual nlohmann::json toJson() const override {
    auto j = nlohmann::json::object();
    j["type"] = "path";
    j["path"] = path.toString();
    return j;
  }
};

CommandPath::CommandPath(Path path) : PimplChild(path) {

}
CommandPath::~CommandPath() {

}

std::string CommandPath::toString() const {
  return self(this).path.toString();
}
void CommandPath::path(Path path) {
  self(this).path = path;
}

}
