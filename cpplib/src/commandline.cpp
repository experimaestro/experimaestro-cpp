//
// Created by Benjamin Piwowarski on 09/12/2016.
//

#include <string>
#include <xpm/json.hpp>
#include "include/xpm/commandline.hpp"

namespace xpm {

template<> struct Reference<AbstractCommandComponent> {
  virtual ~Reference<AbstractCommandComponent>() {}
};

CommandLine::CommandLine() {

}
std::shared_ptr<rpc::AbstractCommand> CommandLine::rpc() {
  std::shared_ptr<rpc::Command> command = std::make_shared<rpc::Command>();

  return command;
}

template<> struct Reference<CommandString> : public Reference<AbstractCommandComponent> {
  std::string value;
  Reference(const std::string &value) : value(value) {}
};

CommandString::CommandString(const std::string &value)
  : PimplChild(value) {
}

std::string CommandString::toString() const {
  return self(this).value;
}

void Command::add(AbstractCommandComponent component) {
  components.push_back(component);
}
AbstractCommandComponent::AbstractCommandComponent() {

}


template<> struct Reference<CommandContent> : public Reference<AbstractCommandComponent> {
  std::string value;
  Reference(const std::string &value) : value(value) {}
};

CommandContent::CommandContent(const std::string &value) : PimplChild(value) {
}
std::string CommandContent::toString() const {
  return std::string();
}
}