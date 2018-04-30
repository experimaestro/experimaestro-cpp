#include <xpm/workspace.hpp>
#include <__xpm/common.hpp>

DEFINE_LOGGER("sched");

namespace xpm {

// --- Dependency

Dependency::Dependency(ptr<Resource> const & resource) : _from(resource) {

}

ptr<Resource> Dependency::from() {
    return _from;
}

void Dependency::to(ptr<Resource> const & resource) {
    _to = resource;
}

// --- Resource

Resource::Resource() {}
Resource::~Resource() {}
void Resource::addDependent(ptr<Resource> const & resource) {
    _dependents.insert(resource);
}

ResourceState Resource::state() const {
    return _state;
}


// --- Token

CounterToken::CounterToken(uint32_t limit) : _limit(limit), _usedTokens(0) {}
void CounterToken::limit(uint32_t limit) {
    _limit = limit;
}

// --- Command line job

CommandLineJob::CommandLineJob(xpm::Path const & locator, 
    ptr<Launcher> const & launcher,
    ptr<CommandLine> const & command
): Job(locator, launcher), _command(command) {
}

void CommandLineJob::run() {

}


// --- Job

Job::Job(Path const & locator, ptr<Launcher> const & launcher) : 
    _locator(locator), _launcher(launcher), _unsatisfied(0) {}

void Job::addDependency(ptr<Dependency> const & dependency) {
    _dependencies.push_back(dependency);
    auto self = shared_from_this();
    dependency->to(self);
    dependency->from()->addDependent(self);
}


// -- Workspace

bool JobPriorityComparator::operator()( ptr<Job> const & lhs, ptr<Job> const & rhs ) const {
    // Returns true if the lhs should have a lower priority than the rhs
    if (lhs->_unsatisfied != rhs->_unsatisfied)
        return lhs->_unsatisfied > rhs->_unsatisfied;
    return lhs->_submissionTime > rhs->_submissionTime;
}

Workspace::Workspace(std::string const &path) {

}

void Workspace::submit(ptr<Job> const & job) {
    std::lock_guard<std::mutex> lock(_mutex);

    // Skip if same path exists
    if (_jobs.find(job->locator()) != _jobs.end()) {
        LOGGER->warn("Job with path {} already exists - skipping new submission", job->locator());
        return;
    }

    // Add job to list
    job->_submissionTime = std::time(nullptr);
    job->_workspace = shared_from_this();
    _jobs[job->locator()] = job;

    // Check if ready
    
}

}