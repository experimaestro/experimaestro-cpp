#include <__xpm/local.hpp>

virtual ptr<ProcessBuilder> LocalhostConnector::processBuilder() override {
    return std::make_shared<LocalProcessBuilder>();
}

std::string LocalhostConnector::resolve(Path const & path) override {
    return path.localpath();
}
