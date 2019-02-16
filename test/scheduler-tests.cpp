#include <chrono>
#include <thread>
#include <gtest/gtest.h>
#include <spdlog/fmt/fmt.h>
#include <unistd.h>

#include <config.h>
#include <__xpm/common.hpp>
#include <xpm/connectors/local.hpp>
#include <xpm/commandline.hpp>
#include <xpm/connectors/local.hpp>
#include <xpm/workspace.hpp>
#include <xpm/xpm.hpp>

#include "date.h"

DEFINE_LOGGER("xpm.tests.scheduler");

using namespace xpm;


class TemporaryDirectory {
public:
  Path locator;
  ptr<LocalConnector> connector;

  TemporaryDirectory() {
    char t[] = "/tmp/xpmtests-XXXXXX";
    locator = Path(mkdtemp(t));
    connector = mkptr<LocalConnector>();
    LOGGER->info("Creating directory {}", t);
  }

  ~TemporaryDirectory() {
    LOGGER->info("Removing directory {}", locator);
    connector->remove(locator, true);
  }

};

class FakeJob : public Job {
public:
  static int COUNTER;
  int jobId;

  std::chrono::milliseconds duration;
  std::chrono::time_point<std::chrono::system_clock> start, end;

  /**
   * @param duration Duration in ms
   */
  FakeJob(TemporaryDirectory &d, std::chrono::milliseconds const & duration) :
   Job(d.locator / fmt::format("job{}", ++COUNTER), nullptr), jobId(COUNTER), duration(duration) {};
  virtual ~FakeJob() {}

  virtual void kill() {}

  friend std::ostream & operator<<(std::ostream &out, FakeJob const & job);

  virtual void run(MutexLock && jobLock, std::vector<ptr<Lock>> & locks) override {
    LOGGER->info("[start] Running {}", *this);
    start = std::chrono::system_clock::now();

    jobLock.unlock();
    std::this_thread::sleep_for(duration);

    end = std::chrono::system_clock::now();
    LOGGER->info("[end] Running {}", *this);
  }
};

std::ostream & operator<<(std::ostream &out, FakeJob const & job) {
  out << "Job " << job.jobId;
  static auto const nulltime = std::chrono::time_point<std::chrono::system_clock>();
  if (job.start != nulltime) {
    out << " (" << date::format("%D %T %Z", job.start);
    if (job.end != nulltime) out << " - " << date::format("%D %T %Z", job.end);
    
    out << ")";
  }
  return out;
}

int FakeJob::COUNTER = 0;

class SchedulerTest : public ::testing::Test {
protected:
  TemporaryDirectory directory;
  ptr<Workspace> ws;

  static void SetUpTestCase() {
  }

  static void TearDownTestCase() {
  }

  virtual void SetUp() {
    // directory = std::unique_ptr<TemporaryDirectory>(new TemporaryDirectory());
    ws = mkptr<Workspace>(directory.locator.localpath());
  }
};


TEST_F(SchedulerTest, Token) {
    // using namespace std::chrono_literals;

    auto token = mkptr<CounterToken>(1);
    
    auto job1 = mkptr<FakeJob>(directory, std::chrono::milliseconds(500));
    job1->addDependency(token->createDependency(1));

    auto job2 = mkptr<FakeJob>(directory, std::chrono::milliseconds(500));
    job2->addDependency(token->createDependency(1));

    ws->submit(job1);
    ws->submit(job2);

    LOGGER->info("Waiting for jobs to be completed");
    ws->waitUntilTaskCompleted();

    EXPECT_TRUE(job1->end < job2->start || job2->end < job1->start)
      << job1 << " and " << job2 << " did overlap";

}

// WorkspaceRestart - when an experiment is restarted,
// already running jobs should be picked up and watched
namespace {
 const std::string WAITFILE = XPM_TEST_SOURCEDIR "/scripts/waitfile";
} 


TEST_F(SchedulerTest, WorkspaceRestart) {
  
  auto jobpath = directory.locator / "restart";
  auto wakeuppath = jobpath.parent() / "wakeup";

  auto commandline = mkptr<CommandLine>();
  auto command = mkptr<Command>();
  commandline->add(command);
  command->add(mkptr<CommandPath>(Path(WAITFILE)));
  command->add(mkptr<CommandPath>(wakeuppath));

  // Submit job
  auto job = mkptr<CommandLineJob>(jobpath, Launcher::defaultLauncher(), commandline);
  job->parameters(mkptr<MapValue>());
  ws->submit(job);

  // Wait for job to be started

  struct Listener : WorkspaceListener {
    std::condition_variable cv;
    std::mutex cv_m;
    std::unique_lock<std::mutex> lock;
    
    Listener() : lock(cv_m) {}
    
    void jobStatus(Job const &job) {
      cv.notify_all();
    }
  };

  auto listener = mkptr<Listener>();
  ws->addListener(listener);
  
  auto success = listener->cv.wait_for(listener->lock, std::chrono::seconds(1), [&] { return job->state() == JobState::RUNNING; });
  ASSERT_TRUE(success); //, "Job was not started within 1s");

  // Launch same job on another workspace and wait for completion detection

  auto ws2 = mkptr<Workspace>(directory.locator.localpath());
  auto job2 = mkptr<CommandLineJob>(jobpath, Launcher::defaultLauncher(), commandline);
  job2->parameters(mkptr<MapValue>());
  ws2->submit(job2);

  auto listener2 = mkptr<Listener>();
  ws->addListener(listener2);

  LocalConnector connector;
  connector.createFile(wakeuppath);
  std::cerr << "Creating " << wakeuppath.toString() << "\n";
  success =  listener2->cv.wait_for(listener2->lock, std::chrono::seconds(1), [&] { return job2->state() == JobState::DONE; });

  ASSERT_TRUE(success); //, "Job was not finished within 1s");

}
