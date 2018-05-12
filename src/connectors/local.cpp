#include <fstream>
#include <xpm/connectors/local.hpp>

namespace xpm {

ptr<ProcessBuilder> LocalConnector::processBuilder() const {
    return std::make_shared<LocalProcessBuilder>();
}

std::string LocalConnector::resolve(Path const & path) const {
    return path.localpath();
}

std::unique_ptr<std::ostream> LocalConnector::ostream(Path const & path) const {
    return std::unique_ptr<std::ostream>(new std::ofstream(resolve(path)));
}

std::unique_ptr<std::istream> LocalConnector::istream(Path const & path) const {
    return std::unique_ptr<std::istream>(new std::ifstream(resolve(path)));
}

}
