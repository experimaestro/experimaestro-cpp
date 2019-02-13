/**
 * An experimental workspace
 */

#ifndef XPM_WORKSPACE_H
#define XPM_WORKSPACE_H

#include <string>
#include <queue>
#include <unordered_map>
#include <map>
#include <unordered_set>
#include <vector>
#include <mutex>

#include <xpm/json.hpp>
#include <xpm/common.hpp>
#include <xpm/filesystem.hpp>


namespace xpm {

class Launcher;
class Resource;
class Workspace;
class Value;
class CommandLine;
struct JobPriorityComparator;
class CounterDependency;
class Lock;
class Process;

namespace rpc {
 class ExperimentServerContext;
 class Server;
 struct Emitter;
} //rpc

typedef std::uint64_t ResourceId;

enum class DependencyStatus {
  /// Dependency might be satisfied in the future 
  WAIT,
  /// Dependency is satisfied
  OK,
  /// Dependency won't be satifisfied (parent job failed)
  FAIL
};

/**
 * A dependency between resources
 */
class Dependency : public std::enable_shared_from_this<Dependency>, public Outputable {
public:
  Dependency(std::shared_ptr<Resource> const & origin);
  virtual ~Dependency();

  /// Sets the dependent
  void target(std::shared_ptr<Resource> const & resource);

  /// Get the current dependency status
  virtual DependencyStatus status() const = 0;

  /// Check the status and update the dependent if needed
  void check();

  /// Lock the resource
  std::shared_ptr<Lock> lock();

protected:
  /// To string
  virtual void output(std::ostream &out) const override;

  /// Lock the dependency
  virtual std::shared_ptr<Lock> _lock() = 0;

private:
  // The origin
  std::shared_ptr<Resource> _origin;

  // The dependent
  std::shared_ptr<Resource> _target;

  // The current lock (if any)
  std::weak_ptr<Lock> _currentLock;

  /// Old satisfaction status
  DependencyStatus _oldStatus;
  
  friend class Resource;
  friend class Workspace;
};

/// Base class for any resource
class Resource : public std::enable_shared_from_this<Resource>, public Outputable {
public:
  Resource();
  ~Resource();

  /// Finish the initialization
  virtual void init();

  void addDependent(std::shared_ptr<Dependency> const & dependency);
  void removeDependent(std::shared_ptr<Dependency> const & dependency);

  /**
   * Create a simple dependency from this resource
   * 
   * Subclasses might propose more specific dependency creation
   */
  virtual std::shared_ptr<Dependency> createDependency() = 0;

  /// Returns the resource as JSON (to include in job parameters)
  virtual nlohmann::json toJson() const { return nullptr; }

  /// Returns a JSON state
  virtual nlohmann::json getJsonState() const { return nullptr; };

protected:
  /// Resource that depend on this one to be completed
  std::vector<std::weak_ptr<Dependency>> _dependents;

  /// Signals a change in a dependency
  virtual void dependencyChanged(Dependency & dependency, 
    DependencyStatus from, DependencyStatus to);

  friend class Dependency;

  virtual void output(std::ostream &out) const override;
};



/// Tokens are used to limit the number of running jobs
class Token : public Resource {
};


/// A simple token based on counts
class CounterToken : public Token {
public:
  typedef uint32_t Value;

  /// Initialize the token
  CounterToken(Value limit);
  virtual ~CounterToken();
  
  /// Set the limit
  void limit(Value _limit);

  /// Create a new dependency
  std::shared_ptr<Dependency> createDependency(Value count);

  virtual std::shared_ptr<Dependency> createDependency() override;

private:
  friend class CounterDependency;
    /**
     * Maximum number of tokens available
     */
    Value _limit;

    /**
     * Number of used tokens
     */
    Value _usedTokens;
};

/**
 * Resource state
 * 
 * Possible paths:
 * - WAITING <-> READY -> RUNNING -> { ERROR, DONE } -> { WAITING, READY }
 * - {WAITING, READY} -> ON_HOLD -> READY
 */
enum struct JobState {
  /// For a job only: the job is waiting dependencies status be met
  WAITING,

  /// For a job only: the job is waiting for an available thread status launch it
  READY,

  /// For a job only: The job is currently running
  RUNNING,

  /**
   * The job ran but did not complete, the data was not generated, or dependencies 
   * failed to generate
  */
  ERROR,

  /// Completed (for a job) or generated (for a data resource)
  DONE
};

NLOHMANN_JSON_SERIALIZE_ENUM(JobState, {
    {JobState::WAITING, "waiting"},
    {JobState::READY, "ready"},
    {JobState::RUNNING, "running"},
    {JobState::ERROR, "error"},
    {JobState::DONE, "done"}
});

/// String representation of a job state
std::ostream &operator<<(std::ostream & out, JobState const & state);


extern PathTransformer EXIT_CODE_PATH;
extern PathTransformer LOCK_PATH;
extern PathTransformer LOCK_START_PATH;
extern PathTransformer DONE_PATH;
extern PathTransformer PID_PATH;

/// Base class for jobs
class Job : public Resource {
public:
  Job(Path const & locator, std::shared_ptr<Launcher> const & launcher);

  /// Adds a new dependency
  void addDependency(std::shared_ptr<Dependency> const & dependency);

  /// The locator
  Path const & locator() const { return _locator; }

  /// Returns true if the job is ready to run
  bool ready() const;

  /// Get the current state
  JobState state() const;

  /// Kill the job (if running)
  virtual void kill() = 0;

  /// Get start time
  std::time_t startTime() const { return _startTime; }
  /// Get end time
  std::time_t endTime() const { return _endTime; }
  /// Get submission time
  std::time_t submissionTime() const { return _submissionTime; }

  /// Set task and job IDs
  void setIds(std::string const & taskId, std::string const & jobId);

  /// Get a JSON representation of the state
  std::string const & getJobId() const;

  /// Get a JSON representation of the state
  virtual nlohmann::json getJsonState() const override;

  /// Get a dependency to this resource
  std::shared_ptr<Dependency> createDependency() override;

  /// Get a JSON representation of this resource
  virtual nlohmann::json toJson() const override;

  /// Get the path to the output stream
  Path stdoutPath() const;

  /// Get the path to the error stream
  Path stderrPath() const;

  /// Get the path to the lock start path
  Path pathTo(std::function<Path(Path const &)> f) const;

  /** 
   * Starts the job:
   * 
   * 1) Ensures all dependencies are satisfied and locked
   * 2) Runs by giving all the locks
   */
  void start();

protected:
  friend class Workspace;
  friend struct JobPriorityComparator;

  /// Run the job (called by start)
  virtual void run(std::unique_lock<std::mutex> && jobLock, std::vector<ptr<Lock>> & locks) = 0;

  /// Set the current state
  JobState state(JobState newState);

  /// Signals a dependency change
  virtual void dependencyChanged(Dependency & dependency, 
    DependencyStatus from, DependencyStatus to) override;

  /// Called when the job is completed
  void jobCompleted();

  /// The workspace
  std::shared_ptr<Workspace> _workspace;

  /// Main identifier of the task
  Path _locator;

  /// The launcher used for this task
  std::shared_ptr<Launcher> _launcher;

  /// The dependencies of this task
  std::vector<std::shared_ptr<Dependency>> _dependencies;

  /// Submission time
  std::time_t _submissionTime;

  /// Start time
  std::time_t _startTime;

  /// Start time
  std::time_t _endTime;

  /// Number of dependencies that are not satisifed
  size_t _unsatisfied;

  /// Task ID
  std::string _taskId;

  /// Job ID (hash)
  std::string _jobId;
private:
  /// Resource state
  JobState _state;
};

/// A command line job
class CommandLineJob : public Job {
public:
  CommandLineJob(Path const & locator, 
    std::shared_ptr<Launcher> const & launcher,
    std::shared_ptr<CommandLine> const & command);
  virtual ~CommandLineJob() = default;
  virtual void init() override;
  /// Set the parameters
  void parameters(std::shared_ptr<Value> const & parameters);
  std::shared_ptr<Value> parameters();
  virtual nlohmann::json getJsonState() const override;
  void kill() override;

protected:
  virtual void run(std::unique_lock<std::mutex> && jobLock, std::vector<ptr<Lock>> & locks) override;
private:
  std::shared_ptr<CommandLine> _command;
  std::shared_ptr<Value> _parameters;
  std::shared_ptr<Process> _process;
};

/// Defines the priority between two jobs
struct JobPriorityComparator {
  bool operator()( std::shared_ptr<Job> const & lhs, std::shared_ptr<Job> const & rhs ) const;

};

/** 
 * Workspace tracking resources, jobs and scheduling
 */
class Workspace : public std::enable_shared_from_this<Workspace> {
public:
  /// Creates a work space
  Workspace();

  /// Creates a new work space with a given path
  Workspace(std::string const &path);

  /// Destructor
  virtual ~Workspace();

  /// Submit a job
  void submit(std::shared_ptr<Job> const & job);

  /// Get an iterator for a key
  std::map<std::string, std::string>::const_iterator find(std::string const &key) const;

  /// Get the basepath
  Path const workdir() const;

  /// Get the jobs directory
  Path const jobsdir() const;

  /// Sets a variable
  void set(std::string const &key, std::string const &value);

  /// Sets a variable with a ns
  void set(std::string const &ns, std::string const &key, std::string const &value);

  /// Gets a variable given a fully qualified name
  std::string get(std::string const &key) const;

  /// Checks if the variable exists
  bool has(std::string const &key) const;

  /// Set the current workspace
  void current();

  /// Current workspace 
  static std::shared_ptr<Workspace> currentWorkspace();

  /// Experiment
  void experiment(std::string const & name);

  /// Kill a job
  void kill(std::string const & jobId);

  /// Wait that all tasks are completed
  static void waitUntilTaskCompleted();

  /// Notify that a job start
  void jobStarted(Job const &job);

  /// Notify that a job finished
  void jobFinished(Job const &job);

  /// Server
  std::shared_ptr<rpc::Server> server(int port, std::string const & htdocs);

  /// Refresh
  void refresh(xpm::rpc::Emitter &);

private:
  /// Working directory path
  Path _path;

  /// Current experiment name
  std::string _experiment;

  /// All the jobs
  std::unordered_map<std::string, std::shared_ptr<Job>> _jobs;

  /// Count the number of waiting jobs
  std::unordered_set<Job const *> waitingJobs;

  /// List of active workspaces
  static std::unordered_set<Workspace *> activeWorkspaces;

  /// The variables for this workspace
  std::map<std::string, std::string> _variables;

  /// The server context, if any
  std::shared_ptr<rpc::ExperimentServerContext> _serverContext;
  std::shared_ptr<rpc::Server> _server;
};


}

#endif
