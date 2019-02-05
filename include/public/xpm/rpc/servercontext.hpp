/**
 * Defines the different context for the app (main or experiment context)
 */
#ifndef PROJECT_RPC_SERVERCONTEXT_H
#define PROJECT_RPC_SERVERCONTEXT_H

#include <memory>
#include <unordered_set>

#include <xpm/json.hpp>
#include <Poco/File.h>

namespace Poco {
namespace Data {
    class Session;
} // Data
} // Poco

namespace xpm {
    class Workspace;
    class Job;
} // xpm

namespace xpm::rpc {

struct ServerContextListener {
    virtual void jobSubmitted(xpm::Job const & job) {}
};

class ServerContext {
protected:
    int _port;
    std::string _hostname;
    std::string _htdocs;
    std::unordered_set<ServerContextListener*> _listeners;
public:
    virtual ~ServerContext();

    virtual nlohmann::json handle(nlohmann::json &message);
    std::unique_ptr<Poco::File> pidFile() const;

    int port();
    std::string const & host();
    std::string const & htdocs();

    void add(ServerContextListener *);
    void remove(ServerContextListener *);
    void forEach(std::function<void(ServerContextListener&)> );
};

class MainServerContext : public ServerContext {
public:
    MainServerContext();
    virtual ~MainServerContext();
private:
  std::unique_ptr<Poco::Data::Session> session;
};

class ExperimentServerContext : public ServerContext {
public:
    ExperimentServerContext(Workspace & workspace, std::string const & host, int port, std::string const & htdocs);
private:
    Workspace & _workspace;
};

} // namespace xpm

#endif