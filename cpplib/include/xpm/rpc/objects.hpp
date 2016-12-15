#ifndef _XPM_RPCOBJECTS_H
#define _XPM_RPCOBJECTS_H

#include <vector>
#include <xpm/rpc/utils.hpp>

#ifdef SWIG
%nspace xpm::rpc::CommandComponent;
%shared_ptr(xpm::rpc::CommandComponent);
%nspace xpm::rpc::Connector;
%shared_ptr(xpm::rpc::Connector);
%nspace xpm::rpc::Functions;
%shared_ptr(xpm::rpc::Functions);
%nspace xpm::rpc::LocalhostConnector;
%shared_ptr(xpm::rpc::LocalhostConnector);
%nspace xpm::rpc::ConnectorOptions;
%shared_ptr(xpm::rpc::ConnectorOptions);
%nspace xpm::rpc::JsonParameterFile;
%shared_ptr(xpm::rpc::JsonParameterFile);
%nspace xpm::rpc::ParameterFile;
%shared_ptr(xpm::rpc::ParameterFile);
%nspace xpm::rpc::Command;
%shared_ptr(xpm::rpc::Command);
%nspace xpm::rpc::AbstractCommandComponent;
%shared_ptr(xpm::rpc::AbstractCommandComponent);
%nspace xpm::rpc::ReadWriteDependency;
%shared_ptr(xpm::rpc::ReadWriteDependency);
%nspace xpm::rpc::Pipe;
%shared_ptr(xpm::rpc::Pipe);
%nspace xpm::rpc::DirectLauncher;
%shared_ptr(xpm::rpc::DirectLauncher);
%nspace xpm::rpc::CommandPath;
%shared_ptr(xpm::rpc::CommandPath);
%nspace xpm::rpc::SingleHostConnector;
%shared_ptr(xpm::rpc::SingleHostConnector);
%nspace xpm::rpc::XPM;
%shared_ptr(xpm::rpc::XPM);
%nspace xpm::rpc::LauncherParameters;
%shared_ptr(xpm::rpc::LauncherParameters);
%nspace xpm::rpc::Launcher;
%shared_ptr(xpm::rpc::Launcher);
%nspace xpm::rpc::Path;
%shared_ptr(xpm::rpc::Path);
%nspace xpm::rpc::Namespace;
%shared_ptr(xpm::rpc::Namespace);
%nspace xpm::rpc::CommandLineTask;
%shared_ptr(xpm::rpc::CommandLineTask);
%nspace xpm::rpc::TokenResource;
%shared_ptr(xpm::rpc::TokenResource);
%nspace xpm::rpc::SSHOptions;
%shared_ptr(xpm::rpc::SSHOptions);
%nspace xpm::rpc::OARParameters;
%shared_ptr(xpm::rpc::OARParameters);
%nspace xpm::rpc::Job;
%shared_ptr(xpm::rpc::Job);
%nspace xpm::rpc::CommandString;
%shared_ptr(xpm::rpc::CommandString);
%nspace xpm::rpc::AbstractCommand;
%shared_ptr(xpm::rpc::AbstractCommand);
%nspace xpm::rpc::Resource;
%shared_ptr(xpm::rpc::Resource);
%nspace xpm::rpc::OARLauncher;
%shared_ptr(xpm::rpc::OARLauncher);
%nspace xpm::rpc::Dependency;
%shared_ptr(xpm::rpc::Dependency);
%nspace xpm::rpc::Commands;
%shared_ptr(xpm::rpc::Commands);
%nspace xpm::rpc::CommandOutput;
%shared_ptr(xpm::rpc::CommandOutput);
%nspace xpm::rpc::SSHConnector;
%shared_ptr(xpm::rpc::SSHConnector);
%nspace xpm::rpc::SubCommand;
%shared_ptr(xpm::rpc::SubCommand);
#endif
namespace xpm { namespace rpc {


// Pre-declaration
class CommandComponent;
class Connector;
class Functions;
class LocalhostConnector;
class ConnectorOptions;
class JsonParameterFile;
class ParameterFile;
class Command;
class AbstractCommandComponent;
class ReadWriteDependency;
class Pipe;
class DirectLauncher;
class CommandPath;
class SingleHostConnector;
class XPM;
class LauncherParameters;
class Launcher;
class Path;
class Namespace;
class CommandLineTask;
class TokenResource;
class SSHOptions;
class OARParameters;
class Job;
class CommandString;
class AbstractCommand;
class Resource;
class OARLauncher;
class Dependency;
class Commands;
class CommandOutput;
class SSHConnector;
class SubCommand;


// Classes
class AbstractCommandComponent : public virtual ServerObject {
protected:
  friend struct RPCConverter<std::shared_ptr<AbstractCommandComponent>>;
  explicit AbstractCommandComponent(ObjectIdentifier o);
  AbstractCommandComponent() {}

public:
};

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

class AbstractCommand : public virtual ServerObject {
protected:
  friend struct RPCConverter<std::shared_ptr<AbstractCommand>>;
  explicit AbstractCommand(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

  AbstractCommand() {}

public:
  /**   */
  virtual void add_dependency(std::shared_ptr<Dependency> const &dependency);
  /**   */
  virtual std::shared_ptr<CommandOutput> output();
};

class SingleHostConnector : public Connector {
protected:
  friend struct RPCConverter<std::shared_ptr<SingleHostConnector>>;
  explicit SingleHostConnector(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

  SingleHostConnector() {}

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
  /**   */
  virtual void set_notification_url(std::string const &string);
  /** Gets the value of the environment variable
  */
  virtual std::string env(std::string const &string);
  /** Sets an environment variable and returns the old value (if any)
  */
  virtual std::string env(std::string const &key, std::string const &value);
  /** Sets the temporary directory for this launcher
  */
  virtual void set_tmpdir(std::shared_ptr<Path> const &path);
};

class ConnectorOptions : public virtual ServerObject {
protected:
  friend struct RPCConverter<std::shared_ptr<ConnectorOptions>>;
  explicit ConnectorOptions(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

  ConnectorOptions() {}

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

class LauncherParameters : public virtual ServerObject {
protected:
  friend struct RPCConverter<std::shared_ptr<LauncherParameters>>;
  explicit LauncherParameters(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

  LauncherParameters() {}

public:
};

class Job : public Resource {
protected:
  friend struct RPCConverter<std::shared_ptr<Job>>;
  explicit Job(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

  Job() {}

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

class SubCommand : public CommandComponent {
protected:
  friend struct RPCConverter<std::shared_ptr<SubCommand>>;
  explicit SubCommand(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

  SubCommand() {}

public:
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

class XPM : public virtual ServerObject {
protected:
  friend struct RPCConverter<std::shared_ptr<XPM>>;
  explicit XPM(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

  XPM() {}

public:
  /**   */
  virtual std::string ns();
  /** Retrieve (or creates) a token resource with a given xpath
com.sun.javafx.binding.StringConstant@5c90e579  */
  static std::shared_ptr<TokenResource> token_resource(std::string const &path);
  /** com.sun.javafx.binding.StringConstant@58ea606ccom.sun.javafx.binding.StringConstant@6f45df59  */
  static std::shared_ptr<TokenResource> token_resource(std::string const &path, bool const &post_process);
  /** Sets the logger debug level
  */
  virtual void log_level(std::string const &name, std::string const &level);
  /** Set the simulate flag: When true, the jobs are not submitted but just output
  */
  virtual bool simulate(bool const &boolean);
  /**   */
  virtual bool simulate();
  /** com.sun.javafx.binding.StringConstant@31f9b85e  */
  static std::shared_ptr<TokenResource> token(std::string const &path);
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
  virtual int64_t get_size();
  /**   */
  virtual std::string read_all();
  /** Get the file path, ignoring the file scheme
  */
  virtual std::string get_path();
  /**   */
  virtual std::string uri();
  /**   */
  virtual std::string toSource();
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

class ParameterFile : public CommandComponent {
protected:
  friend struct RPCConverter<std::shared_ptr<ParameterFile>>;
  explicit ParameterFile(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

public:
  /**   */
  ParameterFile(std::string const &key, std::string const &content);
};

class TokenResource : public Resource {
protected:
  friend struct RPCConverter<std::shared_ptr<TokenResource>>;
  explicit TokenResource(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

  TokenResource() {}

public:
  /**   */
  virtual void set_limit(int32_t const &int_1);
  /**   */
  virtual int32_t used();
  /**   */
  virtual int32_t getLimit();
};

class JsonParameterFile : public CommandComponent {
protected:
  friend struct RPCConverter<std::shared_ptr<JsonParameterFile>>;
  explicit JsonParameterFile(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

public:
};

class Functions : public virtual ServerObject {
protected:
  friend struct RPCConverter<std::shared_ptr<Functions>>;
  explicit Functions(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

  Functions() {}

public:
  /** Returns a path object from an URI
  */
  static std::shared_ptr<Path> path(std::string const &uri);
  /**   */
  static std::shared_ptr<Path> path(std::shared_ptr<Path> const &uri);
  /** Defines a new relationship between a network share and a path on a connector
com.sun.javafx.binding.StringConstant@2f465398  */
  static void define_share(std::string const &host, std::string const &share, std::shared_ptr<SingleHostConnector> const &connector, std::string const &path, int32_t const &priority);
  /** Defines the default launcher
  */
  static void set_default_launcher(std::shared_ptr<Launcher> const &launcher);
  /** Set the experiment for all future commands
com.sun.javafx.binding.StringConstant@3098cf3b  */
  static void set_experiment(std::string const &identifier, bool const &holdPrevious);
  /**   */
  static void set_workdir(std::shared_ptr<Path> const &path);
  /**   */
  static std::shared_ptr<LocalhostConnector> get_localhost_connector();
  /** Returns the notification URL
  */
  static std::string notification_url();
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

class DirectLauncher : public Launcher {
protected:
  friend struct RPCConverter<std::shared_ptr<DirectLauncher>>;
  explicit DirectLauncher(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

public:
  /**   */
  DirectLauncher(std::shared_ptr<Connector> const &connector);
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

class Command : public AbstractCommandComponent, public AbstractCommand {
protected:
  friend struct RPCConverter<std::shared_ptr<Command>>;
  explicit Command(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

public:
  /**   */
  Command();
  /**   */
  virtual void add(std::vector<std::shared_ptr<AbstractCommandComponent>> const &abstractCommandComponent);
  /**   */
  virtual void add(std::vector<std::string> const &string);
  /**   */
  virtual void add_subcommand(std::shared_ptr<Commands> const &commands);
};

class CommandOutput : public CommandComponent {
protected:
  friend struct RPCConverter<std::shared_ptr<CommandOutput>>;
  explicit CommandOutput(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

  CommandOutput() {}

public:
};

class Pipe : public CommandComponent {
protected:
  friend struct RPCConverter<std::shared_ptr<Pipe>>;
  explicit Pipe(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

  Pipe() {}

public:
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

class OARParameters : public LauncherParameters {
protected:
  friend struct RPCConverter<std::shared_ptr<OARParameters>>;
  explicit OARParameters(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

  OARParameters() {}

public:
};

} }// xpm::rpc namespace
#endif
