// Fake experiment -- useful to run tests

#include <thread>

#include <Poco/Path.h>
#include <xpm/common.hpp>
#include <xpm/cpp.hpp>
#include <xpm/logging.hpp>
#include <xpm/workspace.hpp>
#include <xpm/xpm.hpp>

#include <__xpm/CLI11.hpp>
#include <__xpm/common.hpp>


using namespace xpm;
DEFINE_LOGGER("xpm");

struct TypeA : public CppObject<TypeA> {
  bool failure;
  long sleep;

  void run() override {
    std::cerr << "Running !!!\n";
    std::chrono::seconds duration(sleep);
    std::this_thread::sleep_for(duration);
    if (failure) throw std::runtime_error("failed as you wished");
  }

};

XPM_TYPE("TypeA", TypeA)
    .argument("sleep", &TypeA::sleep)
    .argument("failure", &TypeA::failure);
XPM_SIMPLETASK("task.a", TypeA);

int main(int argc, const char **argv) {

  if (argc > 1 && argv[1] == std::string("run")) {
    return currentRegister()->parse(argc, argv) ? 0 : 1;
  }
   
  CLI::App app{"Fake experiments"};
  app.require_subcommand(1);
  app.fallthrough(false);
  *EXECUTABLE_PATH = Path(Poco::Path(argv[0]).absolute().toString());

  {
    std::string workdir;
    int port;

    auto _run = app.add_subcommand("xp", "Run the experiment");
    _run->add_option("--port", port, "The working directory");
    _run->add_option("workdir", workdir, "The working directory")
        ->required(true);

    _run->set_callback([&]() {
      auto ws = mkptr<Workspace>(workdir);
      ws->experiment("test");
      if (port > 0)
        ws->server(port, ".");
      auto v = Value::create(
                   *currentRegister(),
                   {{"$type", "TypeA"}, {"sleep", 100}, {"failure", false}})
                   ->asMap();

      std::vector<std::shared_ptr<Dependency>> dependencies;

      currentRegister()
          ->getTask(Typename("task.a"))
          ->submit(ws, nullptr, v, dependencies);

      Workspace::waitUntilTaskCompleted();
    });
  }


  CLI11_PARSE(app, argc, argv);
}