/**
 * Workspace and related classes (launcher, connector, etc.)
 */

#ifndef XPM_WORKSPACE_H
#define XPM_WORKSPACE_H

#include <string>
#include <set>
#include <queue>
#include <unordered_map>
#include <vector>
#include <mutex>

#include <xpm/filesystem.hpp>

namespace xpm {

class Launcher;
class Resource;
class Workspace;
class CommandLine;
struct JobPriorityComparator;

/**
 * A dependency between resources
 */
class Dependency {
public:
  Dependency(ptr<Resource> const & resource);
  void to(ptr<Resource> const & resource);
  ptr<Resource> from();
private:
  ptr<Resource> _from;
  ptr<Resource> _to;
};

/**
 * Resource state
 * 
 * Possible paths:
 * - WAITING <-> READY -> RUNNING -> { ERROR, DONE } -> { WAITING, READY }
 * - {WAITING, READY} -> ON_HOLD -> READY
 */
enum struct ResourceState {
   /**
     * For a job only: the job is waiting dependencies status be met
     */
    WAITING,

    /**
     * For a job only: the job is waiting for an available thread status launch it
     */
    READY,

    /**
     * For a job only: The job is currently running
     */
    RUNNING,

    /**
     * The job is on hold
     */
    ON_HOLD,

    /**
     * The job ran but did not complete or the data was not generated
     */
    ERROR,

    /**
     * Completed (for a job) or generated (for a data resource)
     */
    DONE
};

/// Base class for any resource
class Resource 
#ifndef SWIG
: public std::enable_shared_from_this<Resource>
#endif
{
public:
  Resource();
  ~Resource();

  void addDependent(ptr<Resource> const & resource);
  ResourceState state() const;
private:
  /// Resource that depend on this one to be completed
  std::set<ptr<Resource>> _dependents;

  /// Resource state
  ResourceState _state;
};




/// Tokens are used to limit the number of running jobs
class Token : public Resource {
};

/// A simple token based on counts
class CounterToken : public Token {
public:

  /// Initialize the token
  CounterToken(uint32_t limit);
  
  /// Set the limit
  void limit(uint32_t _limit);

private:
    /**
     * Maximum number of tokens available
     */
    uint32_t _limit;

    /**
     * Number of used tokens
     */
    uint32_t _usedTokens;
};


/// Base class for jobs
class Job : public Resource {
public:
  Job(Path const & locator, ptr<Launcher> const & launcher);

  void addDependency(ptr<Dependency> const & dependency);

  /// The locator
  Path const & locator() { return _locator; }
  
  /// Run the job
  virtual void run() = 0;

private:
  friend class Workspace;
  friend struct JobPriorityComparator;

  /// The workspace
  ptr<Workspace> _workspace;

  /// Main identifier of the task
  Path _locator;

  /// The launcher used for this task
  ptr<Launcher> _launcher;

  /// The dependencies of this task
  std::vector<ptr<Dependency>> _dependencies;

  /// Submission time
  std::time_t _submissionTime;

  /// Number of dependencies that are not satisifed
  size_t _unsatisfied;
};

/// A command line job
class CommandLineJob : public Job {
public:
  CommandLineJob(Path const & locator, 
    ptr<Launcher> const & launcher,
    ptr<CommandLine> const & command);
  
  virtual void run();
private:
  ptr<CommandLine> _command;
};

/// Defines the priority between two jobs
struct JobPriorityComparator {
  bool operator()( ptr<Job> const & lhs, ptr<Job> const & rhs ) const;

};

/** 
 * Workspace tracking resources, jobs and scheduling
 */
class Workspace 
#ifndef SWIG
: public std::enable_shared_from_this<Workspace>
#endif
{
public:
  /// Creates a new work space with a given path
  Workspace(std::string const &path);

  /// Submit a job
  void submit(ptr<Job> const & job);

private:
  /// Jobs that are ready to run
  std::priority_queue<ptr<Job>, std::vector<ptr<Job>>, JobPriorityComparator> _ready;

  /// All the jobs
  std::unordered_map<Path, ptr<Job>> _jobs;

  /// State mutex
  std::mutex _mutex;
};

}

#endif