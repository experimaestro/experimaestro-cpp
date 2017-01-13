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

template<>
struct Reference<CommandString> : public Reference<AbstractCommandComponent> {
  std::string value;
  Reference(const std::string &value) : value(value) {}
  virtual std::shared_ptr<rpc::AbstractCommandComponent> rpc(CommandContext &context) const override {
    return std::make_shared<rpc::CommandString>(value);
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
nlohmann::json CommandString::toJson() const {
  return nlohmann::json();
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
  return nullptr;
}

template<>
struct Reference<CommandContent> : public Reference<AbstractCommandComponent> {
  std::string key;
  std::string content;
  Reference(std::string const &key, std::string const &content) : key(key), content(content) {}
  virtual std::shared_ptr<rpc::AbstractCommandComponent> rpc(CommandContext &context) const override {
    return std::make_shared<rpc::ParameterFile>(key, content);
  }
};

CommandContent::CommandContent(std::string const &key, std::string const &value) : PimplChild(key, value) {
}
std::string CommandContent::toString() const {
  return std::string();
}
CommandContent::~CommandContent() {

}
nlohmann::json CommandContent::toJson() const {
  auto j = nlohmann::json::object();
  j["type"] = "content";
  j["key"] = self(this).key;
  j["content"] = self(this).content;
  return j;
}

template<>
struct Reference<CommandParameters> : public Reference<AbstractCommandComponent> {
  virtual std::shared_ptr<rpc::AbstractCommandComponent> rpc(CommandContext &context) const override {
    return std::make_shared<rpc::ParameterFile>("params.json", context.parameters);
  }

};

CommandParameters::~CommandParameters() {

}
CommandParameters::CommandParameters() {
}
nlohmann::json CommandParameters::toJson() const {
  return { {"type", "parameters"} };
}

template<>
struct Reference<CommandPath> : public Reference<AbstractCommandComponent> {
  Path path;
  Reference(const Path &path) : path(path) {}
  virtual std::shared_ptr<rpc::AbstractCommandComponent> rpc(CommandContext &context) const override {
    return std::make_shared<rpc::CommandPath>(path.toString());
  }
};

CommandPath::CommandPath(Path path) : PimplChild(path) {

}
CommandPath::~CommandPath() {

}

std::string CommandPath::toString() const {
  return self(this).path.toString();
}

nlohmann::json CommandPath::toJson() const {
  auto j = nlohmann::json::object();
  j["type"] = "path";
  j["path"] = self(this).path.toString();
  return j;
}
}
