#include <csignal>
#include <condition_variable>
#include <ctime>
#include <chrono>

#include <xpm/workspace.hpp>
#include <xpm/commandline.hpp>

#include <xpm/xpm.hpp>
#include <xpm/json.hpp>
#include <xpm/launchers/launchers.hpp>
#include <xpm/connectors/connectors.hpp>

#include <xpm/rpc/servercontext.hpp>
#include <xpm/rpc/server.hpp>

#include <spdlog/fmt/fmt.h>
#include <__xpm/common.hpp>
#include <__xpm/scriptbuilder.hpp>

DEFINE_LOGGER("xpm.workspace");



namespace {
  // Current workspace
  std::shared_ptr<xpm::Workspace> CURRENT_WORKSPACE;
  
  // Use to notify a changed in job status
  std::mutex JOB_CHANGED_MUTEX;
  std::condition_variable JOB_CHANGED;


  /**
   *
   * Comments about the locking:
   * 
   * (1) submit: [check dependencies]
   * (2) start: [acquire resources] - run - [release resources]
   */
  std::mutex WORKSPACE_MUTEX;

  enum class ExitMode {
    NONE,
    WAITING,
    STOPPING
  };

  ExitMode registerExitHandler();
  
  /** 
   * Exit signal 
   * 
   * The policy is to stop launching jobs first
   * If two SIGINTs are received in a short interval, then exit ASAP
   */
  ExitMode exitMode = ExitMode::NONE;
  static const auto DOUBLE_EXIT_INTERVAL = std::chrono::seconds(5);
  auto lastSignal = std::chrono::system_clock::now() - DOUBLE_EXIT_INTERVAL;

  void sigexitHandler(int) {
    switch(exitMode) {
      case ExitMode::NONE: 
        exitMode = ExitMode::WAITING; 
        lastSignal = std::chrono::system_clock::now();
        LOGGER->warn("Received SIGINT signal: not launching any other process. Press control-C again to exit.");
        break;
      case ExitMode::WAITING:
        if (lastSignal < (std::chrono::system_clock::now() + DOUBLE_EXIT_INTERVAL)) {
          exitMode = ExitMode::STOPPING;
          JOB_CHANGED.notify_all();
          LOGGER->warn("Will stop as soon as possible");
        } else {
          LOGGER->warn("Received SIGINT signal: not launching any other process. Press control-C again to exit.");
          lastSignal = std::chrono::system_clock::now();
        }
        break;
      case ExitMode::STOPPING:
        LOGGER->warn("Already stopping... please wait a bit!");
        break;

    }
  }

  ExitMode registerExitHandler() {
    LOGGER->info("Registered exit signal handler");
    std::signal(SIGINT, &sigexitHandler);
    return ExitMode::NONE;
  }

}

namespace xpm {

std::ostream & operator<<(std::ostream &out, DependencyStatus s) {
  switch(s) {
    case DependencyStatus::OK: return out << "OK";
    case DependencyStatus::WAIT: return out << "WAIT";
    case DependencyStatus::FAIL: return out << "FAIL";
  }
  return out << "?";
}

PathTransformer EXIT_CODE_PATH = [](const Path &path) { return path.withExtension("code"); };
PathTransformer LOCK_PATH = [](const Path &path) { return path.withExtension("lock"); };
PathTransformer LOCK_START_PATH = [](const Path &path) { return path.withExtension("lock.start"); };
PathTransformer DONE_PATH = [](const Path &path) { return path.withExtension("done"); };
PathTransformer PID_PATH = [](const Path &path) { return path.withExtension("pid"); };

// --- Dependency

Dependency::Dependency(ptr<Resource> const & origin) : 
  _origin(origin), _oldStatus(DependencyStatus::WAIT) {}
Dependency::~Dependency() {
  if (_origin) {
    _origin->removeDependent(shared_from_this());
  }
}

void Dependency::output(std::ostream &out) const {
  out << fmt::format("Dep[{} -> {}]", _origin, _target);
}

void Dependency::target(ptr<Resource> const &resource) { _target = resource; }

void Dependency::check() {
  if (_currentLock.lock()) return;
  DependencyStatus s = status();
  if (s != _oldStatus) {
    LOGGER->info("Dependency {} is {} (was: {})", *this, s, _oldStatus);
    _target->dependencyChanged(*this, _oldStatus, s);
    _oldStatus = s;
  }
}


std::shared_ptr<Lock> Dependency::lock() {
  auto current = _currentLock.lock();
  if (!current) {
    _currentLock = current = _lock(); 
  }
  return current;
}


// --- Resource

Resource::Resource() {
}
Resource::~Resource() {}
void Resource::init() {}

void Resource::addDependent(ptr<Dependency> const & dependency) {
  _dependents.push_back(dependency);
}

void Resource::removeDependent(ptr<Dependency> const & dependency) {
  for(auto it = _dependents.begin(); it != _dependents.end(); ++it) {
    auto ptr= it->lock();
    if (!ptr) {
      _dependents.erase(it);
    } else if (ptr == dependency) {
      _dependents.erase(it);
      return;
    }
  }
}


void Resource::dependencyChanged(Dependency &dependency,
                                DependencyStatus from, DependencyStatus to) {
  throw assertion_error(
      "A resource cannot handle a change in dependency directly");
}

void Resource::output(std::ostream &out) const {
  // do nothing
}


//
// --- Token
//

class CounterDependency : public Dependency {
  /// Counter token lock
  struct Lock : public xpm::Lock {
    ptr<CounterDependency> dependency;
    Lock(ptr<CounterDependency> const & dependency) : dependency(dependency) {
      dependency->l_acquire();
    }

    virtual ~Lock() {
      if (!this->detached()) {
        dependency->release();
      }
    }
  };

  // Dependency should be locked
  void l_acquire() {
      if (_value + _counter->_usedTokens > _counter->_limit) {
        throw lock_error("Not enough tokens");
      } 
      _counter->_usedTokens += _value;
      LOGGER->info("Acquire: used tokens {}/{}", _counter->_usedTokens, _counter->_limit);
  }

  void release() {
    {
      // Lock guard to avoid concurrent
      _counter->_usedTokens -= _value;
      LOGGER->info("Release: used tokens {}/{}", _counter->_usedTokens, _counter->_limit);
    }
    for(auto & d: _counter->_dependents) {
      d.lock()->check();
    }
  }

public:
  CounterDependency(ptr<CounterToken> const & counter, CounterToken::Value value) 
    : Dependency(counter), _counter(counter), _value(value) {}

  std::shared_ptr<xpm::Lock> _lock() override { 
    return mkptr<Lock>(std::dynamic_pointer_cast<CounterDependency>(shared_from_this()));
  }

  virtual DependencyStatus status() const override {
    return _counter->_usedTokens + _value <= _counter->_limit ? 
      DependencyStatus::OK : DependencyStatus::WAIT;
  }
private:
  ptr<CounterToken> _counter;
  CounterToken::Value _value;
  friend struct Lock;
};

CounterToken::CounterToken(uint32_t limit) : _limit(limit), _usedTokens(0) {}
CounterToken::~CounterToken() {}
void CounterToken::limit(uint32_t limit) { _limit = limit; }

ptr<Dependency> CounterToken::createDependency(Value count) {
  return mkptr<CounterDependency>(std::static_pointer_cast<CounterToken>(shared_from_this()), count);
}

ptr<Dependency> CounterToken::createDependency() {
  throw exception("Cannot make a simple dependency from a token");
}




// --- Job

class JobDependency : public Dependency {
  ptr<Job> job;
public:
  JobDependency(ptr<Job> const & job);
  virtual ~JobDependency() {}
  virtual DependencyStatus status() const override;  
  std::shared_ptr<xpm::Lock> _lock() override { 
    return nullptr;
  }

};

JobDependency::JobDependency(ptr<Job> const & job) : Dependency(job), job(job) {
}

DependencyStatus JobDependency::status() const {
  switch(job->state()) {
    case JobState::ERROR: return DependencyStatus::FAIL;
    case JobState::DONE: return DependencyStatus::OK;
    default: return DependencyStatus::WAIT;
  }
  return DependencyStatus::WAIT;
}



std::ostream &operator<<(std::ostream & out, JobState const & state) {
  switch(state) {
    case JobState::WAITING: return out << "WAITING";
    case JobState::READY: return out << "READY";
    case JobState::RUNNING: return out << "RUNNING";
    case JobState::DONE: return out << "DONE";
    case JobState::ERROR: return out << "ERROR";
  }
  return out;
}

Job::Job(Path const &locator, ptr<Launcher> const &launcher)
    : _locator(locator), _launcher(launcher), _unsatisfied(0) {}

Path Job::stdoutPath() const {
  return _locator.withExtension("out");
}

Path Job::stderrPath() const {
  return _locator.withExtension("err");
}

Path Job::pathTo(std::function<Path(Path const &)> f) const {
  return f(_locator);
}

nlohmann::json Job::toJson() const  {
  nlohmann::json j = {};
  j["locator"] = _locator.toString();
  return j;
}

std::string const & Job::getJobId() const {
  return _jobId;
}

JobState Job::state() const { return _state; }

void Job::setIds(std::string const & taskId, std::string const & jobId) {
  _taskId = taskId;
  _jobId = jobId;
}

nlohmann::json Job::getJsonState() const {
  nlohmann::json payload = {
    { "locator", _locator.toString() },
    { "taskId", _taskId },
    { "jobId", _jobId },
    { "status", state() },
    { "start", startTime() },
    { "end", endTime() },
    { "submitted", submissionTime() },
    { "status", state() },
    { "tags", nlohmann::json::array() }
  };
  return payload;
}
bool Job::ready() const { return _unsatisfied == 0; }

void Job::dependencyChanged(Dependency &dependency, DependencyStatus from, DependencyStatus to) {
  static auto value = [](DependencyStatus s) { return s == DependencyStatus::OK ? 1 : 0; };
  {
    _unsatisfied -= value(to) - value(from);
  }

  LOGGER->info("Job {}: unsatisfied {}", *this, _unsatisfied);

  if (to == DependencyStatus::FAIL) {
    // Job completed
    jobCompleted();
    return;
  }

  if (_unsatisfied == 0) {
    LOGGER->info("Job {} is ready to run", *this);
    start();
  }
}

JobState Job::state(JobState newState) {
  JobState old = _state;
  _state = newState;
  return old;
}

void Job::addDependency(ptr<Dependency> const &dependency) {
  // Add the dependency
  _dependencies.push_back(dependency);
  auto self = shared_from_this();
  dependency->target(self);

  // Update the dependency (it is unsatisfied at the start)
  _unsatisfied += 1;
}

ptr<Dependency> Job::createDependency() {
  return mkptr<JobDependency>(std::static_pointer_cast<Job>(shared_from_this()));
}

void Job::jobCompleted() {
  // Notify workspace
  _workspace->jobFinished(*this);

  // Notify dependents 
  for (auto &entry : this->_dependents) {
    entry.lock()->check();
  }
}

void Job::start() {
  if (exitMode != ExitMode::NONE) {
    LOGGER->info("Not starting job: exit signal received");
    std::unique_lock<std::mutex> jobLock(WORKSPACE_MUTEX);
    state(JobState::ERROR);
    return;
  }
  std::thread([this]() {
    std::vector<std::shared_ptr<Lock>> locks;
    
    {
      // Lock while we require all the dependencies
      LOGGER->info("Starting job {}", *this);
      std::unique_lock<std::mutex> jobLock(WORKSPACE_MUTEX);

      // (1) Lock all the dependencies
      for(auto dependency: _dependencies) {
        try {
          auto lock = dependency->lock();
          if (lock) {
            locks.push_back(lock);
          }
        } catch(lock_error &e) {
          // Lock error: we abort
          LOGGER->info("Aborting start for job {}", *this);
          dependency->check();
          state(JobState::READY);
          return;
        }
      }

      // (2) Run the task
      _workspace->jobStarted(*this);
      run(std::move(jobLock), locks);
    }

    // (3) release resources
    
    std::unique_lock<std::mutex> jobLockRelease(WORKSPACE_MUTEX);

    // Mark this job as completed
    this->jobCompleted();

    // All the lock from "locks" will be released here
    locks.clear();
  }).detach();
}

// --- Command line job

CommandLineJob::CommandLineJob(xpm::Path const &locator,
                               ptr<Launcher> const &launcher,
                               ptr<CommandLine> const &command)
    : Job(locator, launcher), _command(command) {
}

void CommandLineJob::parameters(std::shared_ptr<Value> const & parameters) {
  _parameters = parameters;
}

std::shared_ptr<Value> CommandLineJob::parameters() {
  return _parameters;  
}

void CommandLineJob::init() {
  _parameters->addDependencies(*this, false);
}

void CommandLineJob::run(std::unique_lock<std::mutex> && jobLock, std::vector<ptr<Lock>> & locks) {
  // Run uses a thread
  LOGGER->info("Running job {}...", *this);

  auto scriptBuilder = _launcher->scriptBuilder();
  auto processBuilder = _launcher->processBuilder();

  auto & connector = *_launcher->connector();
  auto const donePath = pathTo(DONE_PATH);

  // Check if already done
  auto check = [&] {
    if (connector.fileType(donePath) == FileType::FILE) {
      LOGGER->info("Job {} is already done", *this);
      state(JobState::DONE);
      jobCompleted();
      return true;
    }
    return false;
  };

  // check if done
  if (check()) return;

  // Lock the job and check done again (just in case)
  LOGGER->debug("Making directories job {}...", _locator);
  Path directory = _locator.parent();
  _launcher->connector()->mkdirs(directory, true, false);

  // Lock
  auto lockPath = pathTo(LOCK_PATH);
  auto lock = _launcher->connector()->lock(lockPath);

  // Check again if done (now that we have locked everything)
  if (check()) return;
  
  // Now we can write the script
  scriptBuilder->command = _command;
  scriptBuilder->lockFiles.push_back(lockPath);
  Path scriptPath = scriptBuilder->write(*_workspace, connector, _locator, *this);
  auto startlock = _launcher->connector()->lock(pathTo(LOCK_START_PATH));
  
  LOGGER->info("Starting job {}", _locator);
  processBuilder->environment = _launcher->environment();
  processBuilder->command.push_back(
      _launcher->connector()->resolve(scriptPath));
  processBuilder->stderr = Redirect::file(connector.resolve(directory.resolve({_locator.name() + ".err"})));
  processBuilder->stdout = Redirect::file(connector.resolve(directory.resolve({_locator.name() + ".out"})));

  _process = processBuilder->start();
  state(JobState::RUNNING);
  
  // Avoid to remove the locks ourselves
  startlock->detachState(true);
  lock->detachState(true);

  // Unlock since started
  jobLock.unlock();

  // Wait for end of execution
  LOGGER->info("Waiting for job {} to finish", _locator);
  int exitCode = _process->exitCode();
  state(exitCode == 0 ? JobState::DONE : JobState::ERROR);
  LOGGER->info("Job {} finished with exit code {} (state {})", _locator, exitCode, state());
}

void CommandLineJob::kill() {
  if (_process) _process->kill(false);
}

nlohmann::json CommandLineJob::getJsonState() const {
  auto j = Job::getJsonState();

  auto &tags = j["tags"];
  for(auto &entry: _parameters->tags()) {
    tags.push_back(nlohmann::json::array({ entry.first, entry.second.toJson() }));
  }
  return j;
}

// -- Workspace

std::unordered_set<Workspace *> Workspace::activeWorkspaces;


bool JobPriorityComparator::operator()(ptr<Job> const &lhs,
                                       ptr<Job> const &rhs) const {
  // Returns true if the lhs should have a lower priority than the rhs
  if (lhs->_unsatisfied != rhs->_unsatisfied)
    return lhs->_unsatisfied > rhs->_unsatisfied;
  return lhs->_submissionTime > rhs->_submissionTime;
}

Workspace::Workspace() : Workspace("") {
}

Workspace::Workspace(std::string const &path) : _path(path) {
  activeWorkspaces.insert(this);
  registerExitHandler();
}

Workspace::~Workspace() {
  activeWorkspaces.erase(this);
}


void Workspace::submit(ptr<Job> const &job) {
  {
    if (exitMode != ExitMode::NONE) {
      LOGGER->warn("Not registering job: application received exit signal");
    }

    std::lock_guard<std::mutex> lock(WORKSPACE_MUTEX);

    // Skip if same path exists
    if (_jobs.find(job->getJobId()) != _jobs.end()) {
      LOGGER->warn("Job with path {} already exists - skipping new submission",
                  job->getJobId());
      return;
    }

    waitingJobs.insert(job.get());

    // Add job to list
    job->_submissionTime = std::time(nullptr);
    job->_workspace = shared_from_this();
    _jobs[job->getJobId()] = job;

    // Register dependencies so that we are notified of changes
    for(auto & dependency: job->_dependencies) {
      dependency->_origin->addDependent(dependency);
      dependency->check();
    }
  }

  if (_serverContext) {
    nlohmann::json j = { { "type", "JOB_ADD" }, { "payload", job->getJsonState() } };
    _serverContext->forEach([&j](auto & l) { l.send(j); });
  }

  // Check if ready
  if (job->_dependencies.empty() && job->ready()) {
    job->start();
  }
}

void Workspace::jobStarted(Job const & job) {
  if (_serverContext) {
    nlohmann::json j = { { "type", "JOB_UPDATE" }, { "payload", {
      { "locator", job.locator().toString() },
      { "status", job.state() }
    }}};
    _serverContext->forEach([&j](auto & l) { l.send(j); });
  }
}

void Workspace::jobFinished(Job const & job) {
  std::lock_guard<std::mutex> lock(JOB_CHANGED_MUTEX);
  waitingJobs.erase(&job);
  if (_serverContext) {
    nlohmann::json j = { { "type", "JOB_UPDATE" }, { "payload", {
      { "jobId", job.getJobId() },
      { "status", job.state() }
    }}};
    _serverContext->forEach([&j](auto & l) { l.send(j); });
  }
  JOB_CHANGED.notify_all();
}

void Workspace::kill(std::string const & jobId) {
  std::lock_guard<std::mutex> lock(WORKSPACE_MUTEX);
  auto it = _jobs.find(jobId);
  if (it == _jobs.end()) {
    throw argument_error("Job ID " + jobId + " not found");
  }

  auto & job = *it->second;
  job.kill();
}


void Workspace::current() {
  CURRENT_WORKSPACE = shared_from_this();
}

std::shared_ptr<Workspace> Workspace::currentWorkspace() {
  return CURRENT_WORKSPACE;
}

Path const Workspace::workdir() const {
  return _path;
}

Path const Workspace::jobsdir() const {
  return _path / "jobs";
}

void Workspace::set(std::string const &key, std::string const &value) {
  _variables[key] = value;
}


void Workspace::set(std::string const &ns, std::string const &key, std::string const &value) {
  _variables[ns + "." + key] = value;
}

std::map<std::string,std::string>::const_iterator Workspace::find(std::string const &key) const {
  size_t last_dot = key.rfind('.');
  std::string name = key.substr(last_dot == std::string::npos ? 0 : last_dot + 1);

  for (;last_dot != std::string::npos; last_dot = key.rfind('.', last_dot - 1)) {
    // Get the key
    std::string _key = last_dot == std::string::npos ? 
        name : key.substr(0, last_dot) + "." + name;

    auto it = _variables.find(_key);
    if (it != _variables.end()) {
      return it;
    }
  }

  // Search for name
  return _variables.find(name);
}

std::string Workspace::get(std::string const &key) const {
  auto it = find(key);
  return it != _variables.end() ? it->second : "";
}

bool Workspace::has(std::string const &key) const {
  return find(key) != _variables.end();
}

void Workspace::experiment(std::string const & name) {
  _experiment = name;
}


void Workspace::waitUntilTaskCompleted() {
  // Go through all the workspaces
  size_t count = 0;
  do {
    // Count the number of jobs
    count = 0;
    std::unique_lock<std::mutex> lock(JOB_CHANGED_MUTEX);
    for(auto *ws: activeWorkspaces) {
      count += ws->waitingJobs.size();
    }

    if (count > 0) {
      LOGGER->info("Waiting for {} job(s) to complete", count);
      JOB_CHANGED.wait(lock);
    }
  } while (count > 0 && exitMode != ExitMode::STOPPING);
 
}

std::shared_ptr<rpc::Server> Workspace::server(int port, std::string const & htdocs) {
  if (_serverContext) throw new std::runtime_error("Server already started");

  LOGGER->info("Trying to run server on http://{}:{}", "127.0.0.1", port);
  _serverContext = mkptr<rpc::ExperimentServerContext>(*this, "127.0.0.1", port, htdocs);
  _server = mkptr<rpc::Server>();
  _server->start(*_serverContext, false);
  LOGGER->info("Started server http://{}:{}", "127.0.0.1", port);
  return _server;
}

void Workspace::refresh(xpm::rpc::Emitter & emitter) {
  emitter.send({ {"type", "CLEAN_INFORMATION"} });
  emitter.send({ {"type", "EXPERIMENT_ADD"}, { "payload", {
    { "name", _experiment }
  }}});
  emitter.send({ {"type", "EXPERIMENT_SET_MAIN"}, { "payload", _experiment } });

  std::lock_guard<std::mutex> lock(WORKSPACE_MUTEX);
  for(auto entry: _jobs) {
    emitter.send({ {"type", "JOB_ADD"}, { "payload", entry.second->getJsonState() } });
  }
}



} // namespace xpm