/**
 * Scheduler and related classes (launcher, connector, etc.)
 */

#ifndef XPM_SCHEDULER_H
#define XPM_SCHEDULER_H

#include <string>
#include <vector>

#include <xpm/filesystem.hpp>

namespace xpm {

class Launcher;
class Resource;
class CommandLine;

class Dependency {
public:
  Dependency(ptr<Resource> const &job);
private:
  ptr<Resource> _job;
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
private:
  ptr<Launcher> _launcher;
  std::vector<ptr<Dependency>> _dependencies;
};

/// A command line job
class CommandLineJob : public Job {
public:
  CommandLineJob(Path const & locator, 
    ptr<Launcher> const & launcher,
    ptr<CommandLine> const & command);
private:
  ptr<CommandLine> _command;
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