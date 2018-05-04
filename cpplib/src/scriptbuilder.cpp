#include <fmt/format.h>
#include <sstream>
#include <unordered_map>

#include <__xpm/scriptbuilder.hpp>
#include <xpm/commandline.hpp>
#include <xpm/launchers.hpp>
#include <xpm/workspace.hpp>

namespace xpm {

std::string protect_quoted(std::string const &string) {
  std::ostringstream oss;
  for (char c : string) {
    if (c == '"' || c == '$')
      oss << "\\";
    oss << c;
  }
  return oss.str();
}

struct FolderContext : public CommandContext {
  Path folder;
  std::string name;
  std::unordered_map<std::string, int> counts;

  FolderContext(Path const &folder, std::string const &name)
      : folder(folder), name(name) {}

  Path getAuxiliaryFile(std::string const &prefix, std::string const &suffix) {
    std::string reference = name + "." + prefix + suffix;
    int &count = ++counts[reference];
    return folder.resolve(
        {fmt::format("{}_{0:2d}.{}{}", name, count, prefix, suffix)});
  }

  Path getWorkingDirectory() { return folder; }

  void close() {
    // Keep all the files
  }
};

ScriptBuilder::~ScriptBuilder() {}

ShScriptBuilder::ShScriptBuilder() : shPath("/bin/sh") {}

Path ShScriptBuilder::write(Connector const &connector, Path const &path,
                            Job const &job) {
  // First generate the run file
  Path directory = path.parent();
  Path scriptpath = directory.resolve({path.name() + ".sh"});
  Path donepath = directory.resolve({path.name() + ".done"});
  Path exitcodepath = directory.resolve({path.name() + ".code"});
  Path startlockPath = directory.resolve({path.name() + ".lock.start"});
  Path pidFile = directory.resolve({path.name() + ".pid"});

  std::unique_ptr<std::ostream> _out = connector.ostream(scriptpath);
  std::ostream &out = *_out;

  out << "#!" << shPath << std::endl;
  out << "# Experimaestro generated task" << std::endl << std::endl;

  FolderContext context(path.parent(), path.name());

  // --- Checks locks right away

  // Checks the main lock - if not there, we are not protected
  if (!lockFiles.empty()) {
    out << "# Checks that the locks are set" << std::endl;
    for (Path const &lockFile : lockFiles) {
      out << "test -f " << lockFile << " || (echo Locks not set; exit 017)"
          << std::endl;
    }
  }

  // Checks the start lock to avoid two experimaestro launched processes to
  // start
  out << "# Checks that the start lock is set, and removes it" << std::endl;
  out << "test -f " << connector.resolve(startlockPath)
      << " || (echo start lock not set; exit 017)" << std::endl;
  out << "rm -f " << connector.resolve(startlockPath) << std::endl;
  out << std::endl;

  // Use pipefail for fine grained analysis of errors in commands
  out << "set -o pipefail" << std::endl << std::endl;

  out << "echo $? > \"" << protect_quoted(connector.resolve(pidFile)) << "\""
      << std::endl
      << std::endl;

  for (auto const &pair : environment) {
    out << "export " << pair.first << "=\"" << protect_quoted(pair.second)
        << "\"" << std::endl;
  }

  // Adds notification URL to script
  if (!notificationURL.empty()) {
    out << "export %s=\"" << protect_quoted(notificationURL) << "/"
        << job.getId() << std::endl;
  }

  out << "cd \"%s\"" << std::endl, protect_quoted(connector.resolve(directory));

  // Write some command
  if (preprocessCommands) {
    writeCommands(context, out, *preprocessCommands);
  }

  // --- CLEANUP

  out << "cleanup() {" << std::endl;
  // Write something
  out << " echo Cleaning up 1>&2" << std::endl;
  // Remove traps
  out << " trap - 0" << std::endl;

  out << " rm -f " << connector.resolve(pidFile, directory) << ";" << std::endl;

  // Remove locks
  for (auto &file : lockFiles) {
    out << " rm -f " << connector.resolve(file) << std::endl;
  }

  // Remove temporary files
  command->forEach([&](CommandPart &c) {
    auto &namedRedirections = context.getNamedRedirections(c, false);
    for (Path const &file : namedRedirections.outputRedirections) {
      out << " rm -f " << connector.resolve(file, directory) << ";"
          << std::endl;
    }
    for (Path const &file : namedRedirections.errorRedirections) {
      out << " rm -f " << connector.resolve(file, directory) << ";"
          << std::endl;
    }
  });

  // Notify if possible
  if (!notificationURL.empty()) {
    out << " wget --tries=1 --connect-timeout=1 --read-timeout=1 --quiet -O "
      << "/dev/null \"$XPM_NOTIFICATION_URL/eoj\"" << std::endl;
  }

  // Kills remaining processes
  out << " test ! -z \"$PID\" && pkill -KILL -P $PID";

  out << "}" << std::endl;

  // --- END CLEANUP

  out << "# Set trap to cleanup when exiting" << std::endl;
  out << "trap cleanup 0" << std::endl;

  // Write the command
  out << "code=$?" << std::endl;
  out << "if test $code -ne 0; then" << std::endl;
  out << " echo $code > \""
      << protect_quoted(connector.resolve(exitcodepath, directory)) << "\""
      << std::endl,

      out << " exit $code" << std::endl;
  out << "fi" << std::endl;

  out << "%ncheckerror()  { local e; for e in \"$@\"; do [[ \"$e\" != 0 ]] && "
         "[[ "
         "\"$e\" != 141 ]] && exit $e; done; return 0; }%n"
      << std::endl;

  // The prepare all the command
  out << "(" << std::endl;
  writeCommands(context, out, *command);
  out << ") ";

  writeRedirection(context, out, stdout, 1);
  writeRedirection(context, out, stderr, 2);

  // Retrieve PID
  out << " & " << std::endl;
  out << "PID=$!" << std::endl;
  out << "wait $PID" << std::endl;

  out << "echo 0 > \"%s\"" << std::endl,
      protect_quoted(connector.resolve(exitcodepath, directory));
  out << "touch \"%s\"" << std::endl,
      protect_quoted(connector.resolve(donepath, directory));

  // Set the file as executable
  _out = nullptr;
  connector.setExecutable(path, true);
} // namespace xpm

void ShScriptBuilder::writeRedirection(Connector & connector,
    CommandContext &context,
    std::ostream &out,
    Redirect const &redirect, int stream) {
  switch (redirect.type) {
  case Redirection::INHERIT:
    break;
  case Redirection::FILE:
    out << " " 
        << stream << "> " 
        << protect_quote(connector.resolve(redirect.file, context.getWorkingDirectory()));
    break;
  default:
    throw exception("Unsupported output redirection type");
  }
}

void ShScriptBuilder::writeCommands(CommandContext &env, std::ostream &out,
                                    Command &commands) {
  // std::vector<ptr<Command>> list = commands.reorder();

  int detached = 0;

  for (Command command : list) {
    // Write files
    final CommandContext::NamedPipeRedirections namedRedirections =
        env.getNamedRedirections(command, false);

    // Write named pipes
    for (Path file : Iterables.concat(namedRedirections.outputRedirections,
                                      namedRedirections.errorRedirections)) {
      out << " mkfifo \""
          << protect_quoted(env.resolve(file, env.getWorkingDirectory()))
          << "\"" << std::endl;
    }

    if (command.inputRedirect != null &&
        command.inputRedirect.type() == Redirect.Type.READ) {
      out << " cat \""
          << protect_quoted(env.resolve(command.inputRedirect.file(),
                                        env.getWorkingDirectory()))
          << "\" | ";
    }

    if (auto commands = dynamic_cast<Commands *>(command)) {
      out << "(" << std::endl;
      writeCommands(env, out, command);
      out << ") ";
    } else {
      for (CommandComponent &argument : command.components()) {
        if (argument instanceof Unprotected) {
          out << argument.tostd::string const & (env);
        }

        out << ' ';
        if (argument instanceof Pipe) {
          out << " | ";
        } else if (argument instanceof SubCommand) {
          out << " (" << std::endl;
          writeCommands(env, out, ((SubCommand)argument).get());
          out << std::endl;
          out << " )";
        } else {
          out << protect(argument.tostd::string const &(env), SHELL_SPECIAL);
        }
      }
    }

    printRedirections(env, 1, out, command.getOutputRedirect(),
                      namedRedirections.outputRedirections);
    printRedirections(env, 2, out, command.getErrorRedirect(),
                      namedRedirections.errorRedirections);
    out << " || checkerror \"${PIPESTATUS[@]}\" ";

    if (env.detached(command)) {
      // Just keep a pointer
      out << " & CHILD_" << detached << "=$!" << std::endl;
      detached++;
    } else {
      // Stop if an error occurred
      out << " || exit $?" << std::endl;
    }
  }

  // Monitors detached jobs
  for (int i = 0; i < detached; i++) {
    out.format("wait $CHILD_%d || exit $?%n", i);
  }
}

void ShScriptBuilder::printRedirections(
    CommandContext &env, int stream, std::ostream &out,
    Redirect const &outputRedirect, std::vector<Path> const &outputRedirects) {
  if (!outputRedirects.isEmpty()) {

    // Special case : just one redirection
    if (outputRedirects.size() == 1 && outputRedirect == null) {
      writeRedirection(env, out, Redirect.file(outputRedirects.get(0)), stream);
    } else {
      out << " : " << stream << "> >(tee";
      for (Path file : outputRedirects) {
        out(" \"" <<  protect_quote(env.resolve(file, env.getWorkingDirectory()) << "\"",
      }
      writeRedirection(env, out, outputRedirect, stream);
      out << ")";
    }
  } else {
    // Finally, write the main redirection
    writeRedirection(env, out, outputRedirect, stream);
  }
}

} // namespace xpm