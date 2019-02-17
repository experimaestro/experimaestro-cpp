
#include <Poco/Data/SQLite/Connector.h>
#include <Poco/Data/Session.h>
#include <Poco/Path.h>
#include <Poco/Net/WebSocket.h>


#include <xpm/workspace.hpp>
#include <xpm/rpc/configuration.hpp>
#include <xpm/rpc/servercontext.hpp>
#include <__xpm/common.hpp>

DEFINE_LOGGER("xpm.rpc");


namespace xpm::rpc {

namespace {
    static int DB_VERSION = 1;
}

inline void protectThread(ptr<Emitter> const & emitter,  std::function<void()> f) {
    std::thread([emitter, f]() {
        try {
            f();
        } catch(std::exception & e) {
            emitter->send({ { "type", "SERVER_ERROR" }, { "payload", e.what() } });
        }
    }).detach();
}

ServerContext::~ServerContext() {}
nlohmann::json ServerContext::handle(std::shared_ptr<Emitter> const & emitter, nlohmann::json &message) {
    if (message.count("type") > 0) {
        std::string type = message["type"];
        if (type == "refresh") {
            // application requested a refresh (new connexion)
            protectThread(emitter, [this, emitter]() { refresh(emitter); });
            return nullptr;
        } 

        if (type == "kill") {
            std::string jobId = message["payload"];
            protectThread(emitter, [this, emitter, jobId]() { this->kill(emitter, jobId); });                       
            return nullptr;
        }

        if (type == "details") {
            std::string jobId = message["payload"];
            protectThread(emitter, [this, emitter, jobId]() { this->jobDetails(emitter, jobId); });                       
            return nullptr;
        }

        throw std::runtime_error("Cannot handle action " + type);
    }
    throw std::runtime_error("No type in message");
}

std::unique_ptr<Poco::File> ServerContext::pidFile() const { return nullptr; }

int ServerContext::port() { return _port; }
std::string const & ServerContext::host() { return _hostname; }
std::string const & ServerContext::htdocs() { return _htdocs; }


void ServerContext::add(Emitter * listener) {
    _listeners.insert(listener);
}
void ServerContext::remove(Emitter * listener) {
    _listeners.erase(listener);
}
void ServerContext::forEach(std::function<void(Emitter&)> f) {
    for(auto listener: _listeners) {
        f(*listener);
    }
}



MainServerContext::~MainServerContext() {}
MainServerContext::MainServerContext() {
  ConfigurationParameters parameters;
  auto conf = parameters.serverConfiguration();

  auto basepath = Poco::Path::forDirectory(conf.directory);
  auto pidfile = Poco::File(basepath.resolve("server.pid"));
  _hostname = conf.host;
  _port = conf.port;

 

  try {
    // --- SQLite connection

    using namespace Poco::Data::Keywords;

    auto sqlitepath = Poco::Path(basepath).resolve("data.sqlite");
    LOGGER->info("Opening database {}", sqlitepath.toString());
    Poco::Data::SQLite::Connector::registerConnector();
    session = std::make_unique<Poco::Data::Session>(
        "SQLite", sqlitepath.absolute().toString());

    *session << "CREATE TABLE IF NOT EXISTS Config (key VARCHAR(30) PRIMARY "
                "KEY, value VARCHAR NOT NULL)",
        now;

    Poco::Data::Statement select(*session);
    int version = 0;
    *session << "PRAGMA foreign_keys = ON", now;
    select << "SELECT Value FROM Config WHERE key='version'", into(version),
        now;
    LOGGER->info("Database version is {}", version);

    switch (version) {
    case 0: {
      // New database
      *session << R"SQL(
          CREATE TABLE IF NOT EXISTS Auth (
            token VARCHAR(255) PRIMARY KEY, 
            validity DATETIME NOT NULL
          );
          
          CREATE TABLE IF NOT EXISTS Experiment (
            id INTEGER PRIMARY KEY,
            name VARCHAR(255) NOT NULL,
            timestamp DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
            CONSTRAINT unique_experiment UNIQUE(name, timestamp)
          );

          CREATE TABLE IF NOT EXISTS Task (
            id INTEGER PRIMARY KEY,
            experiment_id INTEGER NOT NULL,
            CONSTRAINT valid_experiment FOREIGN KEY (experiment_id) REFERENCES Experiment(id)
          );

          CREATE TABLE IF NOT EXISTS Tag (
            task_id INTEGER NOT NULL REFERENCES Task(id),
            key VARCHAR(255) NOT NULL,
            value JSON NOT NULL,
            CONSTRAINT tag_reference PRIMARY KEY(task_id, key)
          );

          CREATE TABLE IF NOT EXISTS Token (
            id INTEGER NOT NULL PRIMARY KEY,
            key VARCHAR(255) NOT NULL,
            capacity int NOT NULL,
            value int NOT NULL,
            CONSTRAINT unique_token UNIQUE(key)
          );

          CREATE TABLE IF NOT EXISTS LockedToken (
            token_id INTEGER NOT NULL REFERENCES Token(id),
            task_id INTEGER NOT NULL REFERENCES Task(id),
            value INTEGER NOT NULL,
            CONSTRAINT unique_lockedtoken UNIQUE(token_id, task_id)
          );

        )SQL",
          now;

      // Poco::Data::Statement update(*session);
      *session << "INSERT OR REPLACE INTO Config(key, value) VALUES "
                  "('version', ?)",
          use(DB_VERSION), now;
      break;
    }
    }

  } catch (Poco::Data::DataException &e) {
    LOGGER->error("Error while updating database: {}", e.displayText());
    throw;
  }

  LOGGER->info("Database update to version {}", DB_VERSION);
}


void MainServerContext::refresh(std::shared_ptr<Emitter> const & emitter) {   
  // TODO: implement refresh
  NOT_IMPLEMENTED();
}

void MainServerContext::kill(std::shared_ptr<Emitter> const & emitter, std::string const & jobId) {
  // TODO: implement kill
  NOT_IMPLEMENTED();
}

void MainServerContext::jobDetails(std::shared_ptr<Emitter> const & emitter, std::string const & jobId) {
  // TODO: implement jobDetails
  NOT_IMPLEMENTED();
}


// --- Experiment server context

ExperimentServerContext::ExperimentServerContext(Workspace & workspace, std::string const & host, int port, std::string const & htdocs) : _workspace(workspace) {
    _port = port;
    _hostname = host;
    _htdocs = htdocs;
}

void ExperimentServerContext::refresh(std::shared_ptr<Emitter> const & emitter) {
    _workspace.refresh(*emitter);
}

void ExperimentServerContext::jobCreation(Job const & job) {
  nlohmann::json j = { { "type", "JOB_ADD" }, { "payload", job.getJsonState() } };
  forEach([&j](auto & l) { l.send(j); });
}

void ExperimentServerContext::jobStatus(Job const & job) {
    nlohmann::json j = { { "type", "JOB_UPDATE" }, { "payload", {
      { "locator", job.locator().toString() },
      { "status", job.state() }
    }}};
    
    forEach([&j](auto & l) { l.send(j); });
}

void ExperimentServerContext::jobProgress(Job const & job) {
}

void ExperimentServerContext::kill(std::shared_ptr<Emitter> const & emitter, std::string const & jobId) {
    _workspace.kill(jobId);
}

void ExperimentServerContext::jobDetails(std::shared_ptr<Emitter> const & emitter, std::string const & jobId) {
  auto job = _workspace.getJob(jobId);
  if (job) {
    nlohmann::json dependencies;
    for(auto resource: job->dependencies()) {
      dependencies.push_back(job->getJobId());
    }
    nlohmann::json j = {
      { "type", "JOB_UPDATE" }, { "payload", {
        { "start", job->startTime() },
        { "end", job->endTime() },
        { "dependencies", dependencies },
        { "status", job->state() },
        { "locator", job->locator().toString() }
      }}
    };
    emitter->send(j);
  } else {
    throw argument_error("Could not find job");
  }
}

} // namespace xpm::rpc
