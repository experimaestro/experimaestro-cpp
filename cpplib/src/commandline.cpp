//
// Created by Benjamin Piwowarski on 09/12/2016.
//

#include <string>
#include <xpm/json.hpp>
#include "include/xpm/commandline.hpp"

namespace xpm {

template<>
struct Reference<AbstractCommandComponent> {
  virtual ~Reference<AbstractCommandComponent>() {}
};

CommandLine::CommandLine() {

}
std::shared_ptr<rpc::AbstractCommand> CommandLine::rpc(CommandContext &context) {
  std::shared_ptr<rpc::Commands> rpcCommand = std::make_shared<rpc::Commands>();
  for (auto &command: commands) {
    rpcCommand->add(command.rpc(context));
  }
  return rpcCommand;
}

template<>
struct Reference<CommandString> : public Reference<AbstractCommandComponent> {
  std::string value;
  Reference(const std::string &value) : value(value) {}
};

CommandString::CommandString(const std::string &value)
    : PimplChild(value) {
}

std::string CommandString::toString() const {
  return self(this).value;
}
std::shared_ptr<rpc::AbstractCommandComponent> CommandString::rpc(CommandContext &context) {
  return std::make_shared<rpc::CommandString>(self(this).value);
}

CommandString::~CommandString() {

}

void Command::add(AbstractCommandComponent component) {
  components.push_back(component);
}

std::shared_ptr<rpc::Command> Command::rpc(CommandContext &context) {
  auto rpc = std::shared_ptr<rpc::Command>();
  for (auto &component: components) {
    rpc->add({component.rpc(context)});
  }
  return rpc;
}
AbstractCommandComponent::AbstractCommandComponent() {

}
std::shared_ptr<rpc::AbstractCommandComponent> AbstractCommandComponent::rpc(CommandContext &context) {
  throw std::runtime_error("AbstractCommandComponent::rpc is virtual");
}
AbstractCommandComponent::~AbstractCommandComponent() {

}

template<>
struct Reference<CommandContent> : public Reference<AbstractCommandComponent> {
  std::string key;
  std::string content;
  Reference(std::string const &key, std::string const &content) : key(key), content(content) {}
};

CommandContent::CommandContent(std::string const &key, std::string const &value) : PimplChild(key, value) {
}
std::string CommandContent::toString() const {
  return std::string();
}
std::shared_ptr<rpc::AbstractCommandComponent> CommandContent::rpc(CommandContext &context) {
  return std::make_shared<rpc::ParameterFile>(self(this).key, self(this).content);
}
CommandContent::~CommandContent() {

}



CommandParameters::~CommandParameters() {

}
std::shared_ptr<rpc::AbstractCommandComponent> CommandParameters::rpc(CommandContext &context) {
  return std::make_shared<rpc::ParameterFile>("params.json", context.parameters);
}

}
