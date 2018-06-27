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
#include <xpm/workspace.hpp>
#include <xpm/type.hpp>

#include <xpm/connectors/connectors.hpp>

#include <__xpm/scriptbuilder.hpp>
#include <__xpm/common.hpp>

DEFINE_LOGGER("xpm")

namespace xpm {

// ---- Command part

void CommandPart::forEach(std::function<void(CommandPart &)> f) {
  f(*this);
}


// ---- Abstract command

AbstractCommandComponent::AbstractCommandComponent() {

}
AbstractCommandComponent::~AbstractCommandComponent() {

}

// --- Command string

namespace {
std::string transform(Workspace &context, std::string const &value) {
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

void CommandString::output(CommandContext & context, std::ostream & out) const {
  out << transform(context.workspace, value);
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

void CommandContent::output(CommandContext &context, std::ostream & out) const {
  auto path = context.getAuxiliaryFile(key, ".input");
  auto fileOut = context.connector.ostream(path);
  *fileOut << content;
  out << context.connector.resolve(path);
}


// --- CommandParameters


namespace {
  /// Generates the JSON that will be used to configure the task
  void fill(CommandContext & context, std::ostream & out, ptr<Parameters> const & conf) {
    if (conf->hasValue()) {
      // The object has one value, just use this and discards the rest
      switch(conf->valueType()) {
        case ValueType::ARRAY: {
          out << "{\"" << xpm::KEY_TYPE << "\":\"" << conf->type()->name().toString() << "\",\""
              << xpm::KEY_VALUE << "\": [";
          for(size_t i = 0; i < conf->size(); ++i) {
            if (i > 0) out << ", ";
            fill(context, out, (*conf)[i]);
          }
          out << "]}";
          break;
        }

        case ValueType::PATH:
          out << "{\"" << xpm::KEY_TYPE << "\":\"" << xpm::PathType->name().toString() << "\",\""
              << xpm::KEY_VALUE << "\": \"";
          out << context.connector.resolve(conf->asPath());
          out << "\"}";
          break;

        default:
         out << conf->valueAsJson();
         break;
      }
    } else {
      // No simple value: output the structure

      out << "{";
      bool first = true;

      auto comma = [&first,&out] {        
        if (first) first = false;
        else out << ',';
      };

      if (conf->type()) {
        out << "\"" << KEY_TYPE << "\": \"" << conf->type()->name() << "\"";
        first = false;
      }

      if (conf->job()) {
        comma();
        out << "\"$job\": " <<  conf->job()->toJson() << std::endl;
      }

      for (auto type = conf->type(); type; type = type->parentType()) {
        for (auto entry: type->arguments()) {
          Argument &argument = *entry.second;
          comma();
          out << "\"" << entry.first << "\":";
          
          if (conf->hasKey(argument.name())) {
            fill(context, out, conf->get(argument.name()));
          } else {
            out << "null";
          }
        }
      }

      out << "}";
    }
  }
} // end unnamed ns

void CommandParameters::output(CommandContext &context, std::ostream & out) const {
  auto path = context.getAuxiliaryFile("params", ".json");
  auto fileOut = context.connector.ostream(path);
  fill(context, *fileOut, context.parameters);
  out << context.connector.resolve(path);
}

CommandParameters::~CommandParameters() {

}
CommandParameters::CommandParameters() {
}

nlohmann::json CommandParameters::toJson() const {
  return { {"type", "parameters"} };
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

void CommandPathReference::output(CommandContext & context, std::ostream & out) const {
  auto & ws = context.workspace;
  if (!ws.has(key)) {
    throw std::invalid_argument("Context has no variable named [" + key + "]");
  }

  auto value = ws.get(key);
  LOGGER->debug("Path ref {} is {}", key, value);
  out << value;
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

void CommandPath::output(CommandContext & context, std::ostream & out) const {
  out << context.connector.resolve(_path);
}

// --- Abstract command

void AbstractCommand::output(CommandContext & context, std::ostream & out) const {
    std::vector<ptr<AbstractCommand>> list = reorder();

  int detached = 0;

  if (list.size() > 1) {
    out << "(" << std::endl;
  }

  for (auto & command : list) {

    // Write files
    NamedPipeRedirections &namedRedirections = context.getNamedRedirections(*command, false);

    // Write named pipes
    auto mkfifo = [&](Path const &file) {
      out << " mkfifo \""
          << ShScriptBuilder::protect_quoted(context.connector.resolve(file, context.getWorkingDirectory()))
          << "\"" << std::endl;
    };

    for (auto & file : namedRedirections.outputRedirections) {
      mkfifo(file);
    }
    for (auto & file : namedRedirections.errorRedirections) {
      mkfifo(file);
    }


    if (command->inputRedirect.type == Redirection::FILE) {
      out << " cat \""
          << ShScriptBuilder::protect_quoted(context.connector.resolve(inputRedirect.path,
                                        context.getWorkingDirectory()))
          << "\" | ";
    }

    command->output(context, out);

    context.printRedirections(1, out, command->outputRedirect,
                      namedRedirections.outputRedirections);
    context.printRedirections(2, out, command->errorRedirect,
                      namedRedirections.errorRedirections);
    out << " || checkerror \"${PIPESTATUS[@]}\" ";

    // if (env.detached(command)) {
    //   // Just keep a pointer
    //   out << " & CHILD_" << detached << "=$!" << std::endl;
    //   detached++;
    // } else {
      // Stop if an error occurred
      out << " || exit $?" << std::endl;
    // }
  }

  // Monitors detached jobs
  for (int i = 0; i < detached; i++) {
    out << "wait $CHILD_" << i << " || exit $?%" << std::endl;
  }

  if (list.size() > 1) {
    out << ")" << std::endl;
  }
}




// --- Command

Command::~Command() {}

void Command::add(ptr<AbstractCommandComponent> const & component) {
  components.push_back(component);
}

void Command::forEach(std::function<void(CommandPart &)> f) {
  CommandPart::forEach(f);
  for(auto & c : components) {
    c->forEach(f);
  }
}

void Command::output(CommandContext & context, std::ostream & out) const {
  bool first = true;
  for(auto & c : components) {
    if (first) first = false;
    else out << " ";
    c->output(context, out);
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

std::vector<ptr<AbstractCommand>> Command::reorder() const {
  std::vector<ptr<AbstractCommand>> list;
  auto ptr = std::static_pointer_cast<AbstractCommand>(const_cast<Command*>(this)->shared_from_this());
  list.push_back(ptr);
  return list;
}


nlohmann::json Command::toJson() const {
  auto j = nlohmann::json::array();
  for(auto &component: this->components) {
    j.push_back(component->toJson());
  }
  return j;
}


// --- Command line

CommandLine::CommandLine() {
}

CommandLine::~CommandLine() {}

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

std::vector<ptr<AbstractCommand>> CommandLine::reorder() const {
  return commands;
}



// --- Command context


namespace {
  NamedPipeRedirections EMPTY_REDIRECTIONS;
}

CommandContext::CommandContext(Workspace & workspace, Connector const & connector, Path const &folder, std::string const &name)
      : workspace(workspace), connector(connector), folder(folder), name(name) {}


Path CommandContext::getWorkingDirectory() { return folder; }

void CommandContext::writeRedirection(std::ostream &out, Redirect const &redirect, int stream) {
  switch (redirect.type) {
    case Redirection::INHERIT:
      break;
    case Redirection::FILE:
      out << " " 
          << stream << "> " 
          << ShScriptBuilder::protect_quoted(connector.resolve(redirect.path, getWorkingDirectory()));
      break;
    default:
      throw exception("Unsupported output redirection type");
  }
}

void CommandContext::printRedirections(int stream, std::ostream &out,
    Redirect const &outputRedirect, std::vector<Path> const &outputRedirects) {
  if (!outputRedirects.empty()) {

    // Special case : just one redirection
    if (outputRedirects.size() == 1 && outputRedirect.type == Redirection::INHERIT) {
      writeRedirection(out, Redirect::file(outputRedirects[0].toString()), stream);
    } else {
      out << " : " << stream << "> >(tee";
      for (Path file : outputRedirects) {
        out << " \"" <<  ShScriptBuilder::protect_quoted(connector.resolve(file, getWorkingDirectory())) << "\"";
      }
      writeRedirection(out, outputRedirect, stream);
      out << ")";
    }
  } else {
    // Finally, write the main redirection
    writeRedirection(out, outputRedirect, stream);
  }
}

NamedPipeRedirections &CommandContext::getNamedRedirections(CommandPart const & key,
  bool create) {
    auto x = namedPipeRedirectionsMap.find(&key);
    if (x != namedPipeRedirectionsMap.end()) {
      return x->second;
    }

    if (!create)
      return EMPTY_REDIRECTIONS;

    return namedPipeRedirectionsMap[&key] = NamedPipeRedirections();
}

Path CommandContext::getAuxiliaryFile(std::string const & prefix, std::string const & suffix) {
    std::string reference = name + "." + prefix + suffix;
    int &count = ++counts[reference];
    return folder.resolve(
        {fmt::format("{}_{:02d}.{}{}", name, count, prefix, suffix)});
}


} // end xpm ns
