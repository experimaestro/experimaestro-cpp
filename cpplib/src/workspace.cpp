#include <xpm/launchers.hpp>
#include <xpm/workspace.hpp>

#include <__xpm/common.hpp>
#include <__xpm/scriptbuilder.hpp>

DEFINE_LOGGER("workspace");

namespace xpm {

// --- Dependency

Dependency::~Dependency() {}

void Dependency::target(ptr<Resource> const &resource) { _target = resource; }

void Dependency::check() {
  std::unique_lock<std::mutex> lock(_mutex);
  bool s = satisfied();
  if (s != _oldSatisfied) {
    _target->dependencyChange(*this, s);
  }
}

// --- Resource

Resource::Resource() {}
Resource::~Resource() {}

void Resource::addDependent(ptr<Dependency> const & dependency) {
  _dependents.insert(dependency);
}

virtual Resource::dependencyChanged(Dependency &dependency,
                                    bool satisfied) const {
  throw assertion_error(
      "A resource cannot handle a change in dependency directly");
}

// --- Token

CounterToken::CounterToken(uint32_t limit) : _limit(limit), _usedTokens(0) {}
void CounterToken::limit(uint32_t limit) { _limit = limit; }

// --- Job

Job::Job(Path const &locator, ptr<Launcher> const &launcher)
    : _locator(locator), _launcher(launcher), _unsatisfied(0) {}

JobState Job::state() const { return _state; }

void Job::dependencyChanged(Dependency &dependency, bool satisfied) const {
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
  dependency->to(self);
  dependency->from()->addDependent(self);

  // Update the dependency (it is unsatisfied at the start)
  _unsatisfied += 1;
  dependency->check();
}

void Job::updateDependency(Dependency const &dependency) {
  std::unique_lock<std::mutex> jobLock(_mutex);

  int change = dependency.change();
  _unsatisfied += change;
}

// --- Command line job

CommandLineJob::CommandLineJob(xpm::Path const &locator,
                               ptr<Launcher> const &launcher,
                               ptr<CommandLine> const &command)
    : Job(locator, launcher), _command(command) {}

void CommandLineJob::run() {
  auto scriptBuilder = _launcher->scriptBuilder();
  auto processBuilder = _launcher->processBuilder();

  Path scriptPath = scriptBuilder->write(_launcher->connector(), _locator);
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
        entry->check();
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

Workspace::Workspace(std::string const &path) {}

void Workspace::submit(ptr<Job> const &job) {
  std::lock_guard<std::mutex> lock(_mutex);

  // Skip if same path exists
  if (_jobs.find(job->locator()) != _jobs.end()) {
    LOGGER->warn("Job with path {} already exists - skipping new submission",
                 job->locator());
    return;
  }

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