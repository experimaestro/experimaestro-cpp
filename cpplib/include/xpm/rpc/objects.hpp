#ifndef _XPM_RPCOBJECTS_H
#define _XPM_RPCOBJECTS_H

#include <vector>
#include <xpm/rpc/utils.hpp>

#ifdef SWIG
%shared_ptr(xpm::rpc::Dependency);
%shared_ptr(xpm::rpc::ConnectorOptions);
%shared_ptr(xpm::rpc::Json);
%shared_ptr(xpm::rpc::JsonResource);
%shared_ptr(xpm::rpc::Tasks);
%shared_ptr(xpm::rpc::JsonParameterFile);
%shared_ptr(xpm::rpc::JsonString);
%shared_ptr(xpm::rpc::ReadWriteDependency);
%shared_ptr(xpm::rpc::SingleHostConnector);
%shared_ptr(xpm::rpc::LauncherParameters);
%shared_ptr(xpm::rpc::ScriptingList);
%shared_ptr(xpm::rpc::Job);
%shared_ptr(xpm::rpc::Namespace);
%shared_ptr(xpm::rpc::Path);
%shared_ptr(xpm::rpc::JsonArray);
%shared_ptr(xpm::rpc::XPM);
%shared_ptr(xpm::rpc::Connector);
%shared_ptr(xpm::rpc::JsonNull);
%shared_ptr(xpm::rpc::Task);
%shared_ptr(xpm::rpc::AbstractCommand);
%shared_ptr(xpm::rpc::CommandOutput);
%shared_ptr(xpm::rpc::SSHOptions);
%shared_ptr(xpm::rpc::Launcher);
%shared_ptr(xpm::rpc::JsonObject);
%shared_ptr(xpm::rpc::JsonReal);
%shared_ptr(xpm::rpc::JsonPath);
%shared_ptr(xpm::rpc::ScriptingLogger);
%shared_ptr(xpm::rpc::Module);
%shared_ptr(xpm::rpc::Command);
%shared_ptr(xpm::rpc::JsonBoolean);
%shared_ptr(xpm::rpc::Pipe);
%shared_ptr(xpm::rpc::ObjectPropertyReference);
%shared_ptr(xpm::rpc::JavaTaskFactory);
%shared_ptr(xpm::rpc::ParameterFile);
%shared_ptr(xpm::rpc::Resource);
%shared_ptr(xpm::rpc::Scheduler);
%shared_ptr(xpm::rpc::LocalhostConnector);
%shared_ptr(xpm::rpc::DirectLauncher);
%shared_ptr(xpm::rpc::ScriptingMap);
%shared_ptr(xpm::rpc::OARParameters);
%shared_ptr(xpm::rpc::SSHConnector);
%shared_ptr(xpm::rpc::OARLauncher);
%shared_ptr(xpm::rpc::TokenResource);
%shared_ptr(xpm::rpc::SubCommand);
%shared_ptr(xpm::rpc::JsonTask);
#endif
namespace xpm { namespace rpc {


// Pre-declaration
class Dependency;
class ConnectorOptions;
class Json;
class JsonResource;
class Tasks;
class JsonParameterFile;
class JsonString;
class ReadWriteDependency;
class SingleHostConnector;
class LauncherParameters;
class ScriptingList;
class Job;
class Namespace;
class Path;
class JsonArray;
class XPM;
class Connector;
class JsonNull;
class Task;
class AbstractCommand;
class CommandOutput;
class SSHOptions;
class Launcher;
class JsonObject;
class JsonReal;
class JsonPath;
class ScriptingLogger;
class Module;
class Command;
class JsonBoolean;
class Pipe;
class ObjectPropertyReference;
class JavaTaskFactory;
class ParameterFile;
class Resource;
class Scheduler;
class LocalhostConnector;
class DirectLauncher;
class ScriptingMap;
class OARParameters;
class SSHConnector;
class OARLauncher;
class TokenResource;
class SubCommand;
class JsonTask;


// Classes
class Connector : public ServerObject {
protected:
  virtual std::string const &__name__() const override;

public:
  /**   */
  std::string resolve(std::shared_ptr<Path> const &path);
  /**   */
  static std::shared_ptr<Connector> create(std::string const &string, std::string const &string_1, std::shared_ptr<ConnectorOptions> const &connectorOptions);
  /**   */
  std::shared_ptr<Launcher> default_launcher();
};

class LauncherParameters : public ServerObject {
protected:
  virtual std::string const &__name__() const override;

public:
};

class Json : public ServerObject {
protected:
  virtual std::string const &__name__() const override;

public:
  /**   */
  std::shared_ptr<Json> copy();
  /**   */
  std::shared_ptr<Json> copy(bool const &boolean);
  /**   */
  bool isSimple();
  /**   */
  bool is_array();
  /**   */
  std::string toSource();
  /**   */
  std::string get_descriptor();
  /** Creates a parameter file from this JSON
  */
  std::shared_ptr<ParameterFile> as_parameter_file(std::string const &string, std::shared_ptr<SingleHostConnector> const &singleHostConnector);
  /**   */
  std::shared_ptr<JsonObject> as_object();
  /**   */
  bool is_object();
};

class Dependency : public ServerObject {
protected:
  virtual std::string const &__name__() const override;

public:
};

class ConnectorOptions : public ServerObject {
protected:
  virtual std::string const &__name__() const override;

public:
};

class SingleHostConnector : public Connector {
protected:
  virtual std::string const &__name__() const override;

public:
};

class Resource : public ServerObject {
protected:
  virtual std::string const &__name__() const override;

public:
  /**   */
  std::string toString();
  /**   */
  std::shared_ptr<Path> resolve(std::string const &string);
  /**   */
  std::shared_ptr<Path> file();
  /**   */
  std::shared_ptr<Path> output();
};

class Launcher : public ServerObject {
protected:
  virtual std::string const &__name__() const override;

public:
  /**   */
  std::shared_ptr<LauncherParameters> parameters();
  /**   */
  std::string environment(std::string const &key);
  /** Gets the value of the environment variable
  */
  std::string env(std::string const &string);
  /** Sets an environment variable and returns the old value (if any)
  */
  std::string env(std::string const &key, std::string const &value);
  /**   */
  void set_notification_url(std::string const &string);
  /** Sets the temporary directory for this launcher
  */
  void set_tmpdir(std::shared_ptr<Path> const &path);
};

class AbstractCommand : public ServerObject {
protected:
  virtual std::string const &__name__() const override;

public:
  /**   */
  void add_dependency(std::shared_ptr<Dependency> const &dependency);
  /**   */
  std::shared_ptr<CommandOutput> output();
};

class JsonObject : public Json {
protected:
  virtual std::string const &__name__() const override;

public:
  /**   */
  void put(std::string const &string, std::shared_ptr<Json> const &json_1);
  /**   */
  std::shared_ptr<Json> getField(std::string const &string);
};

class OARParameters : public LauncherParameters {
protected:
  virtual std::string const &__name__() const override;

public:
};

class Job : public Resource {
protected:
  virtual std::string const &__name__() const override;

public:
};

class JsonTask : public Json {
protected:
  virtual std::string const &__name__() const override;

public:
  /**   */
  std::shared_ptr<AbstractCommand> command();
  /**   */
  std::shared_ptr<JsonObject> json();
};

class JsonResource : public Json {
protected:
  virtual std::string const &__name__() const override;

public:
};

class Path : public ServerObject {
protected:
  virtual std::string const &__name__() const override;

public:
  /**   */
  std::string toString();
  /**   */
  bool exists();
  /** Creates this folder, if it does not exist.  Also creates any ancestor
folders which do not exist.  This method does nothing if the folder
already exists.
  */
  void mkdirs();
  /**   */
  std::string read_all();
  /**   */
  int64_t get_size();
  /** Find all the matching files within this folder
com.sun.javafx.binding.StringConstant@1fb700ee  */
  std::shared_ptr<JsonArray> find_matching_files(std::string const &regexp);
  /** Get the file path, ignoring the file scheme
  */
  std::string get_path();
  /**   */
  std::string toSource();
  /**   */
  std::string uri();
};

class JsonPath : public Json {
protected:
  virtual std::string const &__name__() const override;

public:
  /**   */
  std::string uri();
};

class DirectLauncher : public Launcher {
protected:
  virtual std::string const &__name__() const override;

public:
  /**   */
  DirectLauncher(std::shared_ptr<Connector> const &connector);
};

class Namespace : public ServerObject {
protected:
  virtual std::string const &__name__() const override;

public:
  /**   */
  Namespace(std::string const &string, std::string const &string_1);
  /**   */
  std::string uri();
};

class SSHOptions : public ConnectorOptions {
protected:
  virtual std::string const &__name__() const override;

public:
  /**   */
  SSHOptions();
  /**   */
  std::string hostname();
  /**   */
  std::shared_ptr<SSHOptions> check_host(bool const &boolean);
  /**   */
  void set_use_ssh_agent(bool const &boolean);
  /**   */
  void set_stream_proxy(std::string const &uri, std::shared_ptr<SSHOptions> const &options);
  /**   */
  void set_stream_proxy(std::shared_ptr<SSHConnector> const &proxy);
  /**   */
  void hostname(std::string const &string);
  /**   */
  void port(int32_t const &int_1);
  /**   */
  void username(std::string const &string);
  /**   */
  void password(std::string const &string);
  /**   */
  std::string username();
};

class Pipe : public ServerObject {
protected:
  virtual std::string const &__name__() const override;

public:
};

class JavaTaskFactory : public ServerObject {
protected:
  virtual std::string const &__name__() const override;

public:
};

class JsonParameterFile : public ServerObject {
protected:
  virtual std::string const &__name__() const override;

public:
  /**   */
  JsonParameterFile(std::string const &string, std::shared_ptr<Json> const &json_1);
};

class ObjectPropertyReference : public ServerObject {
protected:
  virtual std::string const &__name__() const override;

public:
};

class ScriptingLogger : public ServerObject {
protected:
  virtual std::string const &__name__() const override;

public:
  /** Sets the level
  */
  void set_level(std::string const &level);
};

class ScriptingMap : public ServerObject {
protected:
  virtual std::string const &__name__() const override;

public:
};

class Task : public ServerObject {
protected:
  virtual std::string const &__name__() const override;

public:
};

class JsonString : public Json {
protected:
  virtual std::string const &__name__() const override;

public:
  /**   */
  std::string toString();
};

class TokenResource : public Resource {
protected:
  virtual std::string const &__name__() const override;

public:
  /**   */
  int32_t getLimit();
  /**   */
  int32_t used();
  /**   */
  void set_limit(int32_t const &int_1);
};

class OARLauncher : public Launcher {
protected:
  virtual std::string const &__name__() const override;

public:
  /**   */
  OARLauncher(std::shared_ptr<Connector> const &connector);
  /**   */
  std::shared_ptr<OARParameters> parameters();
  /** Send a notification email. Process it to notify experimaestro when a job status changes.
  */
  void email(std::string const &string);
  /**   */
  void use_notify(bool const &boolean);
};

class SubCommand : public ServerObject {
protected:
  virtual std::string const &__name__() const override;

public:
};

class JsonNull : public Json {
protected:
  virtual std::string const &__name__() const override;

public:
};

class ScriptingList : public ServerObject {
protected:
  virtual std::string const &__name__() const override;

public:
  /**   */
  int32_t length();
};

class JsonArray : public Json {
protected:
  virtual std::string const &__name__() const override;

public:
  /**   */
  std::shared_ptr<Json> get(int32_t const &int_1);
  /**   */
  std::string join(std::string const &string);
  /**   */
  int32_t length();
  /**   */
  int32_t _size();
};

class Scheduler : public ServerObject {
protected:
  virtual std::string const &__name__() const override;

public:
  /**   */
  static void submitJob(std::shared_ptr<AbstractCommand> const &abstractCommand);
};

class JsonBoolean : public Json {
protected:
  virtual std::string const &__name__() const override;

public:
};

class Tasks : public ServerObject {
protected:
  virtual std::string const &__name__() const override;

public:
  /**   */
  Tasks();
};

class Command : public AbstractCommand {
protected:
  virtual std::string const &__name__() const override;

public:
  /**   */
  Command();
  /**   */
  void add(std::vector<std::string> const &string);
  /**   */
  static std::shared_ptr<Command> getCommand(std::shared_ptr<ScriptingList> const &list);
};

class CommandOutput : public ServerObject {
protected:
  virtual std::string const &__name__() const override;

public:
};

class SSHConnector : public SingleHostConnector {
protected:
  virtual std::string const &__name__() const override;

public:
  /**   */
  std::string env(std::shared_ptr<Launcher> const &launcher, std::string const &string);
};

class ParameterFile : public ServerObject {
protected:
  virtual std::string const &__name__() const override;

public:
};

class XPM : public ServerObject {
protected:
  virtual std::string const &__name__() const override;

public:
  /** Returns a file relative to the current connector
  */
  std::shared_ptr<Path> file(std::string const &filepath);
  /**   */
  std::shared_ptr<Task> get_task(std::string const &string, std::string const &string_1);
  /** com.sun.javafx.binding.StringConstant@2525ff7ecom.sun.javafx.binding.StringConstant@524d6d96  */
  static std::shared_ptr<TokenResource> token_resource(std::string const &path, bool const &post_process);
  /** Retrieve (or creates) a token resource with a given xpath
com.sun.javafx.binding.StringConstant@152aa092  */
  static std::shared_ptr<TokenResource> token_resource(std::string const &path);
  /** Sets the logger debug level
  */
  void log_level(std::string const &name, std::string const &level);
  /**   */
  std::string get_script_path();
  /** Publish the repository on the web server
  */
  void publish();
  /**   */
  std::string ns();
  /** Set the simulate flag: When true, the jobs are not submitted but just output
  */
  bool simulate(bool const &boolean);
  /**   */
  bool simulate();
  /** com.sun.javafx.binding.StringConstant@37858383  */
  static std::shared_ptr<TokenResource> token(std::string const &path);
};

class LocalhostConnector : public SingleHostConnector {
protected:
  virtual std::string const &__name__() const override;

public:
  /**   */
  std::string env(std::string const &string);
};

class ReadWriteDependency : public Dependency {
protected:
  virtual std::string const &__name__() const override;

public:
};

class JsonReal : public Json {
protected:
  virtual std::string const &__name__() const override;

public:
};

class Module : public ServerObject {
protected:
  virtual std::string const &__name__() const override;

public:
};

} }// xpm::rpc namespace
#endif
