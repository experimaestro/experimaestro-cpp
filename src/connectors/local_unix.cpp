#ifndef _WIN32

// For Windows, see
// https://msdn.microsoft.com/en-us/library/windows/desktop/ms682425(v=vs.85).aspx

#include <condition_variable>
#include <mutex>

#include <cstdlib>
#include <dirent.h>
#include <fcntl.h>
#include <fstream>
#include <mutex>
#include <signal.h>
#include <sys/stat.h>
#include <sys/wait.h> // waitpid
#include <thread>
#include <unistd.h>

#include <Poco/DirectoryWatcher.h>
#include <Poco/Delegate.h>
#include <Poco/Path.h>

#include <__xpm/common.hpp>
#include <xpm/connectors/local.hpp>

DEFINE_LOGGER("xpm.local");

namespace {
// TODO: promote to parameter?
const size_t buffer_size = 8192;
} // namespace

namespace xpm {

typedef int fd_type;

struct FileDescriptor {
  int fd;

  FileDescriptor(FileDescriptor const &) = delete;
  FileDescriptor(FileDescriptor &&) = delete;

  operator bool() const { return fd != -1; }
  FileDescriptor(int fd) : fd(fd) {}

  ~FileDescriptor() {
    if (fd != -1)
      close(fd);
  }
};

struct Pipe {
  /// Redirect
  Redirect redirect;

  /// Input stream
  std::unique_ptr<FileDescriptor> input;
  std::unique_ptr<FileDescriptor> output;

  /// True if this pipe is for an output stream (stderr, stdout)
  bool outputStream;

  Pipe(Redirect const &redirect, bool outputStream)
      : redirect(redirect), outputStream(outputStream) {
    switch (redirect.type) {
    case Redirection::PIPE: {
      // Create a new pipe
      int pipefd[2];
      if (pipe(pipefd) != 0)
        throw exception();

      input = std::unique_ptr<FileDescriptor>(new FileDescriptor(pipefd[0]));
      output = std::unique_ptr<FileDescriptor>(new FileDescriptor(pipefd[1]));
      break;
    }
    case Redirection::NONE:
    case Redirection::INHERIT:
    case Redirection::FILE:
      break;
    }
  }

  Pipe(Pipe const &) = delete;

  // Get the file descriptor
  std::unique_ptr<FileDescriptor> pipefd() {
    if (redirect.type == Redirection::PIPE) {
      return std::move(outputStream ? input : output);
    }

    return nullptr;
  }

  /// Associate the stream to a given file descriptor
  void associate(int tofd) {
    switch (redirect.type) {
    case Redirection::INHERIT:
      break;

    case Redirection::FILE:
    case Redirection::NONE: {
      auto path = redirect.type == Redirection::NONE ? "/dev/null"
                                                     : redirect.path.c_str();
      LOGGER->debug("Changing redirection for fd {} to path {}", tofd, path);
      int fd = open(
          path, outputStream ? O_WRONLY | O_TRUNC | O_CREAT : O_RDONLY, 0666);
      if (fd < 0)
        throw exception();
      dup2(fd, tofd);
      close(fd);
      break;
    }

    case Redirection::PIPE: {
      auto &p = outputStream ? output : input;
      if (p) {
        // Associate to one output
        dup2(p->fd, tofd);
      }
      input = nullptr;
      output = nullptr;
    }
    }
  }
};

class LocalProcess : public Process {
  std::unique_ptr<FileDescriptor> stdin, stdout, stderr;
  std::mutex close_mutex;
  std::mutex stdin_mutex;
  std::mutex stdout_mutex;
  std::mutex stderr_mutex;
  std::thread stdout_thread, stderr_thread;

  bool closed = true;
  int pid = -1;

public:
  ~LocalProcess() { LOGGER->debug("Deleting LocalProcess", (void *)this); }

  LocalProcess(LocalProcessBuilder &builder) {
    // Pipes for the different streams
    LOGGER->debug("Creating LocalProcess", (void *)this);
    Pipe stdin_p(builder.stdin, false);
    Pipe stdout_p(builder.stdout, true);
    Pipe stderr_p(builder.stderr, true);

    pid_t pid = fork();

    if (pid < 0) {
      // Error
      throw exception("Could not fork");
    }

    if (pid == 0) {
      // --- Child process: the new one

      // Associate with stdin/stdout/stderr
      stdin_p.associate(0);
      stdout_p.associate(1);
      stderr_p.associate(2);

      // Close all the opened file handlers
      // Based on http://stackoverflow.com/a/899533/3808293
      int fd_max =
          static_cast<int>(sysconf(_SC_OPEN_MAX)); // truncation is safe
      for (int fd = 3; fd < fd_max; fd++) {
        close(fd);
      }

      // Detach
      setpgid(0, 0);

      // and run!
      run(builder);

      // Don't go further
      _exit(0);
    }

    // --- Parent process 

    // Get pipes if any
    stdin = stdin_p.pipefd();
    stdout = stdin_p.pipefd();
    stderr = stdin_p.pipefd();

    // We are running!
    closed = false;

    if (stdout) {
      stdout_thread = std::thread([this, &builder]() {
        auto buffer = std::unique_ptr<char[]>(new char[buffer_size]);
        ssize_t n;
        while ((n = read(stdout->fd, buffer.get(), buffer_size)) > 0)
          builder.stdout.function(buffer.get(), static_cast<size_t>(n));
      });
    }

    if (stderr) {
      stderr_thread = std::thread([this, &builder]() {
        auto buffer = std::unique_ptr<char[]>(new char[buffer_size]);
        ssize_t n;
        while ((n = read(stderr->fd, buffer.get(), buffer_size)) > 0)
          builder.stderr.function(buffer.get(), static_cast<size_t>(n));
      });
    }
  }

  void run(ProcessBuilder &builder) {
    // Change current directory
    chdir(builder.workingDirectory.c_str());

    // Prepare the environment
    char *envp[builder.environment.size() + 1];
    envp[builder.environment.size()] = nullptr;
    std::vector<std::string> envarray;
    LOGGER->info("Setting up the environment");
    for (auto const &entry : builder.environment) {
      LOGGER->info("ENV: {}={}", entry.first, entry.second);
      envarray.push_back(entry.first + "=" + entry.second);
      envp[envarray.size() - 1] = const_cast<char *>(envarray.back().c_str());
    }

    // Prepare the arguments
    char *args[builder.command.size()];
    args[builder.command.size() - 1] = nullptr;
    for (size_t i = 1; i < builder.command.size(); ++i) {
      args[i] = const_cast<char *>(builder.command[i].c_str());
    }

    // Execute the command
    execve(builder.command[0].c_str(), args, envp);
  }

  /// isRunning
  virtual bool isRunning() override { return !closed; }

  /// Exit code
  virtual int exitCode() override {
    int exit_status;
    auto code = waitpid(pid, &exit_status, 0);

    // FIXME: not clear what to wait (PID or -PID)
    LOGGER->info("Local unix process exit status is {} (exited={}, signaled={}, stopped={})", WEXITSTATUS(exit_status), WIFEXITED(exit_status), WIFSIGNALED(exit_status), WIFSTOPPED(exit_status));

    if (code == -1) {
      LOGGER->error("Error with waitpid: {} - waiting with -PID", strerror(errno));

      code = waitpid(-pid, &exit_status, 0);
      if (code == -1) {
        LOGGER->error("Error with waitpid {}", strerror(errno));
        throw std::runtime_error("waitpid error could not be handled");
      }
    }

    // Signals that the process ended
    {
      std::lock_guard<std::mutex> lock(close_mutex);
      closed = true;
    }
    close_fds();

    LOGGER->info("Local unix process exit status is {} (exited={}, signaled={}, stopped={})", WEXITSTATUS(exit_status),
      WIFEXITED(exit_status), WIFSIGNALED(exit_status), WIFSTOPPED(exit_status));

    return WEXITSTATUS(exit_status);
  }

  void close_fds() noexcept {
    if (stdout_thread.joinable())
      stdout_thread.join();
    if (stderr_thread.joinable())
      stderr_thread.join();

    stdin = nullptr;
    stdout = nullptr;
  }

  /// Kill
  virtual void kill(bool force) override {
    std::lock_guard<std::mutex> lock(close_mutex);
    if (pid > 0 && !closed) {
      if (force)
        ::kill(-pid, SIGTERM);
      else
        ::kill(-pid, SIGINT);
    }
  }

  virtual long write(void * s, long count) override {
    if (!stdin)
      throw std::invalid_argument("Can't write to an unopened stdin pipe. "
                                  "Please set open_stdin=true when "
                                  "constructing the process.");

    std::lock_guard<std::mutex> lock(stdin_mutex);
    if (stdin->fd) {
      return ::write(stdin->fd, s, count);
    }
    return -1;
  }

  virtual void eof() override {
    stdin = nullptr;
  }


};

ptr<Process> LocalProcessBuilder::start() {
  return std::make_shared<LocalProcess>(*this);
}

void LocalConnector::setExecutable(Path const &path, bool flag) const {
  if (chmod(path.localpath().c_str(), S_IRWXU) != 0) {
    throw io_error(fmt::format("Could not chmod {} to be executable ({})", path,
                               strerror(errno)));
  }
}

FileType LocalConnector::fileType(Path const &path) const {
  struct stat s;
  if (stat(path.localpath().c_str(), &s) != 0)
    return FileType::UNEXISTING;

  if (s.st_mode & S_IFDIR)
    return FileType::DIRECTORY;

  if (s.st_mode & S_IFREG)
    return FileType::FILE;

  return FileType::OTHER;
}

void LocalConnector::mkdir(Path const &path) const {
  // Make directory
  if (::mkdir(path.localpath().c_str(), 0777) != 0) {
    throw io_error(fmt::format("Could not create directory {} ({})", path,
                               strerror(errno)));
  }
}

void LocalConnector::createFile(Path const &path, bool errorIfExists) const {
  std::ofstream out(path.localpath());
}

struct DeleteListener {
  std::mutex mutex;
  std::condition_variable cv;
  std::string filename; ///< Filename to watch
  bool deleted = false;

  DeleteListener(std::string const & filename) : filename(filename) {}

  void onFileDeleted(const void *, const Poco::DirectoryWatcher::DirectoryEvent& removeEvent) {
    auto filename = Poco::Path(removeEvent.item.path()).getFileName();
    LOGGER->debug("Notification in directory: file {} deleted", filename);
    if (this->filename == filename) {
      std::lock_guard<std::mutex> lock(mutex);
      deleted = true;
      cv.notify_all();
    }
  }
};

std::unique_ptr<Lock> LocalConnector::lock(Path const &path) const {
  // Loop until the lock is taken
  LOGGER->debug("Trying to lock {}", path);
  while (!FileDescriptor(open(path.localpath().c_str(), O_CREAT | O_EXCL, S_IRWXU))) {
    LOGGER->info("Waiting for lock file {}", path);
    // Exit if the path does not exists
    auto ft = fileType(path);
    if (ft != FileType::FILE) {
      throw io_error(fmt::format(
          "Lock path {} already exists and is not a file", path.localpath()));
    }

    Poco::DirectoryWatcher dw (path.parent().localpath(), Poco::DirectoryWatcher::DW_ITEM_REMOVED);
    DeleteListener listener(path.name());
    dw.itemRemoved += Poco::delegate(&listener, &DeleteListener::onFileDeleted);
    std::unique_lock<std::mutex> lock(listener.mutex);

    LOGGER->debug("Waiting for lock file {} to be removed", path);
    listener.cv.wait(lock, [&] { 
      return listener.deleted; 
    });
  }

  return std::unique_ptr<Lock>(new FileLock(
      const_cast<LocalConnector *>(this)->shared_from_this(), path));
}

void LocalConnector::remove(Path const &path, bool recursive) const {
  FileType ft = fileType(path);
  if (ft != FileType::DIRECTORY) {
    if (unlink(path.localpath().c_str()) != 0) {
      throw io_error(
          fmt::format("Could not remove {}: {}", path, strerror(errno)));
    }
  } else {
    if (recursive) {
      DIR *dir;
      struct dirent *ent;
      if ((dir = opendir(path.localpath().c_str())) != NULL) {
        /* print all the files and directories within directory */
        try {
          while ((ent = readdir(dir)) != NULL) {
            std::string name = ent->d_name;
            if (name != "." && name != "..") {
              LOGGER->debug("Recursive: removing path {}", path / name);
              remove(path / name, recursive);
            }
          }
        } catch (std::exception const &e) {
          closedir(dir);
          throw e;
        }
        closedir(dir);
      } else {
        throw io_error(fmt::format("Could not list the directory {}: {}", path,
                                   strerror(errno)));
      }
    }

    if (rmdir(path.localpath().c_str()) != 0) {
      throw io_error(
          fmt::format("Could not remove {}: {}", path, strerror(errno)));
    }

  }
}

} // namespace xpm

#endif
