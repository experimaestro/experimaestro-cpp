/** Classes related to localhost launchers
 */
#ifndef EXPERIMAESTRO__LOCAL_HPP
#define EXPERIMAESTRO__LOCAL_HPP

#include <xpm/connectors/connectors.hpp>
#include <xpm/launchers/launchers.hpp>

namespace xpm {

class LocalProcess;

/** Localhost connector */
class LocalConnector : public Connector {
public:
  virtual std::shared_ptr<ProcessBuilder> processBuilder() const override;
  virtual void setExecutable(Path const & path, bool flag) const override;
  virtual void mkdir(Path const & path) const override;
  virtual FileType fileType(Path const & path) const override;

  virtual void createFile(Path const &path, bool errorIfExists = false) const override;
  virtual void remove(Path const &path, bool recursive=false) const override;
  virtual std::unique_ptr<Lock> lock(Path const &path, std::chrono::seconds const &) const override;

  std::unique_ptr<std::ostream> ostream(Path const & path) const override;
  std::unique_ptr<std::istream> istream(Path const & path) const override;
};

class LocalProcessBuilder : public ProcessBuilder {
public:
  virtual std::shared_ptr<Process> start() override;
  friend class LocalProcess;

  /// function to execute (if no command)
  std::function<void()> function;
};

}

#endif 