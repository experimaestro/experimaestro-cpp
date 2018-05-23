//
// Created by Benjamin Piwowarski on 19/11/2016.
//

#include <numeric>
#include <vector>
#include <fstream>
#include <sstream>
#include <streambuf>

#include <spdlog/fmt/fmt.h>

#include <xpm/common.hpp>
#include <xpm/filesystem.hpp>
#include <__xpm/common.hpp>

DEFINE_LOGGER("filesystem");

namespace xpm {

std::ostream &operator<<(std::ostream & out, Path const & path) {
  return out << path.toString();
}

Path::Path(std::string const &path) : Path("", path) {
}

Path::Path() : _path("/")  {
}

bool operator==(Path const & lhs, Path const & rhs) {
  return (lhs._share == rhs._share)
    && (lhs._path == rhs._path);
}


Path::Path(Path const &parent, std::initializer_list<std::string> const &relative) :
    Path(parent._share, std::accumulate(relative.begin(), relative.end(), parent._path == "/" ?  "" :  parent._path,
                          [](std::string &s, const std::string &piece) -> std::string { return s += "/" + piece; })) {

}
Path::Path(Path const &parent, std::vector<std::string> const &relative) :
    Path(parent._share, std::accumulate(relative.begin(), relative.end(), parent._path == "/" ?  "" :  parent._path,
                          [](std::string &s, const std::string &piece) -> std::string { return s += "/" + piece; })) {

}

Path::Path(std::string const &share, std::string const &path) : _share(share), _path(path) {
  // TODO: perform some cleanup
}

Path Path::operator/(std::string const & filename) const {
  return Path(*this, { filename });
}



Path Path::parent() const {
  unsigned long i = _path.rfind('/');
  if (i == 0) {
    return Path(_share, "/");
  }
  if (i != std::string::npos) {
    std::string parentpath = _path.substr(0, i);
    return Path(_share, parentpath);
  }

  return *this;
}

Path Path::changeExtension(std::string const & extension) const {
  auto pos = _path.rfind("/");
  if (pos == std::string::npos) pos = 0;
  pos = _path.find('.', pos);
  return Path(_share, pos == std::string::npos ? 
    _path + "." + extension 
    : _path.substr(0, pos) + extension
  );
}


std::string Path::toString() const {
  if (_share.empty()) return _path;
  return _share + ":" + _path;
}

std::string Path::share() const {
  return _share;
}

std::string const & Path::localpart() const {
  return _path;
}

std::string Path::name() const {
  auto pos = _path.rfind('/');

  return pos == std::string::npos ? _path : _path.substr(pos + 1);
}

bool Path::isRelative() const {
  return _path[0] != '/';
}

std::string Path::localpath() const {
  if (!isLocal())
    throw std::logic_error("Path " + toString() + " is not local");
  return _path;
}

bool Path::isLocal() const {
  return _share.empty();
}

bool Path::isRoot() const {
  return _path == "/";
}

Path Path::relativeTo(Path const & other) const {
  if (isRelative() || other.isRelative()) 
    throw argument_error(fmt::format("Cannot relativize with relative paths {} and {}",
      this->toString(), other.toString()));
  if (_share != other._share) return *this;

  std::string const & b = other._path;

  // TODO: normalize paths
  
  // TODO: find common root
  size_t i = 0;
  size_t last_slash = 0;
  while (b[i] == _path[i]) {
    if (b[i] == '/') last_slash = i;
    ++i;
  }

  if (b.size() == i) {
    if (_path.size() == i) {
      return Path(".");
    }
    if (_path[i] == '/') {
      last_slash = i;
    }
  }

  // Add back relative
  std::ostringstream oss;
  auto pos = last_slash;
  while ((pos = b.find('/', pos + 1)) != std::string::npos) {
    oss << "../";
  }
  oss << _path.substr(last_slash + 1);

  Path relative(oss.str());
  return relative;
}


std::string Path::getContent() const {
  if (isLocal()) {
    std::ifstream t(localpath());
    std::string str;

    t.seekg(0, std::ios::end);
    str.reserve(t.tellg());
    t.seekg(0, std::ios::beg);

    str.assign((std::istreambuf_iterator<char>(t)),
               std::istreambuf_iterator<char>());
    return str;
  }

  // auto rpcPath = rpc::Path::toPath(toString());
  // return rpcPath->read_all();
  NOT_IMPLEMENTED();
}

}