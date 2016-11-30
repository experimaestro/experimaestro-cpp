#include <xpm/rpc/objects.hpp>

namespace xpm {
std::shared_ptr<Launcher> Connector::default_launcher() {
  json params = json::object();
  return rpc2cpp<std::shared_ptr<Launcher>>(__call__(params));
}

static std::shared_ptr<Connector> Connector::create(std::string const &string, std::string const &string_1, std::shared_ptr<ConnectorOptions> const &connectorOptions) {
  json params = json::object();
  params["string"] = cpp2rpc(string);
  params["string_1"] = cpp2rpc(string_1);
  params["connectorOptions"] = cpp2rpc(connectorOptions);
  return rpc2cpp<std::shared_ptr<Connector>>(__static_call__(params));
}

std::string Launcher::environment(std::string const &key) {
  json params = json::object();
  params["key"] = cpp2rpc(key);
  return rpc2cpp<std::string>(__call__(params));
}

void Launcher::set_notification_url(std::string const &string) {
  json params = json::object();
  params["string"] = cpp2rpc(string);
  __call__(params);
}

std::string Launcher::env(std::string const &string) {
  json params = json::object();
  params["string"] = cpp2rpc(string);
  return rpc2cpp<std::string>(__call__(params));
}

std::string Launcher::env(std::string const &key, std::string const &value) {
  json params = json::object();
  params["key"] = cpp2rpc(key);
  params["value"] = cpp2rpc(value);
  return rpc2cpp<std::string>(__call__(params));
}

std::shared_ptr<LauncherParameters> Launcher::parameters() {
  json params = json::object();
  return rpc2cpp<std::shared_ptr<LauncherParameters>>(__call__(params));
}

std::string Resource::toString() {
  json params = json::object();
  return rpc2cpp<std::string>(__call__(params));
}

bool Json::is_array() {
  json params = json::object();
  return rpc2cpp<bool>(__call__(params));
}

std::string Json::get_descriptor() {
  json params = json::object();
  return rpc2cpp<std::string>(__call__(params));
}

std::string Json::toSource() {
  json params = json::object();
  return rpc2cpp<std::string>(__call__(params));
}

std::shared_ptr<ParameterFile> Json::as_parameter_file(std::string const &string, std::shared_ptr<SingleHostConnector> const &singleHostConnector) {
  json params = json::object();
  params["string"] = cpp2rpc(string);
  params["singleHostConnector"] = cpp2rpc(singleHostConnector);
  return rpc2cpp<std::shared_ptr<ParameterFile>>(__call__(params));
}

bool Json::isSimple() {
  json params = json::object();
  return rpc2cpp<bool>(__call__(params));
}

std::shared_ptr<JsonObject> Json::as_object() {
  json params = json::object();
  return rpc2cpp<std::shared_ptr<JsonObject>>(__call__(params));
}

std::shared_ptr<Json> Json::copy(bool const &boolean) {
  json params = json::object();
  params["boolean"] = cpp2rpc(boolean);
  return rpc2cpp<std::shared_ptr<Json>>(__call__(params));
}

std::shared_ptr<Json> Json::copy() {
  json params = json::object();
  return rpc2cpp<std::shared_ptr<Json>>(__call__(params));
}

bool Json::is_object() {
  json params = json::object();
  return rpc2cpp<bool>(__call__(params));
}

std::string LocalhostConnector::env(std::string const &string) {
  json params = json::object();
  params["string"] = cpp2rpc(string);
  return rpc2cpp<std::string>(__call__(params));
}

JsonParameterFile::JsonParameterFile(std::string const &string, std::shared_ptr<Json> const &json) {
  json params = json::object();
  params["string"] = cpp2rpc(string);
  params["json"] = cpp2rpc(json);
  __set__(__static_call__(params));
}

std::string JsonString::toString() {
  json params = json::object();
  return rpc2cpp<std::string>(__call__(params));
}

Tasks::Tasks() {
  json params = json::object();
  __set__(__static_call__(params));
}

SSHOptions::SSHOptions() {
  json params = json::object();
  __set__(__static_call__(params));
}

std::string SSHOptions::hostname() {
  json params = json::object();
  return rpc2cpp<std::string>(__call__(params));
}

void SSHOptions::hostname(std::string const &string) {
  json params = json::object();
  params["string"] = cpp2rpc(string);
  __call__(params);
}

void SSHOptions::set_stream_proxy(std::string const &uri, std::shared_ptr<SSHOptions> const &options) {
  json params = json::object();
  params["uri"] = cpp2rpc(uri);
  params["options"] = cpp2rpc(options);
  __call__(params);
}

void SSHOptions::set_stream_proxy(std::shared_ptr<SSHConnector> const &proxy) {
  json params = json::object();
  params["proxy"] = cpp2rpc(proxy);
  __call__(params);
}

void SSHOptions::password(std::string const &string) {
  json params = json::object();
  params["string"] = cpp2rpc(string);
  __call__(params);
}

void SSHOptions::port(int32_t const &int_1) {
  json params = json::object();
  params["int_1"] = cpp2rpc(int_1);
  __call__(params);
}

void SSHOptions::set_use_ssh_agent(bool const &boolean) {
  json params = json::object();
  params["boolean"] = cpp2rpc(boolean);
  __call__(params);
}

std::shared_ptr<SSHOptions> SSHOptions::check_host(bool const &boolean) {
  json params = json::object();
  params["boolean"] = cpp2rpc(boolean);
  return rpc2cpp<std::shared_ptr<SSHOptions>>(__call__(params));
}

void SSHOptions::username(std::string const &string) {
  json params = json::object();
  params["string"] = cpp2rpc(string);
  __call__(params);
}

std::string SSHOptions::username() {
  json params = json::object();
  return rpc2cpp<std::string>(__call__(params));
}

Command::Command() {
  json params = json::object();
  __set__(__static_call__(params));
}

void Command::add(std::vector<std::string> const &string) {
  json params = json::object();
  params["string"] = cpp2rpc(string);
  __call__(params);
}

DirectLauncher::DirectLauncher(std::shared_ptr<Connector> const &connector) {
  json params = json::object();
  params["connector"] = cpp2rpc(connector);
  __set__(__static_call__(params));
}

std::string SSHConnector::env(std::shared_ptr<Launcher> const &launcher, std::string const &string) {
  json params = json::object();
  params["launcher"] = cpp2rpc(launcher);
  params["string"] = cpp2rpc(string);
  return rpc2cpp<std::string>(__call__(params));
}

Namespace::Namespace(std::string const &string, std::string const &string_1) {
  json params = json::object();
  params["string"] = cpp2rpc(string);
  params["string_1"] = cpp2rpc(string_1);
  __set__(__static_call__(params));
}

std::shared_ptr<ScriptingLogger> XPM::get_logger(std::string const &string) {
  json params = json::object();
  params["string"] = cpp2rpc(string);
  return rpc2cpp<std::shared_ptr<ScriptingLogger>>(__call__(params));
}

std::shared_ptr<Path> XPM::get_script_file() {
  json params = json::object();
  return rpc2cpp<std::shared_ptr<Path>>(__call__(params));
}

std::string XPM::ns() {
  json params = json::object();
  return rpc2cpp<std::string>(__call__(params));
}

std::shared_ptr<ScriptingLogger> XPM::logger(std::string const &string) {
  json params = json::object();
  params["string"] = cpp2rpc(string);
  return rpc2cpp<std::shared_ptr<ScriptingLogger>>(__call__(params));
}

void XPM::log_level(std::string const &name, std::string const &level) {
  json params = json::object();
  params["name"] = cpp2rpc(name);
  params["level"] = cpp2rpc(level);
  __call__(params);
}

static std::shared_ptr<TokenResource> XPM::token(std::string const &path) {
  json params = json::object();
  params["path"] = cpp2rpc(path);
  return rpc2cpp<std::shared_ptr<TokenResource>>(__static_call__(params));
}

std::string XPM::get_script_path() {
  json params = json::object();
  return rpc2cpp<std::string>(__call__(params));
}

void XPM::publish() {
  json params = json::object();
  __call__(params);
}

static std::shared_ptr<TokenResource> XPM::token_resource(std::string const &path) {
  json params = json::object();
  params["path"] = cpp2rpc(path);
  return rpc2cpp<std::shared_ptr<TokenResource>>(__static_call__(params));
}

static std::shared_ptr<TokenResource> XPM::token_resource(std::string const &path, bool const &post_process) {
  json params = json::object();
  params["path"] = cpp2rpc(path);
  params["post_process"] = cpp2rpc(post_process);
  return rpc2cpp<std::shared_ptr<TokenResource>>(__static_call__(params));
}

bool XPM::simulate() {
  json params = json::object();
  return rpc2cpp<bool>(__call__(params));
}

bool XPM::simulate(bool const &boolean) {
  json params = json::object();
  params["boolean"] = cpp2rpc(boolean);
  return rpc2cpp<bool>(__call__(params));
}

std::shared_ptr<Task> XPM::get_task(std::string const &string, std::string const &string_1) {
  json params = json::object();
  params["string"] = cpp2rpc(string);
  params["string_1"] = cpp2rpc(string_1);
  return rpc2cpp<std::shared_ptr<Task>>(__call__(params));
}

void JsonObject::FIELDS(std::string const &string, std::shared_ptr<Json> const &json) {
  json params = json::object();
  params["string"] = cpp2rpc(string);
  params["json"] = cpp2rpc(json);
  __call__(params);
}

std::shared_ptr<Json> JsonObject::FIELDS(std::string const &string) {
  json params = json::object();
  params["string"] = cpp2rpc(string);
  return rpc2cpp<std::shared_ptr<Json>>(__call__(params));
}

int32_t JsonArray::LENGTH() {
  json params = json::object();
  return rpc2cpp<int32_t>(__call__(params));
}

std::shared_ptr<Json> JsonArray::FIELDS(int32_t const &int_1) {
  json params = json::object();
  params["int_1"] = cpp2rpc(int_1);
  return rpc2cpp<std::shared_ptr<Json>>(__call__(params));
}

std::string JsonArray::join(std::string const &string) {
  json params = json::object();
  params["string"] = cpp2rpc(string);
  return rpc2cpp<std::string>(__call__(params));
}

int32_t TokenResource::getLimit() {
  json params = json::object();
  return rpc2cpp<int32_t>(__call__(params));
}

void TokenResource::set_limit(int32_t const &int_1) {
  json params = json::object();
  params["int_1"] = cpp2rpc(int_1);
  __call__(params);
}

int32_t TokenResource::used() {
  json params = json::object();
  return rpc2cpp<int32_t>(__call__(params));
}

OARLauncher::OARLauncher(std::shared_ptr<Connector> const &connector) {
  json params = json::object();
  params["connector"] = cpp2rpc(connector);
  __set__(__static_call__(params));
}

std::shared_ptr<LauncherParameters> OARLauncher::parameters() {
  json params = json::object();
  return rpc2cpp<std::shared_ptr<LauncherParameters>>(__call__(params));
}

std::shared_ptr<OARParameters> OARLauncher::parameters() {
  json params = json::object();
  return rpc2cpp<std::shared_ptr<OARParameters>>(__call__(params));
}

std::shared_ptr<LauncherParameters> OARLauncher::parameters() {
  json params = json::object();
  return rpc2cpp<std::shared_ptr<LauncherParameters>>(__call__(params));
}

void OARLauncher::use_notify(bool const &boolean) {
  json params = json::object();
  params["boolean"] = cpp2rpc(boolean);
  __call__(params);
}

void OARLauncher::email(std::string const &string) {
  json params = json::object();
  params["string"] = cpp2rpc(string);
  __call__(params);
}

std::string JsonPath::uri() {
  json params = json::object();
  return rpc2cpp<std::string>(__call__(params));
}

}
