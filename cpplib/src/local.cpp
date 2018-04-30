#include <__xpm/local.hpp>

// For Windows, see
// https://msdn.microsoft.com/en-us/library/windows/desktop/ms682425(v=vs.85).aspx

namespace xpm {

#ifndef _WIN32

typedef int fd_type;

namespace {
struct FileDescriptor {};

struct Pipe {
  mode = mode;

  int pipefd[2];
  bool opened;

  Pipe(int mode) : mode(mode) {}

  // Get the file descriptor
  std::unique_ptr<FileDescriptor> fd() {
    if (!opened)
      return nullptr;

    close(pipefd[mode]);
    opened = false;
    return new std::make_unique(pipefd[1 - mode]);
  }

  bool open() {
    opened = pipe(pipefd) == 0;
    if (!opened)
      throw new exception();
  }

  void close() {
    if (opened) {
      close(pipefd[0]);
      close(pipefd[1]);
      opened = false;
    }
  }

  void associate(int to) {
    if (opened) {
      dup2(pipefd[mode], to);
      close();
    }
  }

  ~Pipe() { close(); }
}
} // namespace

class LocalProcess : public Process {
  Pipe stdin, stdout, stderr;
  int pid;

public:
  LocalProcess(LocalProcessBuilder &builder) {
    // Pipes for the different streams
    Pipe stdin_p, stdout_p, stderr_p;

    if (dynamic_cast<RedirectPipe *>(builder._stdin))
      stdin.open();

    if (dynamic_cast<RedirectPipe *>(builder._stdout))
      stdout.open();

    if (dynamic_cast<RedirectPipe *>(builder._stdout))
      stderr.open();

    id_type pid = fork();

    if (pid < 0) {
      // Error
      throw std::exception("Could not fork");
    } else if (pid == 0) {
      // Child process
      stdin.associate(0);
      stdout.associate(1);
      stderr.associate(2);

      // Close all the opened file handlers
      // Based on http://stackoverflow.com/a/899533/3808293
      int fd_max =
          static_cast<int>(sysconf(_SC_OPEN_MAX)); // truncation is safe
      for (int fd = 3; fd < fd_max; fd++) {
        close(fd);
      }

      setpgid(0, 0);

      if (function)
        function();

      _exit(EXIT_FAILURE);
    }

    // Parent process
    stdin_fd = stdin.fd();
    stdout_fd = stdin.fd();
    stderr_fd = stdin.fd();

    closed = false;

    if (stdout_fd) {
      stdout_thread = std::thread([this]() {
        auto buffer = std::unique_ptr<char[]>(new char[buffer_size]);
        ssize_t n;
        while ((n = read(*stdout_fd, buffer.get(), buffer_size)) > 0)
          read_stdout(buffer.get(), static_cast<size_t>(n));
      });
    }

    if (stderr_fd) {
      stderr_thread = std::thread([this]() {
        auto buffer = std::unique_ptr<char[]>(new char[buffer_size]);
        ssize_t n;
        while ((n = read(*stderr_fd, buffer.get(), buffer_size)) > 0)
          read_stderr(buffer.get(), static_cast<size_t>(n));
      });
    }
  }

  /// isRunning
  virtual void isRunning() override {}

  /// Exit code
  virtual int exitCode() override {
    int exit_status;
    waitpid(data.id, &exit_status, 0);
    {
      std::lock_guard<std::mutex> lock(close_mutex);
      closed = true;
    }
    close_fds();

    if (exit_status >= 256)
      exit_status = exit_status >> 8;
    return exit_status;
  }

  /// Kill
  virtual void kill() override {}

  /// Write to stdin
  virtual void writeStdin(std::string const &string) override {}
};
#endif

ptr<Process> LocalProcessBuilder::start() {
  return std::make_shared<LocalProcess>(*this);
}

} // namespace xpm