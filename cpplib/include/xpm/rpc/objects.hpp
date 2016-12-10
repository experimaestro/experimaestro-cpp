#ifndef _XPM_RPCOBJECTS_H
#define _XPM_RPCOBJECTS_H

#include <vector>
#include <xpm/rpc/utils.hpp>

#ifdef SWIG
%shared_ptr(xpm::rpc::DirectLauncher);
%shared_ptr(xpm::rpc::ObjectPropertyReference);
%shared_ptr(xpm::rpc::CommandOutput);
%shared_ptr(xpm::rpc::Commands);
%shared_ptr(xpm::rpc::ScriptingList);
%shared_ptr(xpm::rpc::AbstractCommandComponent);
%shared_ptr(xpm::rpc::JsonReal);
%shared_ptr(xpm::rpc::SSHConnector);
%shared_ptr(xpm::rpc::Module);
%shared_ptr(xpm::rpc::Json);
%shared_ptr(xpm::rpc::LocalhostConnector);
%shared_ptr(xpm::rpc::JsonString);
%shared_ptr(xpm::rpc::JsonNull);
%shared_ptr(xpm::rpc::JsonParameterFile);
%shared_ptr(xpm::rpc::ScriptingLogger);
%shared_ptr(xpm::rpc::OARLauncher);
%shared_ptr(xpm::rpc::SubCommand);
%shared_ptr(xpm::rpc::SSHOptions);
%shared_ptr(xpm::rpc::Resource);
%shared_ptr(xpm::rpc::JsonResource);
%shared_ptr(xpm::rpc::AbstractCommand);
%shared_ptr(xpm::rpc::XPM);
%shared_ptr(xpm::rpc::Launcher);
%shared_ptr(xpm::rpc::Pipe);
%shared_ptr(xpm::rpc::LauncherParameters);
%shared_ptr(xpm::rpc::Path);
%shared_ptr(xpm::rpc::ParameterFile);
%shared_ptr(xpm::rpc::Dependency);
%shared_ptr(xpm::rpc::Namespace);
%shared_ptr(xpm::rpc::Job);
%shared_ptr(xpm::rpc::JsonPath);
%shared_ptr(xpm::rpc::JsonBoolean);
%shared_ptr(xpm::rpc::Tasks);
%shared_ptr(xpm::rpc::Command);
%shared_ptr(xpm::rpc::JsonObject);
%shared_ptr(xpm::rpc::ReadWriteDependency);
%shared_ptr(xpm::rpc::Task);
%shared_ptr(xpm::rpc::CommandComponent);
%shared_ptr(xpm::rpc::Scheduler);
%shared_ptr(xpm::rpc::SingleHostConnector);
%shared_ptr(xpm::rpc::OARParameters);
%shared_ptr(xpm::rpc::JsonArray);
%shared_ptr(xpm::rpc::ScriptingMap);
%shared_ptr(xpm::rpc::JsonTask);
%shared_ptr(xpm::rpc::ConnectorOptions);
%shared_ptr(xpm::rpc::JavaTaskFactory);
%shared_ptr(xpm::rpc::Connector);
%shared_ptr(xpm::rpc::TokenResource);
%shared_ptr(xpm::rpc::CommandString);
#endif
namespace xpm { namespace rpc {


// Pre-declaration
class DirectLauncher;
class ObjectPropertyReference;
class CommandOutput;
class Commands;
class ScriptingList;
class AbstractCommandComponent;
class JsonReal;
class SSHConnector;
class Module;
class Json;
class LocalhostConnector;
class JsonString;
class JsonNull;
class JsonParameterFile;
class ScriptingLogger;
class OARLauncher;
class SubCommand;
class SSHOptions;
class Resource;
class JsonResource;
class AbstractCommand;
class XPM;
class Launcher;
class Pipe;
class LauncherParameters;
class Path;
class ParameterFile;
class Dependency;
class Namespace;
class Job;
class JsonPath;
class JsonBoolean;
class Tasks;
class Command;
class JsonObject;
class ReadWriteDependency;
class Task;
class CommandComponent;
class Scheduler;
class SingleHostConnector;
class OARParameters;
class JsonArray;
class ScriptingMap;
class JsonTask;
class ConnectorOptions;
class JavaTaskFactory;
class Connector;
class TokenResource;
class CommandString;


// Classes
class Connector : public virtual ServerObject {
protected:
  virtual std::string const &__name__() const override;

protected:
  Connector() {}

public:
  /**   */
  virtual std::string resolve(std::shared_ptr<Path> const &path);
  /**   */
  static std::shared_ptr<Connector> create(std::string const &string, std::string const &string_1, std::shared_ptr<ConnectorOptions> const &connectorOptions);
  /**   */
  virtual std::shared_ptr<Launcher> default_launcher();
};

class AbstractCommandComponent : public virtual ServerObject {
protected:
  AbstractCommandComponent() {}

public:
};

class SingleHostConnector : public Connector {
protected:
  virtual std::string const &__name__() const override;

protected:
  SingleHostConnector() {}

public:
};

class Json : public virtual ServerObject {
protected:
  virtual std::string const &__name__() const override;

protected:
  Json() {}

public:
  /**   */
  virtual std::shared_ptr<Json> copy();
  /**   */
  virtual std::shared_ptr<Json> copy(bool const &boolean);
  /**   */
  virtual std::string get_descriptor();
  /**   */
  virtual bool is_array();
  /**   */
  virtual bool isSimple();
  /**   */
  virtual std::string toSource();
  /** Creates a parameter file from this JSON
  */
  virtual std::shared_ptr<ParameterFile> as_parameter_file(std::string const &string, std::shared_ptr<SingleHostConnector> const &singleHostConnector);
  /**   */
  virtual std::shared_ptr<JsonObject> as_object();
  /**   */
  virtual bool is_object();
};

class CommandComponent : public AbstractCommandComponent {
protected:
  virtual std::string const &__name__() const override;

protected:
  CommandComponent() {}

public:
};

class Launcher : public virtual ServerObject {
protected:
  virtual std::string const &__name__() const override;

protected:
  Launcher() {}

public:
  /**   */
  virtual std::shared_ptr<LauncherParameters> parameters();
  /**   */
  virtual std::string environment(std::string const &key);
  /** Gets the value of the environment variable
  */
  virtual std::string env(std::string const &string);
  /** Sets an environment variable and returns the old value (if any)
  */
  virtual std::string env(std::string const &key, std::string const &value);
  /**   */
  virtual void set_notification_url(std::string const &string);
  /** Sets the temporary directory for this launcher
  */
  virtual void set_tmpdir(std::shared_ptr<Path> const &path);
};

class ConnectorOptions : public virtual ServerObject {
protected:
  virtual std::string const &__name__() const override;

protected:
  ConnectorOptions() {}

public:
};

class Dependency : public virtual ServerObject {
protected:
  virtual std::string const &__name__() const override;

protected:
  Dependency() {}

public:
};

class LauncherParameters : public virtual ServerObject {
protected:
  virtual std::string const &__name__() const override;

protected:
  LauncherParameters() {}

public:
};

class AbstractCommand : public virtual ServerObject {
protected:
  virtual std::string const &__name__() const override;

protected:
  AbstractCommand() {}

public:
  /**   */
  virtual std::shared_ptr<CommandOutput> output();
  /**   */
  virtual void add_dependency(std::shared_ptr<Dependency> const &dependency);
};

class Resource : public virtual ServerObject {
protected:
  virtual std::string const &__name__() const override;

protected:
  Resource() {}

public:
  /**   */
  virtual std::string toString();
  /**   */
  virtual std::shared_ptr<Path> resolve(std::string const &string);
  /**   */
  virtual std::shared_ptr<Path> file();
  /**   */
  virtual std::shared_ptr<Path> output();
};

class XPM : public virtual ServerObject {
protected:
  virtual std::string const &__name__() const override;

protected:
  XPM() {}

public:
  /** Returns a file relative to the current connector
  */
  virtual std::shared_ptr<Path> file(std::string const &filepath);
  /** Publish the repository on the web server
  */
  virtual void publish();
  /** Retrieve (or creates) a token resource with a given xpath
com.sun.javafx.binding.StringConstant@68e965f5  */
  static std::shared_ptr<TokenResource> token_resource(std::string const &path);
  /** com.sun.javafx.binding.StringConstant@f381794com.sun.javafx.binding.StringConstant@2cdd0d4b  */
  static std::shared_ptr<TokenResource> token_resource(std::string const &path, bool const &post_process);
  /** Sets the logger debug level
  */
  virtual void log_level(std::string const &name, std::string const &level);
  /**   */
  virtual std::string get_script_path();
  /** Set the simulate flag: When true, the jobs are not submitted but just output
  */
  virtual bool simulate(bool const &boolean);
  /**   */
  virtual bool simulate();
  /**   */
  virtual std::string ns();
  /**   */
  virtual std::shared_ptr<Task> get_task(std::string const &string, std::string const &string_1);
  /** com.sun.javafx.binding.StringConstant@776b83cc  */
  static std::shared_ptr<TokenResource> token(std::string const &path);
};

class ScriptingMap : public virtual ServerObject {
protected:
  virtual std::string const &__name__() const override;

protected:
  ScriptingMap() {}

public:
};

class ObjectPropertyReference : public virtual ServerObject {
protected:
  virtual std::string const &__name__() const override;

protected:
  ObjectPropertyReference() {}

public:
};

class JsonString : public Json {
protected:
  virtual std::string const &__name__() const override;

protected:
  JsonString() {}

public:
  /**   */
  virtual std::string toString();
};

class Pipe : public CommandComponent {
protected:
  virtual std::string const &__name__() const override;

protected:
  Pipe() {}

public:
};

class JsonTask : public Json {
protected:
  virtual std::string const &__name__() const override;

protected:
  JsonTask() {}

public:
  /**   */
  virtual std::shared_ptr<JsonObject> json();
  /**   */
  virtual std::shared_ptr<AbstractCommand> command();
};

class ReadWriteDependency : public Dependency {
protected:
  virtual std::string const &__name__() const override;

protected:
  ReadWriteDependency() {}

public:
};

class LocalhostConnector : public SingleHostConnector {
protected:
  virtual std::string const &__name__() const override;

protected:
  LocalhostConnector() {}

public:
  /**   */
  virtual std::string env(std::string const &string);
};

class JsonResource : public Json {
protected:
  virtual std::string const &__name__() const override;

protected:
  JsonResource() {}

public:
};

class ScriptingLogger : public virtual ServerObject {
protected:
  virtual std::string const &__name__() const override;

public:
  /** Sets the level
  */
  virtual void set_level(std::string const &level);
};

class SSHConnector : public SingleHostConnector {
protected:
  virtual std::string const &__name__() const override;

protected:
  SSHConnector() {}

public:
  /**   */
  virtual std::string env(std::shared_ptr<Launcher> const &launcher, std::string const &string);
};

class Namespace : public virtual ServerObject {
protected:
  virtual std::string const &__name__() const override;

public:
  /**   */
  Namespace(std::string const &string, std::string const &string_1);
  /**   */
  virtual std::string uri();
};

class CommandString : public CommandComponent {
protected:
  virtual std::string const &__name__() const override;

public:
  /**   */
  CommandString(std::string const &string);
  /**   */
  virtual std::string toString();
};

class JsonPath : public Json {
protected:
  virtual std::string const &__name__() const override;

protected:
  JsonPath() {}

public:
  /**   */
  virtual std::string uri();
};

class Commands : public AbstractCommand {
protected:
  virtual std::string const &__name__() const override;

public:
  /**   */
  Commands();
  /**   */
  Commands(std::vector<std::shared_ptr<AbstractCommand>> const &abstractCommand);
  /**   */
  virtual void add(std::shared_ptr<AbstractCommand> const &abstractCommand);
};

class Job : public Resource {
protected:
  virtual std::string const &__name__() const override;

protected:
  Job() {}

public:
};

class Tasks : public virtual ServerObject {
protected:
  virtual std::string const &__name__() const override;

public:
  /**   */
  Tasks();
};

class Task : public virtual ServerObject {
protected:
  virtual std::string const &__name__() const override;

protected:
  Task() {}

public:
};

class SubCommand : public CommandComponent {
protected:
  virtual std::string const &__name__() const override;

protected:
  SubCommand() {}

public:
};

class ScriptingList : public virtual ServerObject {
protected:
  virtual std::string const &__name__() const override;

protected:
  ScriptingList() {}

public:
  /**   */
  virtual int32_t length();
};

class OARParameters : public LauncherParameters {
protected:
  virtual std::string const &__name__() const override;

protected:
  OARParameters() {}

public:
};

class JsonArray : public Json {
protected:
  virtual std::string const &__name__() const override;

protected:
  JsonArray() {}

public:
  /**   */
  virtual std::shared_ptr<Json> get(int32_t const &int_1);
  /**   */
  virtual std::string join(std::string const &string);
  /**   */
  virtual int32_t length();
  /**   */
  virtual int32_t _size();
};

class OARLauncher : public Launcher {
protected:
  virtual std::string const &__name__() const override;

public:
  /**   */
  OARLauncher(std::shared_ptr<Connector> const &connector);
  /**   */
  virtual std::shared_ptr<OARParameters> oarParameters();
  /** Send a notification email. Process it to notify experimaestro when a job status changes.
  */
  virtual void email(std::string const &string);
  /**   */
  virtual void use_notify(bool const &boolean);
};

class Module : public virtual ServerObject {
protected:
  virtual std::string const &__name__() const override;

protected:
  Module() {}

public:
};

class JsonReal : public Json {
protected:
  virtual std::string const &__name__() const override;

protected:
  JsonReal() {}

public:
};

class JsonNull : public Json {
protected:
  virtual std::string const &__name__() const override;

protected:
  JsonNull() {}

public:
};

class Command : public AbstractCommandComponent, public AbstractCommand {
protected:
  virtual std::string const &__name__() const override;

public:
  /**   */
  Command();
  /**   */
  virtual void add(std::vector<std::string> const &string);
  /**   */
  virtual void add(std::vector<std::shared_ptr<AbstractCommandComponent>> const &abstractCommandComponent);
  /**   */
  virtual void add_subcommand(std::shared_ptr<Commands> const &commands);
  /**   */
  static std::shared_ptr<Command> getCommand(std::shared_ptr<ScriptingList> const &list);
};

class CommandOutput : public CommandComponent {
protected:
  virtual std::string const &__name__() const override;

protected:
  CommandOutput() {}

public:
};

class ParameterFile : public CommandComponent {
protected:
  virtual std::string const &__name__() const override;

public:
  /**   */
  ParameterFile(std::string const &key, std::string const &content);
};

class Scheduler : public virtual ServerObject {
protected:
  virtual std::string const &__name__() const override;

protected:
  Scheduler() {}

public:
  /**   */
  static void submitJob(std::shared_ptr<AbstractCommand> const &abstractCommand);
};

class JsonBoolean : public Json {
protected:
  virtual std::string const &__name__() const override;

protected:
  JsonBoolean() {}

public:
};

class JsonParameterFile : public CommandComponent {
protected:
  virtual std::string const &__name__() const override;

public:
  /**   */
  JsonParameterFile(std::string const &string, std::shared_ptr<Json> const &json_1);
};

class JsonObject : public Json {
protected:
  virtual std::string const &__name__() const override;

protected:
  JsonObject() {}

public:
  /**   */
  virtual void put(std::string const &string, std::shared_ptr<Json> const &json_1);
  /**   */
  virtual std::shared_ptr<Json> getField(std::string const &string);
};

class DirectLauncher : public Launcher {
protected:
  virtual std::string const &__name__() const override;

public:
  /**   */
  DirectLauncher(std::shared_ptr<Connector> const &connector);
};

class JavaTaskFactory : public virtual ServerObject {
protected:
  virtual std::string const &__name__() const override;

protected:
  JavaTaskFactory() {}

public:
};

class TokenResource : public Resource {
protected:
  virtual std::string const &__name__() const override;

protected:
  TokenResource() {}

public:
  /**   */
  virtual int32_t used();
  /**   */
  virtual int32_t getLimit();
  /**   */
  virtual void set_limit(int32_t const &int_1);
};

class SSHOptions : public ConnectorOptions {
protected:
  virtual std::string const &__name__() const override;

public:
  /**   */
  SSHOptions();
  /**   */
  virtual std::string hostname();
  /**   */
  virtual void set_use_ssh_agent(bool const &boolean);
  /**   */
  virtual void set_stream_proxy(std::shared_ptr<SSHConnector> const &proxy);
  /**   */
  virtual void set_stream_proxy(std::string const &uri, std::shared_ptr<SSHOptions> const &options);
  /**   */
  virtual std::shared_ptr<SSHOptions> check_host(bool const &boolean);
  /**   */
  virtual void hostname(std::string const &string);
  /**   */
  virtual void port(int32_t const &int_1);
  /**   */
  virtual void username(std::string const &string);
  /**   */
  virtual void password(std::string const &string);
  /**   */
  virtual std::string username();
};

class Path : public virtual ServerObject {
protected:
  virtual std::string const &__name__() const override;

public:
  /**   */
  virtual std::string toString();
  /**   */
  virtual bool exists();
  /** Creates this folder, if it does not exist.  Also creates any ancestor
folders which do not exist.  This method does nothing if the folder
already exists.
  */
  virtual void mkdirs();
  /**   */
  virtual std::string toSource();
  /**   */
  virtual std::string read_all();
  /**   */
  virtual int64_t get_size();
  /** Find all the matching files within this folder
com.sun.javafx.binding.StringConstant@5939a379  */
  virtual std::shared_ptr<JsonArray> find_matching_files(std::string const &regexp);
  /** Get the file path, ignoring the file scheme
  */
  virtual std::string get_path();
  /**   */
  virtual std::string uri();
};

} }// xpm::rpc namespace
#endif
