/**
 * Scheduler and related classes (launcher, connector, etc.)
 */

#ifndef XPM_SCHEDULER_H
#define XPM_SCHEDULER_H

#include <string>
#include <xpm/filesystem.hpp>

namespace xpm {

class Launcher;
class Resource;
class Command;

class Dependency {
public:
  Dependency(ptr<Resource> const &job);
private:
  ptr<Resource> job;
};

/// Base class for any resource
class Resource {
public:
  Resource(Path const & locator);
  Path const & locator() { return _locator; }
private:
  Path _locator;
};

/// Base class for jobs
class Job : public Resource {
public:
  Job(Path const & locator, ptr<Launcher> const & launcher);

  void addDependency(ptr<Dependency> const & dependency);
};

/// A command line job
class CommandLineJob : public Job {
public:
  CommandLineJob(Path const & locator, 
    ptr<Launcher> const & launcher,
    ptr<Command> const & command);
private:
  ptr<Command> _command;
};


/** 
 * Workspace tracking resources, jobs and scheduling
 */
class Workspace {
public:
  /// Creates a new work space with a given path
  Workspace(std::string const &path);

  /// Submit a job
  void submit(ptr<Job> const & job);
};

}

#endif