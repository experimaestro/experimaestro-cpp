/**
 * Scheduler and related classes (launcher, connector, etc.)
 */

#ifndef XPM_SCHEDULER_H
#define XPM_SCHEDULER_H

#include <string>
#include <xpm/utils.hpp>
#include <xpm/filesystem.hpp>

namespace xpm {

class Launcher;
class Resource;

class XPM_PIMPL(Dependency) {
public:
  Dependency(Resource & job);
};

/// Base class for any resource
class XPM_PIMPL(Resource) {
public:
  Resource(xpm::Path const & locator);
  xpm::Path const & locator() { return _locator; }
};

/// Base class for jobs
class XPM_PIMPL_CHILD(Job, Resource) {
public:
  Job(xpm::Path const & locator, ptr<Launcher> const & launcher);
  void addDependency(ptr<Dependency> const & dependency);
};

/// A command line job
class XPM_PIMPL_CHILD(CommandLineJob, Job) {
public:
  CommandLineJob(Path locator, Launcher launcher, Command command);
  void submit();
};


/** 
 * Workspace tracking resources, jobs and scheduling
 */
class XPM_PIMPL(Workspace) {
public:
  /// Creates a new work space with a given path
  Workspace(std::string const &path);

  /// Submit a job
  void submit(Job const & job);
};

}

#endif