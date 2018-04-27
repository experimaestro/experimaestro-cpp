#include <xpm/workspace.hpp>


CommandLineJob::CommandLineJob(xpm::Path const & locator, 
    ptr<Launcher> const & launcher,
    ptr<Command> const & command
): Job(locator, launcher), _command(command) {
}
void command();

void submit();


Workspace::Workspace(std::string const &path) {

}
void Workspace::submit(ptr<Job> const & job) {

}
