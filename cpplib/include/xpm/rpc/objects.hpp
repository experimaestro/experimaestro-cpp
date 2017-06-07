#ifndef _XPM_RPCOBJECTS_H
#define _XPM_RPCOBJECTS_H

#include <vector>
#include <xpm/rpc/utils.hpp>
#include <xpm/rpc/optional.hpp>

#ifdef SWIG
%shared_ptr(xpm::rpc::AbstractCommand);
%shared_ptr(xpm::rpc::DirectLauncher);
%shared_ptr(xpm::rpc::Command);
%shared_ptr(xpm::rpc::Pipe);
%shared_ptr(xpm::rpc::StringContent);
%shared_ptr(xpm::rpc::Job);
%shared_ptr(xpm::rpc::ConnectorOptions);
%shared_ptr(xpm::rpc::CommandOutput);
%shared_ptr(xpm::rpc::SSHOptions);
%shared_ptr(xpm::rpc::SingleHostConnector);
%shared_ptr(xpm::rpc::ReadWriteDependency);
%shared_ptr(xpm::rpc::CommandString);
%shared_ptr(xpm::rpc::Path);
%shared_ptr(xpm::rpc::Dependency);
%shared_ptr(xpm::rpc::Launcher);
%shared_ptr(xpm::rpc::TokenResource);
%shared_ptr(xpm::rpc::SubCommand);
%shared_ptr(xpm::rpc::CommandPath);
%shared_ptr(xpm::rpc::LauncherParameters);
%shared_ptr(xpm::rpc::CommandLineTask);
%shared_ptr(xpm::rpc::CommandComponent);
%shared_ptr(xpm::rpc::Resource);
%shared_ptr(xpm::rpc::ParameterFile);
%shared_ptr(xpm::rpc::Content);
%shared_ptr(xpm::rpc::ContentsFile);
%shared_ptr(xpm::rpc::PathContent);
%shared_ptr(xpm::rpc::Namespace);
%shared_ptr(xpm::rpc::LocalhostConnector);
%shared_ptr(xpm::rpc::Connector);
%shared_ptr(xpm::rpc::SSHConnector);
%shared_ptr(xpm::rpc::Functions);
%shared_ptr(xpm::rpc::Commands);
%shared_ptr(xpm::rpc::AbstractCommandComponent);
%shared_ptr(xpm::rpc::OARParameters);
%shared_ptr(xpm::rpc::XPM);
%shared_ptr(xpm::rpc::OARLauncher);
#endif
#if defined(SWIGJAVA) && defined(SWIG) 
%nspace xpm::rpc::AbstractCommand;
%nspace xpm::rpc::DirectLauncher;
%nspace xpm::rpc::Command;
%nspace xpm::rpc::Pipe;
%nspace xpm::rpc::StringContent;
%nspace xpm::rpc::Job;
%nspace xpm::rpc::ConnectorOptions;
%nspace xpm::rpc::CommandOutput;
%nspace xpm::rpc::SSHOptions;
%nspace xpm::rpc::SingleHostConnector;
%nspace xpm::rpc::ReadWriteDependency;
%nspace xpm::rpc::CommandString;
%nspace xpm::rpc::Path;
%nspace xpm::rpc::Dependency;
%nspace xpm::rpc::Launcher;
%nspace xpm::rpc::TokenResource;
%nspace xpm::rpc::SubCommand;
%nspace xpm::rpc::CommandPath;
%nspace xpm::rpc::LauncherParameters;
%nspace xpm::rpc::CommandLineTask;
%nspace xpm::rpc::CommandComponent;
%nspace xpm::rpc::Resource;
%nspace xpm::rpc::ParameterFile;
%nspace xpm::rpc::Content;
%nspace xpm::rpc::ContentsFile;
%nspace xpm::rpc::PathContent;
%nspace xpm::rpc::Namespace;
%nspace xpm::rpc::LocalhostConnector;
%nspace xpm::rpc::Connector;
%nspace xpm::rpc::SSHConnector;
%nspace xpm::rpc::Functions;
%nspace xpm::rpc::Commands;
%nspace xpm::rpc::AbstractCommandComponent;
%nspace xpm::rpc::OARParameters;
%nspace xpm::rpc::XPM;
%nspace xpm::rpc::OARLauncher;
#endif
namespace xpm { namespace rpc {


// Pre-declaration
class AbstractCommand;
class DirectLauncher;
class Command;
class Pipe;
class StringContent;
class Job;
class ConnectorOptions;
class CommandOutput;
class SSHOptions;
class SingleHostConnector;
class ReadWriteDependency;
class CommandString;
class Path;
class Dependency;
class Launcher;
class TokenResource;
class SubCommand;
class CommandPath;
class LauncherParameters;
class CommandLineTask;
class CommandComponent;
class Resource;
class ParameterFile;
class Content;
class ContentsFile;
class PathContent;
class Namespace;
class LocalhostConnector;
class Connector;
class SSHConnector;
class Functions;
class Commands;
class AbstractCommandComponent;
class OARParameters;
class XPM;
class OARLauncher;


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
  virtual std::string taskId();
  /**   */
  virtual void taskId(std::string const &string);
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
  static std::shared_ptr<Connector> create(std::string const &string, std::string const &string_1, std::shared_ptr<ConnectorOptions> const &connectorOptions = std::shared_ptr<ConnectorOptions>());
  /**   */
  virtual std::shared_ptr<SingleHostConnector> asSingleHostConnector();
  /**   */
  virtual std::shared_ptr<Launcher> defaultlauncher();
};

class Job : public Resource {
protected:
  friend struct RPCConverter<std::shared_ptr<Job>>;
  explicit Job(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

  Job() {}

public:
  /**   */
  virtual std::shared_ptr<Resource> submit();
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

class LauncherParameters : public virtual ServerObject {
protected:
  friend struct RPCConverter<std::shared_ptr<LauncherParameters>>;
  explicit LauncherParameters(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

  LauncherParameters() {}

public:
};

class Content : public virtual ServerObject {
protected:
  friend struct RPCConverter<std::shared_ptr<Content>>;
  explicit Content(ObjectIdentifier o);
  Content() {}

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
  /** Sets the temporary directory for this launcher
  */
  virtual void set_tmpdir(std::shared_ptr<Path> const &path);
  /** Sets an environment variable and returns the old value (if any)
  */
  virtual std::string env(std::string const &key, std::string const &value);
  /** Gets the value of the environment variable
  */
  virtual std::string env(std::string const &string);
};

class SingleHostConnector : public Connector {
protected:
  friend struct RPCConverter<std::shared_ptr<SingleHostConnector>>;
  explicit SingleHostConnector(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

  SingleHostConnector() {}

public:
};

class ConnectorOptions : public virtual ServerObject {
protected:
  friend struct RPCConverter<std::shared_ptr<ConnectorOptions>>;
  explicit ConnectorOptions(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

  ConnectorOptions() {}

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
  virtual void add_dependency(std::shared_ptr<Dependency> const &dependency);
  /**   */
  virtual std::shared_ptr<CommandOutput> output();
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

class ContentsFile : public CommandComponent {
protected:
  friend struct RPCConverter<std::shared_ptr<ContentsFile>>;
  explicit ContentsFile(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

public:
  /**   */
  ContentsFile(std::string const &key, std::string const &extension);
  /**   */
  virtual void add(std::string const &string);
  /**   */
  virtual void add(std::shared_ptr<Path> const &path);
  /**   */
  virtual void add(std::shared_ptr<Content> const &content);
};

class PathContent : public Content {
protected:
  friend struct RPCConverter<std::shared_ptr<PathContent>>;
  explicit PathContent(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

  PathContent() {}

public:
};

class CommandLineTask : public Job {
protected:
  friend struct RPCConverter<std::shared_ptr<CommandLineTask>>;
  explicit CommandLineTask(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

public:
  /**   */
  CommandLineTask(std::shared_ptr<Path> const &path);
  /**   */
  virtual void setLauncher(std::shared_ptr<Launcher> const &launcher, std::shared_ptr<LauncherParameters> const &launcherParameters);
  /**   */
  virtual void command(std::shared_ptr<AbstractCommand> const &abstractCommand);
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

class CommandPath : public CommandComponent {
protected:
  friend struct RPCConverter<std::shared_ptr<CommandPath>>;
  explicit CommandPath(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

public:
  /**   */
  CommandPath(std::string const &pathname);
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
  virtual void password(std::string const &string);
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
  virtual void username(std::string const &string);
  /**   */
  virtual std::string username();
  /**   */
  virtual void port(int32_t const &int_1);
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

class XPM : public virtual ServerObject {
protected:
  friend struct RPCConverter<std::shared_ptr<XPM>>;
  explicit XPM(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

  XPM() {}

public:
  /** com.sun.javafx.binding.StringConstant@56928307com.sun.javafx.binding.StringConstant@3899782c  */
  static std::shared_ptr<TokenResource> token_resource(std::string const &path, bool const &post_process);
  /** Retrieve (or creates) a token resource with a given xpath
com.sun.javafx.binding.StringConstant@1603cd68  */
  static std::shared_ptr<TokenResource> token_resource(std::string const &path);
  /** com.sun.javafx.binding.StringConstant@4b23c30a  */
  static std::shared_ptr<TokenResource> token(std::string const &path);
  /** Set the simulate flag: When true, the jobs are not submitted but just output
  */
  virtual bool simulate(bool const &boolean);
  /** Sets the logger debug level
  */
  virtual void log_level(std::string const &name, std::string const &level);
  /**   */
  virtual std::string ns();
};

class Namespace : public virtual ServerObject {
protected:
  friend struct RPCConverter<std::shared_ptr<Namespace>>;
  explicit Namespace(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

public:
  /**   */
  Namespace(std::string const &string, optional<std::string> const &string_1 = optional<std::string>());
  /**   */
  virtual std::string uri();
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

class DirectLauncher : public Launcher {
protected:
  friend struct RPCConverter<std::shared_ptr<DirectLauncher>>;
  explicit DirectLauncher(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

public:
  /**   */
  DirectLauncher(std::shared_ptr<Connector> const &connector);
};

class OARParameters : public LauncherParameters {
protected:
  friend struct RPCConverter<std::shared_ptr<OARParameters>>;
  explicit OARParameters(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

  OARParameters() {}

public:
  /**   */
  virtual void setMemory(int32_t const &int_1);
  /**   */
  virtual int32_t getCores();
  /**   */
  virtual void setCores(int32_t const &int_1);
  /**   */
  virtual int32_t getNodes();
  /**   */
  virtual void setNodes(int32_t const &int_1);
  /**   */
  virtual int32_t getMemory();
  /**   */
  virtual int64_t getJobDuration();
  /**   */
  virtual void setJobDuration(int64_t const &long_1);
  /**   */
  virtual int32_t getHosts();
  /**   */
  virtual void setHosts(int32_t const &int_1);
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
  /** Defines a new relationship between a network share and a path on a connector
com.sun.javafx.binding.StringConstant@6581dc0a  */
  static void define_share(std::string const &host, std::string const &share, std::shared_ptr<SingleHostConnector> const &connector, std::string const &path, optional<int32_t> const &priority = optional<int32_t>());
  /** Defines the default launcher
  */
  static void set_default_launcher(std::shared_ptr<Launcher> const &launcher);
  /** Set the experiment for all future commands
com.sun.javafx.binding.StringConstant@39de3d36  */
  static void set_experiment(std::string const &identifier, optional<bool> const &holdPrevious = optional<bool>());
  /**   */
  static void set_workdir(std::shared_ptr<Path> const &path);
  /**   */
  static std::shared_ptr<LocalhostConnector> get_localhost_connector();
  /** Returns the notification URL
  */
  static std::string notification_url();
};

class StringContent : public Content {
protected:
  friend struct RPCConverter<std::shared_ptr<StringContent>>;
  explicit StringContent(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

public:
  /**   */
  StringContent(std::string const &string);
};

class Pipe : public CommandComponent {
protected:
  friend struct RPCConverter<std::shared_ptr<Pipe>>;
  explicit Pipe(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

  Pipe() {}

public:
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
  virtual std::string toSource();
  /**   */
  virtual std::string read_all();
  /**   */
  virtual int64_t get_size();
  /** Get the file path, ignoring the file scheme
  */
  virtual std::string get_path();
  /**   */
  virtual std::string uri();
};

class SSHConnector : public SingleHostConnector {
protected:
  friend struct RPCConverter<std::shared_ptr<SSHConnector>>;
  explicit SSHConnector(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

public:
  /**   */
  SSHConnector(std::string const &string, std::string const &string_1, std::shared_ptr<ConnectorOptions> const &connectorOptions);
  /**   */
  virtual std::string env(std::shared_ptr<Launcher> const &launcher, optional<std::string> const &string = optional<std::string>());
};

class CommandOutput : public CommandComponent {
protected:
  friend struct RPCConverter<std::shared_ptr<CommandOutput>>;
  explicit CommandOutput(ObjectIdentifier o);
  virtual std::string const &__name__() const override;

  CommandOutput() {}

public:
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
