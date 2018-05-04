#include <__xpm/local.hpp>

namespace xpm {

ptr<ProcessBuilder> LocalhostConnector::processBuilder() const {
    return std::make_shared<LocalProcessBuilder>();
}

std::string LocalhostConnector::resolve(Path const & path) const {
    return path.localpath();
}

std::unique_ptr<std::ostream> LocalhostConnector::(Path const & path) const {
    return new std::ofstream(resolve(path));
}

std::unique_ptr<std::istream> LocalhostConnector::istream(Path const & path) const {
    return new std::ifstream(resolve(path));
}

}
