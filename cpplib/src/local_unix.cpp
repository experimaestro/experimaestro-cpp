#ifndef _WIN32

#include <__xpm/local.hpp>

// For Windows, see
// https://msdn.microsoft.com/en-us/library/windows/desktop/ms682425(v=vs.85).aspx

#include <cstdlib>
#include <fcntl.h>
#include <mutex>
#include <signal.h>
#include <thread>
#include <unistd.h>

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

  FileDescriptor(int fd) : fd(fd) {}

  ~FileDescriptor() { close(fd); }
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
      int fd = open(redirect.type == Redirection::NONE
                        ? "/dev/null"
                        : redirect.path.c_str(),
                    outputStream ? O_RDONLY : O_WRONLY | O_TRUNC | O_CREAT);
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
  std::thread stdout_thread, stderr_thread;

  bool closed = true;
  int pid = -1;

public:
  LocalProcess(LocalProcessBuilder &builder) {
    // Pipes for the different streams
    Pipe stdin_p(builder.stdin, false);
    Pipe stdout_p(builder.stdout, true);
    Pipe stderr_p(builder.stderr, true);

    pid_t pid = fork();

    if (pid < 0) {
      // Error
      throw exception("Could not fork");
    }

    if (pid == 0) {
      // Child process

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

      run(builder);

      // Don't go further
      _exit(0);
    }

    // Parent process

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
          builder.stdin.function(buffer.get(), static_cast<size_t>(n));
      });
    }

    if (stderr) {
      stderr_thread = std::thread([this, &builder]() {
        auto buffer = std::unique_ptr<char[]>(new char[buffer_size]);
        ssize_t n;
        while ((n = read(stderr->fd, buffer.get(), buffer_size)) > 0)
          builder.stdout.function(buffer.get(), static_cast<size_t>(n));
      });
    }
  }

  void run(ProcessBuilder &builder) {
    // Change current directory
    chdir(builder.workingDirectory.c_str());

    // Prepare the environment
    char * envp[builder.environment.size() + 1];
    envp[builder.environment.size()] = nullptr;
    std::vector<std::string> envarray;
    for (auto const &entry : builder.environment) {
      envarray.push_back(entry.first + "=" + entry.second);
      envp[envarray.size() - 1] = const_cast<char*>(envarray.back().c_str());
    }

    // Prepare the arguments
    char * args[builder.command.size()];
    args[builder.command.size() - 1] = nullptr;
    for (size_t i = 1; i < builder.command.size(); ++i) {
      args[i] = const_cast<char*>(builder.command[i].c_str());
    }

    // Execute the command
    execve(builder.command[0].c_str(), args, envp);
  }

  /// isRunning
  virtual bool isRunning() override { 
    return !closed; 
  }

  /// Exit code
  virtual int exitCode() override {
    int exit_status;
    waitpid(pid, &exit_status, 0);
    {
      std::lock_guard<std::mutex> lock(close_mutex);
      closed = true;
    }
    close_fds();

    if (exit_status >= 256)
      exit_status = exit_status >> 8;
    return exit_status;
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

  /// Write to stdin
  virtual bool writeStdin(std::string const &string) override {
    if (!stdin)
      throw std::invalid_argument("Can't write to an unopened stdin pipe. "
                                  "Please set open_stdin=true when "
                                  "constructing the process.");

    std::lock_guard<std::mutex> lock(stdin_mutex);
    if (stdin->fd) {
      if (::write(stdin->fd, string.c_str(), string.size()) >= 0) {
        return true;
      } else {
        return false;
      }
    }
    return false;
  }
};

ptr<Process> LocalProcessBuilder::start() {
  return std::make_shared<LocalProcess>(*this);
}

} // namespace

#endif