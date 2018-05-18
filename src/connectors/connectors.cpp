#include <spdlog/fmt/fmt.h>
#include <__xpm/common.hpp>

#include <xpm/connectors/connectors.hpp>

namespace xpm {
    
Connector::~Connector() {}

std::string Connector::resolve(Path const & path) const {
    // If local, don't change it
    if (path.isLocal())
        return path.localpath();
    
    // Search among mounts
    auto it = _mounts.find(path.share());
    if (it == _mounts.end()) {
        throw io_error(fmt::format("Cannot resolve path {} (share does not exist)", path.toString()));
    }

    std::string const & p = path.localpart();

    for(auto const & mapping: it->second) {
        auto res = std::mismatch(mapping.serverpath.begin(), mapping.serverpath.end(), p.begin());
        if (res.first == mapping.serverpath.end()) {
            return mapping.localpath + "/" + p.substr(mapping.serverpath.size());
        }
    }

    throw io_error(fmt::format("Cannot resolve path {}", path.toString()));
}

std::string Connector::resolve(Path const & path, Path const & base) const {
  return Path(resolve(path)).relativeTo(resolve(base)).toString();
}


void Connector::mkdirs(Path const & path, bool createParents, bool errorExists) const {
  FileType type = fileType(path);
  if (type == FileType::DIRECTORY) {
    if (errorExists) throw io_error(fmt::format("Directory {} already exists", path));
    return;
  } else if (type != FileType::UNEXISTING) {
    if (errorExists) throw io_error(fmt::format("Path {} is not a directory", path));
  }

  // Create parents if needed
  Path parent = path.parent();
  if (createParents) {
    mkdirs(path.parent(), true, false);
  }

  // Make directory
  mkdir(path);
}



}