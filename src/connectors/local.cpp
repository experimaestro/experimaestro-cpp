#include <fstream>
#include <xpm/connectors/local.hpp>

namespace xpm {

using namespace std::chrono_literals;
const std::chrono::seconds LocalProcessBuilder::POLLING_INTERVAL = 1s;

ptr<ProcessBuilder> LocalConnector::processBuilder() const {
    return std::make_shared<LocalProcessBuilder>();
}

std::unique_ptr<std::ostream> LocalConnector::ostream(Path const & path) const {
    return std::unique_ptr<std::ostream>(new std::ofstream(resolve(path)));
}

std::unique_ptr<std::istream> LocalConnector::istream(Path const & path) const {
    return std::unique_ptr<std::istream>(new std::ifstream(resolve(path)));
}

}
