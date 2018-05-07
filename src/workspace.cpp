#include <xpm/launchers.hpp>
#include <xpm/workspace.hpp>
#include <xpm/commandline.hpp>

#include <__xpm/common.hpp>
#include <__xpm/scriptbuilder.hpp>
#include <SQLiteCpp/SQLiteCpp.h>

DEFINE_LOGGER("workspace");

namespace xpm {

// --- Dependency

Dependency::Dependency(ptr<Resource> const & origin) : _origin(origin), _oldSatisfied(false) {}
Dependency::~Dependency() {
  if (_origin) {
    _origin->removeDependent(shared_from_this());
  }
}

void Dependency::target(ptr<Resource> const &resource) { _target = resource; }

void Dependency::check() {
  std::unique_lock<std::mutex> lock(_mutex);
  bool s = satisfied();
  if (s != _oldSatisfied) {
    _target->dependencyChanged(*this, s);
  }
}

// --- Resource

Resource::Resource() {}
Resource::~Resource() {}

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
                                    bool satisfied) {
  throw assertion_error(
      "A resource cannot handle a change in dependency directly");
}

// --- Token

CounterToken::CounterToken(uint32_t limit) : _limit(limit), _usedTokens(0) {}
void CounterToken::limit(uint32_t limit) { _limit = limit; }

class CounterDependency : public Dependency {
public:
  CounterDependency(ptr<CounterToken> const & counter, CounterToken::Value value) 
    : Dependency(counter), _counter(counter), _value(value) {}

  virtual bool satisfied() {
    return _counter->_usedTokens + _value <= _counter->_limit;
  }
private:
  ptr<CounterToken> _counter;
  CounterToken::Value _value;
};

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
  virtual bool satisfied() override;  
};

JobDependency::JobDependency(ptr<Job> const & job) : Dependency(job), job(job) {}

bool JobDependency::satisfied() {
  return job->state() == JobState::DONE;
}


Job::Job(Path const &locator, ptr<Launcher> const &launcher)
    : _locator(locator), _launcher(launcher), _unsatisfied(0) {}

JobState Job::state() const { return _state; }

bool Job::ready() const { return _unsatisfied == 0; }

void Job::dependencyChanged(Dependency &dependency, bool satisfied) {
  {
    std::unique_lock<std::mutex> lock(_mutex);
    _unsatisfied += satisfied ? -1 : 1;
  }

  if (_unsatisfied == 0) {
    run();
  }
}

void Job::addDependency(ptr<Dependency> const &dependency) {
  // Add the dependency
  _dependencies.push_back(dependency);
  auto self = shared_from_this();
  dependency->target(self);

  // Update the dependency (it is unsatisfied at the start)
  _unsatisfied += 1;
  dependency->check();
}

ptr<Dependency> Job::createDependency() {
  return mkptr<JobDependency>(std::static_pointer_cast<Job>(shared_from_this()));
}


// --- Command line job

CommandLineJob::CommandLineJob(xpm::Path const &locator,
                               ptr<Launcher> const &launcher,
                               ptr<CommandLine> const &command)
    : Job(locator, launcher), _command(command) {

  // Adding dependencies
  _command->forEach([&](CommandPart &c) -> void { 
    c.addDependencies(*this); 
  });
}

void CommandLineJob::run() {
  auto scriptBuilder = _launcher->scriptBuilder();
  auto processBuilder = _launcher->processBuilder();

  Path scriptPath = scriptBuilder->write(*_launcher->connector(), _locator, *this);
  processBuilder->command.push_back(
      _launcher->connector()->resolve(scriptPath));

  LOGGER->info("Starting job {}", _locator);
  ptr<Process> process = processBuilder->start();
  std::thread([this, &process]() {
    // Wait for end of execution
    int exitCode = process->exitCode();
    LOGGER->info("Job {} finished with exit code {}", _locator, exitCode);
    _state = exitCode == 0 ? JobState::DONE : JobState::ERROR;

    // Notify dependents if we exited clearly
    if (exitCode == 0) {
      for (auto &entry : this->_dependents) {
        entry.lock()->check();
      }
    }

  })
      .detach();
}

// -- Workspace

bool JobPriorityComparator::operator()(ptr<Job> const &lhs,
                                       ptr<Job> const &rhs) const {
  // Returns true if the lhs should have a lower priority than the rhs
  if (lhs->_unsatisfied != rhs->_unsatisfied)
    return lhs->_unsatisfied > rhs->_unsatisfied;
  return lhs->_submissionTime > rhs->_submissionTime;
}

Workspace::Workspace(std::string const &path) {
  _db = std::unique_ptr<SQLite::Database>(new SQLite::Database(path, SQLite::OPEN_READONLY));
}

Workspace::~Workspace() {}


void Workspace::submit(ptr<Job> const &job) {
  std::lock_guard<std::mutex> lock(_mutex);

  // Skip if same path exists
  if (_jobs.find(job->locator()) != _jobs.end()) {
    LOGGER->warn("Job with path {} already exists - skipping new submission",
                 job->locator());
    return;
  }

  // TODO: get a new resource ID

  // Add job to list
  job->_submissionTime = std::time(nullptr);
  job->_workspace = shared_from_this();
  _jobs[job->locator()] = job;

  // Check if ready
  if (job->ready()) {
    job->run();
  }
}

} // namespace xpm