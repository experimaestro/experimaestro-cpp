#include <xpm/workspace.hpp>

namespace xpm {

// --- Command line job

CommandLineJob::CommandLineJob(xpm::Path const & locator, 
    ptr<Launcher> const & launcher,
    ptr<Command> const & command
): Job(locator, launcher), _command(command) {
}


// --- job

Workspace::Workspace(std::string const &path) {

}

void Workspace::submit(ptr<Job> const & job) {

}

}