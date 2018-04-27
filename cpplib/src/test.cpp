#include "CLI11.hpp"

int main(int argc, char **argv) {

  CLI::App app{"Experimaestro command line parser"};
  app.require_subcommand(1);
  app.fallthrough(false);

  auto _generate =
      app.add_subcommand("generate", "Generate definitions in JSON format");
  _generate->set_callback([&]() { std::cerr << "Generate\n"; });

  {
    auto _run = app.add_subcommand("run", "Run a given task");

    std::string taskName;
    _run->add_option("task", taskName, "Task name", true)->required();

    std::string paramFile;
    _run->add_option("jsonfile", paramFile, "Parameter file in JSON format",
                     true)
        ->check(CLI::ExistingFile)
        ->required();

    _run->set_callback([&]() {
      std::cerr << "Called run with " << taskName << " and " << paramFile;
    });
  }

  CLI11_PARSE(app, argc, argv);
}