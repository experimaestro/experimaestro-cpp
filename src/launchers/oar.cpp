#include <sstream>
#include <chrono>
#include <spdlog/fmt/fmt.h>

#include <xpm/workspace.hpp>
#include <xpm/connectors/connectors.hpp>
#include <xpm/launchers/oar.hpp>
#include <__xpm/scriptbuilder.hpp>
#include <__xpm/common.hpp>

DEFINE_LOGGER("launchers.oar");

namespace xpm {

/**
 * Represents an OAR process: job state update can be either 
 * given throught the notification URL, or through 
 * polling
 */
class OARProcess : public Process {
  ptr<Connector> _connector;
  std::string _jobId;
  decltype(std::chrono::system_clock::now()) last_updated;
  bool running;
public:
  OARProcess(std::string const & jobId, ptr<Connector> const &connector) 
    : _connector(connector), _jobId(jobId) {
  }

  virtual bool isRunning() {
    update();
    return running;
  }

  /// Exit code 
  virtual int exitCode() {
    NOT_IMPLEMENTED();
  }

  /// Kill
  virtual void kill(bool force) {
    // TODO: implement
    NOT_IMPLEMENTED();
  }

  virtual void notify(JobState const & state) override {
    switch(state) {
      case JobState::RUNNING:
        last_updated = std::chrono::system_clock::now();
        break;
      case JobState::DONE:
      case JobState::ERROR:
        // TODO: implement
        NOT_IMPLEMENTED();
      default:
        LOGGER->warn("Job state notification not handled: {}", state);
        // Not doing anything
        break;
    }
  }

  // Update the state
  void update() {
    if ((last_updated - std::chrono::system_clock::now()) > std::chrono::seconds(10)) {
      // Check
      auto builder = _connector->processBuilder();
      builder->command.push_back("oarstat");
      builder->command.push_back("-J");
      builder->command.push_back("-j");
      builder->command.push_back(_jobId);
      std::ostringstream ostr;

      builder->stdout = Redirect::pipe([&](const char * bytes, size_t n) {
        ostr << std::string(bytes, n);
      }); 
      builder->start();
      
      auto j = nlohmann::json::parse(ostr.str());
      std::string state = j[_jobId]["state"];

      if (state == "Terminated") {

      }
      if (state == "Running" || state == "Waiting") {
        
      }

      NOT_IMPLEMENTED();
    }
  }

  /**
   * Write to standard input
   * @return The number of bytes written (or -1 if an error occurred)
   */
  virtual long write(void * s, long count) {
    throw illegal_argument_error("Cannot write on standard input of OAR process");
  }

  /**
   * Closes standard in
   */
  virtual void eof() {
    throw illegal_argument_error("Cannot write on standard input of OAR process");
  }

};

/// Matches the OAR=... string
struct EnvMatcher {
  const std::string prefix;
  size_t pos = 0;
  std::string value;
  bool reading = false;

  EnvMatcher(std::string const & prefix) : prefix(prefix) {}

  void process(const char * bytes, size_t n) {
    for(size_t i = 0; i < n; ++i) {
      if (reading) {
        if (bytes[i] == '\n') {
          reading = false;
        } else {
          value += bytes[i];
        }
      } else {
        if (bytes[i] != prefix[pos]) {
          pos = 0;
        } else {
          if (++pos >= prefix.size()) {
            reading = true;
          }
        }
      }
    }
  }
};

class OARProcessBuilder : public ProcessBuilder {
  ptr<Connector> _connector;
public:
  OARProcessBuilder(ptr<Connector> const &connector) : _connector(connector) {
  }

  virtual ptr<Process> start() override {
    auto builder = _connector->processBuilder();

    // Prepare the OAR command

    if (detach) throw illegal_argument_error("Cannot run OAR undetached");

    auto pushstream = [&] (Redirect & r)  {
      switch (r.type) {
        case Redirection::INHERIT:
        case Redirection::PIPE:
          throw illegal_argument_error("Cannot run OAR undetached");

        case Redirection::NONE:
          builder->command.push_back("/dev/null");
          break;

        case Redirection::FILE:
          builder->command.push_back(r.path);
          break;
      }

    };

    auto process = builder->start();
    builder->command.push_back("oarsub");
    builder->command.push_back("-O");
    pushstream(builder->stdout);
    builder->command.push_back("-E");
    pushstream(builder->stderr);
    builder->detach = false;


    std::ostringstream oss;
    bool first;
    for(auto c: command) {
      if (first) first = false;
      else oss << " ";
      oss << "\"" << ShScriptBuilder::protect_quoted(c) << "\"";
    }
    builder->command.push_back(oss.str());


    // Execute it
    EnvMatcher matcher("\nOAR_JOB_ID=");
    builder->stdout = Redirect::pipe([&](const char * bytes, size_t n) {
      matcher.process(bytes, n);
    });
    int code = process->exitCode();
    
    if (code != 0) {
      throw exception(fmt::format("Could not launch OAR process {}", code));
    }

    if (matcher.value.empty())
      throw exception("Could not get the OAR job ID");

    return mkptr<OARProcess>(matcher.value, _connector);

  }
};

OARLauncher::OARLauncher(ptr<Connector> const &connector)
    : Launcher(connector) {
    
}

std::shared_ptr<ProcessBuilder> OARLauncher::processBuilder() {
    return mkptr<OARProcessBuilder>(connector());
}

std::shared_ptr<ScriptBuilder> OARLauncher::scriptBuilder() {
    return std::make_shared<ShScriptBuilder>();
}

} // namespace xpm