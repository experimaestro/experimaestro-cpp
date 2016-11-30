#ifndef _XPM_RPCOBJECTS_H
#define _XPM_RPCOBJECTS_H

#include <vector>
#include <xpm/rpc/utils.hpp>

namespace xpm {


// Pre-declaration
class Dependency;
class LauncherParameters;
class Json;
class SSHConnector;
class ObjectPropertyReference;
class JavaTaskFactory;
class LocalhostConnector;
class ReadWriteDependency;
class JsonNull;
class DirectLauncher;
class ScriptingLogger;
class SubCommand;
class Module;
class Connector;
class JsonResource;
class JsonBoolean;
class CommandOutput;
class ScriptingMap;
class Namespace;
class ParameterFile;
class ConnectorOptions;
class JsonTask;
class Task;
class JsonParameterFile;
class Job;
class Command;
class SSHOptions;
class Path;
class JsonObject;
class Resource;
class Pipe;
class JsonReal;
class OARLauncher;
class JsonArray;
class ScriptingList;
class JsonPath;
class XPM;
class OARParameters;
class Launcher;
class Tasks;
class TokenResource;
class SingleHostConnector;
class JsonString;


// Classes
class Connector : public ServerObject {
public:
  std::shared_ptr<Launcher> default_launcher();
  static std::shared_ptr<Connector> create(std::string const &string, std::string const &string_1, std::shared_ptr<ConnectorOptions> const &connectorOptions);
};

class SingleHostConnector : public Connector {
public:
};

class Dependency : public ServerObject {
public:
};

class Launcher : public ServerObject {
public:
  std::string environment(std::string const &key);
  void set_notification_url(std::string const &string);
  std::string env(std::string const &string);
  std::string env(std::string const &key, std::string const &value);
  virtual std::shared_ptr<LauncherParameters> parameters();
};

class LauncherParameters : public ServerObject {
public:
};

class Resource : public ServerObject {
public:
  std::string toString();
};

class Json : public ServerObject {
public:
  bool is_array();
  std::string get_descriptor();
  std::string toSource();
  std::shared_ptr<ParameterFile> as_parameter_file(std::string const &string, std::shared_ptr<SingleHostConnector> const &singleHostConnector);
  bool isSimple();
  std::shared_ptr<JsonObject> as_object();
  std::shared_ptr<Json> copy(bool const &boolean);
  std::shared_ptr<Json> copy();
  bool is_object();
};

class ConnectorOptions : public ServerObject {
public:
};

class LocalhostConnector : public SingleHostConnector {
public:
  std::string env(std::string const &string);
};

class JsonResource : public Json {
public:
};

class JsonParameterFile : public ServerObject {
public:
  JsonParameterFile(std::string const &string, std::shared_ptr<Json> const &json);
};

class ScriptingList : public ServerObject {
public:
};

class JsonBoolean : public Json {
public:
};

class ParameterFile : public ServerObject {
public:
};

class CommandOutput : public ServerObject {
public:
};

class JsonString : public Json {
public:
  std::string toString();
};

class OARParameters : public LauncherParameters {
public:
};

class SubCommand : public ServerObject {
public:
};

class ScriptingMap : public ServerObject {
public:
};

class Tasks : public ServerObject {
public:
  Tasks();
};

class SSHOptions : public ConnectorOptions {
public:
  SSHOptions();
  std::string hostname();
  void hostname(std::string const &string);
  void set_stream_proxy(std::string const &uri, std::shared_ptr<SSHOptions> const &options);
  void set_stream_proxy(std::shared_ptr<SSHConnector> const &proxy);
  void password(std::string const &string);
  void port(int32_t const &int_1);
  void set_use_ssh_agent(bool const &boolean);
  std::shared_ptr<SSHOptions> check_host(bool const &boolean);
  void username(std::string const &string);
  std::string username();
};

class Command : public ServerObject {
public:
  Command();
  void add(std::vector<std::string> const &string);
};

class Module : public ServerObject {
public:
};

class Task : public ServerObject {
public:
};

class ScriptingLogger : public ServerObject {
public:
};

class Pipe : public ServerObject {
public:
};

class DirectLauncher : public Launcher {
public:
  DirectLauncher(std::shared_ptr<Connector> const &connector);
};

class ReadWriteDependency : public Dependency {
public:
};

class JsonReal : public Json {
public:
};

class SSHConnector : public SingleHostConnector {
public:
  std::string env(std::shared_ptr<Launcher> const &launcher, std::string const &string);
};

class Namespace : public ServerObject {
public:
  Namespace(std::string const &string, std::string const &string_1);
};

class XPM : public ServerObject {
public:
  std::shared_ptr<ScriptingLogger> get_logger(std::string const &string);
  std::shared_ptr<Path> get_script_file();
  std::string ns();
  std::shared_ptr<ScriptingLogger> logger(std::string const &string);
  void log_level(std::string const &name, std::string const &level);
  static std::shared_ptr<TokenResource> token(std::string const &path);
  std::string get_script_path();
  void publish();
  static std::shared_ptr<TokenResource> token_resource(std::string const &path);
  static std::shared_ptr<TokenResource> token_resource(std::string const &path, bool const &post_process);
  bool simulate();
  bool simulate(bool const &boolean);
  std::shared_ptr<Task> get_task(std::string const &string, std::string const &string_1);
};

class JsonObject : public Json {
public:
  void FIELDS(std::string const &string, std::shared_ptr<Json> const &json);
  std::shared_ptr<Json> FIELDS(std::string const &string);
};

class Job : public Resource {
public:
};

class JsonTask : public Json {
public:
};

class JsonArray : public Json {
public:
  int32_t LENGTH();
  std::shared_ptr<Json> FIELDS(int32_t const &int_1);
  std::string join(std::string const &string);
};

class Path : public ServerObject {
public:
};

class TokenResource : public Resource {
public:
  int32_t getLimit();
  void set_limit(int32_t const &int_1);
  int32_t used();
};

class JsonNull : public Json {
public:
};

class OARLauncher : public Launcher {
public:
  OARLauncher(std::shared_ptr<Connector> const &connector);
  virtual std::shared_ptr<LauncherParameters> parameters();
  void use_notify(bool const &boolean);
  void email(std::string const &string);
};

class JavaTaskFactory : public ServerObject {
public:
};

class JsonPath : public Json {
public:
  std::string uri();
};

class ObjectPropertyReference : public ServerObject {
public:
};

} // xpm namespace
#endif
