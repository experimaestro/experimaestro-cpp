#include <chrono>
#include <thread>
#include <gtest/gtest.h>
#include <spdlog/fmt/fmt.h>
#include <unistd.h>

#include <__xpm/common.hpp>
#include <xpm/connectors/local.hpp>
#include <xpm/workspace.hpp>

#include "date.h"

DEFINE_LOGGER("xpm.tests.scheduler");

using namespace xpm;


class TemporaryDirectory {
public:
  Path locator;
  ptr<LocalConnector> connector;

  TemporaryDirectory() {
    char t[] = "xpmtests-XXXXXX";
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

  friend std::ostream & operator<<(std::ostream &out, FakeJob const & job);

  virtual void run() override {
    LOGGER->info("[start] Running {}", *this);
    start = std::chrono::system_clock::now();
    std::this_thread::sleep_for(duration);

    end = std::chrono::system_clock::now();
    LOGGER->info("[end] Running {}", *this);
    state(JobState::DONE);
    jobCompleted();
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

    ws->waitUntilTaskCompleted();

    EXPECT_TRUE(job1->end > job2->start || job2->end > job1->start)
      << job1 << " and " << job2 << " did overlap";

}