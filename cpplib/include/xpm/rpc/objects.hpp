#ifndef _XPM_RPCOBJECTS_H
#define _XPM_RPCOBJECTS_H

#include <vector>
#include <xpm/rpc/utils.hpp>

namespace xpm {
namespace rpc {
// Pre-declaration
class Dependency;
class LauncherParameters;
class Json;
class SSHConnector;
class ObjectPropertyReference;
class Path;
class JavaTaskFactory;
class LocalhostConnector;
class ReadWriteDependency;
class ScriptingList;
class JsonNull;
class DirectLauncher;
class SubCommand;
class Module;
class Connector;
class JsonResource;
class JsonBoolean;
class CommandOutput;
class Namespace;
class ParameterFile;
class ConnectorOptions;
class JsonTask;
class ScriptingLogger;
class Task;
class JsonParameterFile;
class Job;
class Command;
class SSHOptions;
class JsonObject;
class Resource;
class Pipe;
class JsonReal;
class OARLauncher;
class JsonArray;
class ScriptingMap;
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
 protected:
  virtual std::string const &__name__() const override;

 public:
  /**   */
  std::string resolve(std::shared_ptr<Path> const &path);
  /**   */
  static std::shared_ptr<Connector> create(std::string const &string,
                                           std::string const &string_1,
                                           std::shared_ptr<ConnectorOptions> const &connectorOptions);
  /**   */
  std::shared_ptr<Launcher> default_launcher();
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

class LauncherParameters : public ServerObject {
 protected:
  virtual std::string const &__name__() const override;

 public:
};

class SingleHostConnector : public Connector {
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
  std::shared_ptr<JsonObject> as_object();
  /**   */
  bool is_object();
  /**   */
  std::string toSource();
  /**   */
  bool isSimple();
  /**   */
  bool is_array();
  /**   */
  std::string get_descriptor();
  /** Creates a parameter file from this JSON
  */
  std::shared_ptr<ParameterFile> as_parameter_file(std::string const &string,
                                                   std::shared_ptr<SingleHostConnector> const &singleHostConnector);
};

class ConnectorOptions : public ServerObject {
 protected:
  virtual std::string const &__name__() const override;

 public:
};

class Dependency : public ServerObject {
 protected:
  virtual std::string const &__name__() const override;

 public:
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

class JsonReal : public Json {
 protected:
  virtual std::string const &__name__() const override;

 public:
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

class ScriptingLogger : public ServerObject {
 protected:
  virtual std::string const &__name__() const override;

 public:
  /** Sets the level
  */
  void set_level(std::string const &level);
};

class SSHConnector : public SingleHostConnector {
 protected:
  virtual std::string const &__name__() const override;

 public:
  /**   */
  std::string env(std::shared_ptr<Launcher> const &launcher, std::string const &string);
};

class LocalhostConnector : public SingleHostConnector {
 protected:
  virtual std::string const &__name__() const override;

 public:
  /**   */
  std::string env(std::string const &string);
};

class ObjectPropertyReference : public ServerObject {
 protected:
  virtual std::string const &__name__() const override;

 public:
};

class Pipe : public ServerObject {
 protected:
  virtual std::string const &__name__() const override;

 public:
};

class Command : public ServerObject {
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

class Job : public Resource {
 protected:
  virtual std::string const &__name__() const override;

 public:
};

class ScriptingMap : public ServerObject {
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
  std::string uri();
  /**   */
  std::string toSource();
  /**   */
  std::string read_all();
  /**   */
  int64_t get_size();
  /** Find all the matching files within this folder
com.sun.javafx.binding.StringConstant@1018bde2  */
  std::shared_ptr<JsonArray> find_matching_files(std::string const &regexp);
  /** Get the file path, ignoring the file scheme
  */
  std::string get_path();
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
  void set_use_ssh_agent(bool const &boolean);
  /**   */
  void set_stream_proxy(std::string const &uri, std::shared_ptr<SSHOptions> const &options);
  /**   */
  void set_stream_proxy(std::shared_ptr<SSHConnector> const &proxy);
  /**   */
  void hostname(std::string const &string);
  /**   */
  std::shared_ptr<SSHOptions> check_host(bool const &boolean);
  /**   */
  void port(int32_t const &int_1);
  /**   */
  void username(std::string const &string);
  /**   */
  void password(std::string const &string);
  /**   */
  std::string username();
};

class Module : public ServerObject {
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

class JsonNull : public Json {
 protected:
  virtual std::string const &__name__() const override;

 public:
};

class TokenResource : public Resource {
 protected:
  virtual std::string const &__name__() const override;

 public:
  /**   */
  int32_t used();
  /**   */
  void set_limit(int32_t const &int_1);
  /**   */
  int32_t getLimit();
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

class ParameterFile : public ServerObject {
 protected:
  virtual std::string const &__name__() const override;

 public:
};

class CommandOutput : public ServerObject {
 protected:
  virtual std::string const &__name__() const override;

 public:
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

class XPM : public ServerObject {
 protected:
  virtual std::string const &__name__() const override;

 public:
  /** Returns a file relative to the current connector
  */
  std::shared_ptr<Path> file(std::string const &filepath);
  /**   */
  std::string ns();
  /** Set the simulate flag: When true, the jobs are not submitted but just output
  */
  bool simulate(bool const &boolean);
  /**   */
  bool simulate();
  /**   */
  std::shared_ptr<Task> get_task(std::string const &string, std::string const &string_1);
  /** com.sun.javafx.binding.StringConstant@757acd7bcom.sun.javafx.binding.StringConstant@36b4fe2a  */
  static std::shared_ptr<TokenResource> token_resource(std::string const &path, bool const &post_process);
  /** Retrieve (or creates) a token resource with a given xpath
com.sun.javafx.binding.StringConstant@574b560f  */
  static std::shared_ptr<TokenResource> token_resource(std::string const &path);
  /** Sets the logger debug level
  */
  void log_level(std::string const &name, std::string const &level);
  /**   */
  std::string get_script_path();
  /** Publish the repository on the web server
  */
  void publish();
  /** com.sun.javafx.binding.StringConstant@343570b7  */
  static std::shared_ptr<TokenResource> token(std::string const &path);
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

class JsonBoolean : public Json {
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

class SubCommand : public ServerObject {
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

class JsonTask : public Json {
 protected:
  virtual std::string const &__name__() const override;

 public:
  /**   */
  std::shared_ptr<JsonObject> json();
};

class JsonResource : public Json {
 protected:
  virtual std::string const &__name__() const override;

 public:
};

class ReadWriteDependency : public Dependency {
 protected:
  virtual std::string const &__name__() const override;

 public:
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
}
} // xpm namespace
#endif
