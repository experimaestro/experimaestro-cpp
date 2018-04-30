#include <xpm/workspace.hpp>

namespace xpm {

// --- Dependency

Dependency::Dependency(ptr<Resource> const &job) : _job(job) {

}

// --- Resource

Resource::Resource(Path const & locator) : _locator(locator) {}

// --- Command line job

CommandLineJob::CommandLineJob(xpm::Path const & locator, 
    ptr<Launcher> const & launcher,
    ptr<CommandLine> const & command
): Job(locator, launcher), _command(command) {
}



// --- Job

Job::Job(Path const & locator, ptr<Launcher> const & launcher) : Resource(locator), _launcher(launcher) {}
void Job::addDependency(ptr<Dependency> const & dependency) {
    _dependencies.push_back(dependency);
}


// -- Workspace

Workspace::Workspace(std::string const &path) {

}

void Workspace::submit(ptr<Job> const & job) {

}

}