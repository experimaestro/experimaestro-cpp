#ifndef _XPM_RPCOBJECTS_H
#define _XPM_RPCOBJECTS_H

#include <vector>
#include <xpm/rpc/utils.hpp>

#ifdef SWIG
%shared_ptr(xpm::rpc::Resource);
%shared_ptr(xpm::rpc::Pipe);
%shared_ptr(xpm::rpc::LocalhostConnector);
%shared_ptr(xpm::rpc::AbstractCommandComponent);
%shared_ptr(xpm::rpc::JsonParameterFile);
%shared_ptr(xpm::rpc::Functions);
%shared_ptr(xpm::rpc::SingleHostConnector);
%shared_ptr(xpm::rpc::TokenResource);
%shared_ptr(xpm::rpc::Job);
%shared_ptr(xpm::rpc::CommandPath);
%shared_ptr(xpm::rpc::CommandString);
%shared_ptr(xpm::rpc::ConnectorOptions);
%shared_ptr(xpm::rpc::OARLauncher);
%shared_ptr(xpm::rpc::Commands);
%shared_ptr(xpm::rpc::AbstractCommand);
%shared_ptr(xpm::rpc::CommandComponent);
%shared_ptr(xpm::rpc::CommandLineTask);
%shared_ptr(xpm::rpc::LauncherParameters);
%shared_ptr(xpm::rpc::Module);
%shared_ptr(xpm::rpc::SSHConnector);
%shared_ptr(xpm::rpc::Task);
%shared_ptr(xpm::rpc::JsonTask);
%shared_ptr(xpm::rpc::JavaTaskFactory);
%shared_ptr(xpm::rpc::ParameterFile);
%shared_ptr(xpm::rpc::XPM);
%shared_ptr(xpm::rpc::Dependency);
%shared_ptr(xpm::rpc::OARParameters);
%shared_ptr(xpm::rpc::Namespace);
%shared_ptr(xpm::rpc::Connector);
%shared_ptr(xpm::rpc::Command);
%shared_ptr(xpm::rpc::CommandOutput);
%shared_ptr(xpm::rpc::ReadWriteDependency);
%shared_ptr(xpm::rpc::Launcher);
%shared_ptr(xpm::rpc::DirectLauncher);
%shared_ptr(xpm::rpc::SubCommand);
%shared_ptr(xpm::rpc::SSHOptions);
%shared_ptr(xpm::rpc::Path);
#endif
namespace xpm { namespace rpc {


// Pre-declaration
class Resource;
class Pipe;
class LocalhostConnector;
class AbstractCommandComponent;
class JsonParameterFile;
class Functions;
class SingleHostConnector;
class TokenResource;
class Job;
class CommandPath;
class CommandString;
class ConnectorOptions;
class OARLauncher;
class Commands;
class AbstractCommand;
class CommandComponent;
class CommandLineTask;
class LauncherParameters;
class Module;
class SSHConnector;
class Task;
class JsonTask;
class JavaTaskFactory;
class ParameterFile;
class XPM;
class Dependency;
class OARParameters;
class Namespace;
class Connector;
class Command;
class CommandOutput;
class ReadWriteDependency;
class Launcher;
class DirectLauncher;
class SubCommand;
class SSHOptions;
class Path;


// Classes
class Resource : public virtual ServerObject {
protected:
  friend struct RPCConverter<std::shared_ptr<Resource>>;
  explicit Resource(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

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

class AbstractCommandComponent : public virtual ServerObject {
protected:
  friend struct RPCConverter<std::shared_ptr<AbstractCommandComponent>>;
  explicit AbstractCommandComponent(ObjectIdentifier o);
  AbstractCommandComponent() {}

public:
};

class Connector : public virtual ServerObject {
protected:
  friend struct RPCConverter<std::shared_ptr<Connector>>;
  explicit Connector(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

  Connector() {}

public:
  /**   */
  virtual std::string resolve(std::shared_ptr<Path> const &path);
  /**   */
  static std::shared_ptr<Connector> create(std::string const &string, std::string const &string_1, std::shared_ptr<ConnectorOptions> const &connectorOptions);
  /**   */
  virtual std::shared_ptr<Launcher> default_launcher();
};

class Job : public Resource {
protected:
  friend struct RPCConverter<std::shared_ptr<Job>>;
  explicit Job(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

  Job() {}

public:
};

class LauncherParameters : public virtual ServerObject {
protected:
  friend struct RPCConverter<std::shared_ptr<LauncherParameters>>;
  explicit LauncherParameters(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

  LauncherParameters() {}

public:
};

class Launcher : public virtual ServerObject {
protected:
  friend struct RPCConverter<std::shared_ptr<Launcher>>;
  explicit Launcher(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

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

class SingleHostConnector : public Connector {
protected:
  friend struct RPCConverter<std::shared_ptr<SingleHostConnector>>;
  explicit SingleHostConnector(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

  SingleHostConnector() {}

public:
};

class Dependency : public virtual ServerObject {
protected:
  friend struct RPCConverter<std::shared_ptr<Dependency>>;
  explicit Dependency(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

  Dependency() {}

public:
};

class CommandComponent : public AbstractCommandComponent {
protected:
  friend struct RPCConverter<std::shared_ptr<CommandComponent>>;
  explicit CommandComponent(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

  CommandComponent() {}

public:
};

class AbstractCommand : public virtual ServerObject {
protected:
  friend struct RPCConverter<std::shared_ptr<AbstractCommand>>;
  explicit AbstractCommand(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

  AbstractCommand() {}

public:
  /**   */
  virtual std::shared_ptr<CommandOutput> output();
  /**   */
  virtual void add_dependency(std::shared_ptr<Dependency> const &dependency);
};

class ConnectorOptions : public virtual ServerObject {
protected:
  friend struct RPCConverter<std::shared_ptr<ConnectorOptions>>;
  explicit ConnectorOptions(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

  ConnectorOptions() {}

public:
};

class Path : public virtual ServerObject {
protected:
  friend struct RPCConverter<std::shared_ptr<Path>>;
  explicit Path(ObjectIdentifier o);
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
  static std::shared_ptr<Path> toPath(std::string const &path);
  /**   */
  virtual std::string read_all();
  /**   */
  virtual int64_t get_size();
  /** Get the file path, ignoring the file scheme
  */
  virtual std::string get_path();
  /**   */
  virtual std::string uri();
  /**   */
  virtual std::string toSource();
};

class OARParameters : public LauncherParameters {
protected:
  friend struct RPCConverter<std::shared_ptr<OARParameters>>;
  explicit OARParameters(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

  OARParameters() {}

public:
};

class ParameterFile : public CommandComponent {
protected:
  friend struct RPCConverter<std::shared_ptr<ParameterFile>>;
  explicit ParameterFile(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

public:
  /**   */
  ParameterFile(std::string const &key, std::string const &content);
};

class XPM : public virtual ServerObject {
protected:
  friend struct RPCConverter<std::shared_ptr<XPM>>;
  explicit XPM(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

  XPM() {}

public:
  /** Returns a file relative to the current connector
  */
  virtual std::shared_ptr<Path> file(std::string const &filepath);
  /**   */
  virtual std::string ns();
  /**   */
  virtual bool simulate();
  /** Set the simulate flag: When true, the jobs are not submitted but just output
  */
  virtual bool simulate(bool const &boolean);
  /**   */
  virtual std::shared_ptr<Task> get_task(std::string const &string, std::string const &string_1);
  /** Publish the repository on the web server
  */
  virtual void publish();
  /** Retrieve (or creates) a token resource with a given xpath
com.sun.javafx.binding.StringConstant@3c0be339  */
  static std::shared_ptr<TokenResource> token_resource(std::string const &path);
  /** com.sun.javafx.binding.StringConstant@15ca7889com.sun.javafx.binding.StringConstant@7a675056  */
  static std::shared_ptr<TokenResource> token_resource(std::string const &path, bool const &post_process);
  /** Sets the logger debug level
  */
  virtual void log_level(std::string const &name, std::string const &level);
  /**   */
  virtual std::string get_script_path();
  /** com.sun.javafx.binding.StringConstant@2898ac89  */
  static std::shared_ptr<TokenResource> token(std::string const &path);
};

class Task : public virtual ServerObject {
protected:
  friend struct RPCConverter<std::shared_ptr<Task>>;
  explicit Task(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

  Task() {}

public:
};

class Functions : public virtual ServerObject {
protected:
  friend struct RPCConverter<std::shared_ptr<Functions>>;
  explicit Functions(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

  Functions() {}

public:
  /**   */
  static std::shared_ptr<Path> path(std::shared_ptr<Path> const &uri);
  /** Returns a path object from an URI
  */
  static std::shared_ptr<Path> path(std::string const &uri);
  /** Returns the Path object corresponding to the current script
  */
  static std::shared_ptr<Path> script_file();
  /** Defines a new relationship between a network share and a path on a connector
com.sun.javafx.binding.StringConstant@683dbc2c  */
  static void define_share(std::string const &host, std::string const &share, std::shared_ptr<SingleHostConnector> const &connector, std::string const &path, int32_t const &priority);
  /** Defines the default launcher
  */
  static void set_default_launcher(std::shared_ptr<Launcher> const &launcher);
  /** Include a repository from introspection of a java project
  */
  static void inspect_java_repository(std::shared_ptr<Connector> const &connector, std::vector<std::string> const &string, std::shared_ptr<Path> const &path);
  /** Include a repository from introspection of a JAR file
  */
  static void load_java_repository(std::shared_ptr<Path> const &path);
  /** Include a repository from introspection of a JAR file
  */
  static void load_repository(std::shared_ptr<Path> const &path);
  /** Set the experiment for all future commands
com.sun.javafx.binding.StringConstant@6a2b953e  */
  static void set_experiment(std::string const &identifier, bool const &holdPrevious);
  /**   */
  static void set_workdir(std::shared_ptr<Path> const &path);
  /**   */
  static std::shared_ptr<LocalhostConnector> get_localhost_connector();
  /** Returns the notification URL
  */
  static std::string notification_url();
};

class Namespace : public virtual ServerObject {
protected:
  friend struct RPCConverter<std::shared_ptr<Namespace>>;
  explicit Namespace(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

public:
  /**   */
  Namespace(std::string const &string, std::string const &string_1);
  /**   */
  virtual std::string uri();
};

class JsonTask : public virtual ServerObject {
protected:
  friend struct RPCConverter<std::shared_ptr<JsonTask>>;
  explicit JsonTask(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

  JsonTask() {}

public:
  /**   */
  virtual std::shared_ptr<AbstractCommand> command();
};

class CommandOutput : public CommandComponent {
protected:
  friend struct RPCConverter<std::shared_ptr<CommandOutput>>;
  explicit CommandOutput(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

  CommandOutput() {}

public:
};

class SSHConnector : public SingleHostConnector {
protected:
  friend struct RPCConverter<std::shared_ptr<SSHConnector>>;
  explicit SSHConnector(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

  SSHConnector() {}

public:
  /**   */
  virtual std::string env(std::shared_ptr<Launcher> const &launcher, std::string const &string);
};

class ReadWriteDependency : public Dependency {
protected:
  friend struct RPCConverter<std::shared_ptr<ReadWriteDependency>>;
  explicit ReadWriteDependency(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

public:
  /**   */
  ReadWriteDependency(std::string const &locator);
  /**   */
  ReadWriteDependency(std::shared_ptr<Resource> const &resource);
};

class OARLauncher : public Launcher {
protected:
  friend struct RPCConverter<std::shared_ptr<OARLauncher>>;
  explicit OARLauncher(ObjectIdentifier o);
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

class CommandString : public CommandComponent {
protected:
  friend struct RPCConverter<std::shared_ptr<CommandString>>;
  explicit CommandString(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

public:
  /**   */
  CommandString(std::string const &string);
  /**   */
  virtual std::string toString();
};

class CommandPath : public CommandComponent {
protected:
  friend struct RPCConverter<std::shared_ptr<CommandPath>>;
  explicit CommandPath(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

public:
  /**   */
  CommandPath(std::string const &pathname);
};

class LocalhostConnector : public SingleHostConnector {
protected:
  friend struct RPCConverter<std::shared_ptr<LocalhostConnector>>;
  explicit LocalhostConnector(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

public:
  /**   */
  LocalhostConnector();
  /**   */
  virtual std::string env(std::string const &string);
};

class DirectLauncher : public Launcher {
protected:
  friend struct RPCConverter<std::shared_ptr<DirectLauncher>>;
  explicit DirectLauncher(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

public:
  /**   */
  DirectLauncher(std::shared_ptr<Connector> const &connector);
};

class JavaTaskFactory : public virtual ServerObject {
protected:
  friend struct RPCConverter<std::shared_ptr<JavaTaskFactory>>;
  explicit JavaTaskFactory(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

  JavaTaskFactory() {}

public:
};

class Commands : public AbstractCommand {
protected:
  friend struct RPCConverter<std::shared_ptr<Commands>>;
  explicit Commands(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

public:
  /**   */
  Commands();
  /**   */
  Commands(std::vector<std::shared_ptr<AbstractCommand>> const &abstractCommand);
  /**   */
  virtual void add(std::shared_ptr<AbstractCommand> const &abstractCommand);
};

class Pipe : public CommandComponent {
protected:
  friend struct RPCConverter<std::shared_ptr<Pipe>>;
  explicit Pipe(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

  Pipe() {}

public:
};

class JsonParameterFile : public CommandComponent {
protected:
  friend struct RPCConverter<std::shared_ptr<JsonParameterFile>>;
  explicit JsonParameterFile(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

public:
};

class Module : public virtual ServerObject {
protected:
  friend struct RPCConverter<std::shared_ptr<Module>>;
  explicit Module(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

  Module() {}

public:
};

class CommandLineTask : public Job {
protected:
  friend struct RPCConverter<std::shared_ptr<CommandLineTask>>;
  explicit CommandLineTask(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

  CommandLineTask() {}

public:
  /**   */
  static std::shared_ptr<Resource> submitJob(std::shared_ptr<Path> const &path, std::shared_ptr<AbstractCommand> const &abstractCommand);
};

class TokenResource : public Resource {
protected:
  friend struct RPCConverter<std::shared_ptr<TokenResource>>;
  explicit TokenResource(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

  TokenResource() {}

public:
  /**   */
  virtual int32_t used();
  /**   */
  virtual void set_limit(int32_t const &int_1);
  /**   */
  virtual int32_t getLimit();
};

class SSHOptions : public ConnectorOptions {
protected:
  friend struct RPCConverter<std::shared_ptr<SSHOptions>>;
  explicit SSHOptions(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

public:
  /**   */
  SSHOptions();
  /**   */
  virtual std::string hostname();
  /**   */
  virtual std::shared_ptr<SSHOptions> check_host(bool const &boolean);
  /**   */
  virtual void set_use_ssh_agent(bool const &boolean);
  /**   */
  virtual void set_stream_proxy(std::string const &uri, std::shared_ptr<SSHOptions> const &options);
  /**   */
  virtual void set_stream_proxy(std::shared_ptr<SSHConnector> const &proxy);
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

class Command : public AbstractCommandComponent, public AbstractCommand {
protected:
  friend struct RPCConverter<std::shared_ptr<Command>>;
  explicit Command(ObjectIdentifier o);
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
};

class SubCommand : public CommandComponent {
protected:
  friend struct RPCConverter<std::shared_ptr<SubCommand>>;
  explicit SubCommand(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

  SubCommand() {}

public:
};

} }// xpm::rpc namespace
#endif
