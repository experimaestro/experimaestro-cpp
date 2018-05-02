#include <sstream>

#include <__xpm/scriptbuilder.hpp>
#include <xpm/launchers.hpp>

namespace xpm {

std::string protect_quoted(std::string const &string) {
    std::ostringstream oss;
    for(char c: string) {
        if (c == '"' || c == '$')
            oss << "\\";
        oss << c;
    }
    return oss.str();
}

struct NamedPipeRedirections {
  std::vector<Path> outputRedirections;
  std::vector<Path> errorRedirections;
}

NamedPipeRedirections EMPTY_REDIRECTIONS;

struct CommandContext {
  unordered_map<ptr<Command>, NamedPipeRedirections> namedPipeRedirectionsMap;

  NamedPipeRedirections &getNamedRedirections(AbstractCommand key,
                                              boolean create) {
    auto x = namedPipeRedirectionsMap.find(key);
    if (x != NamedPipeRedirections.end()) {
      return *x;
    }

    if (!create)
      return EMPTY_REDIRECTIONS;

    return namedPipeRedirectionsMap.put[key];
  }
};

struct FolderContext : public CommandContext {
  Path folder;
  std::string basename;

  Path getAuxiliaryFile(String prefix,
                        String suffix) throws FileSystemException {
    final String reference = format("%s.%s%s", name, prefix, suffix);
    MutableInt count = counts.get(reference);
    if (count == null) {
      count = new MutableInt();
      counts.put(reference, count);
    } else {
      count.increment();
    }
    return folder.resolve(
        format("%s_%02d.%s%s", name, count.intValue(), prefix, suffix));
  }

public
  Path getWorkingDirectory() throws IOException { return folder; }

public
  void close() throws IOException {
    // Keep all the files
  }
};

ScriptBuilder::~ScriptBuilder() {}

ShScriptBuilder::ShScriptBuilder() : shPath("/bin/sh") {}

ShScriptBuilder::write(Connector const & connector, Path const &path, Job const & job) {
  command().prepare(env);

  // First generate the run file
  std::unique_ptr<std::ostream> _out = connector.ostream(path);
  std::ostream &out = *_out;

  out << "#!" << shPath << std::endl;
  out << "# Experimaestro generated task" << std::endl << std::endl;

  FolderContext context{basepath, baseName};

  // --- Checks locks right away

  // Checks the main lock - if not there, we are not protected
  if (!lockFiles.isEmpty()) {
    std::cerr << "# Checks that the locks are set" << std::endl;
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

  if (pidFile != null) {
    out << "echo $? > \"" << protect_quoted(env.resolve(pidFile, null)) << "\""
        << std::endl,
        ;
  }

  out << std::endl;

  for (Map.Entry<String, String> pair : environment().entrySet()) {
    out << "export %s=\"%s\"" << std::endl, pair.getKey(),
        protect(pair.getValue());
  }

  // Adds notification URL to script
  if (notificationURL != null) {
    out << "export %s=\"" << protect_quoted(notificationURL) << "/%d\"" << Constants.XPM_NOTIFICATION_URL;
                ().toString()), job.getId());
  }

  out << "cd \"%s\"" << std::endl,
      protect(env.resolve(directory(), null), QUOTED_SPECIAL);

  // Write some command
  if (preprocessCommands != null) {
    writeCommands(env, writer, preprocessCommands);
  }

  // --- CLEANUP

  out << "cleanup() {" << std::endl;
  // Write something
  out << " echo Cleaning up 1>&2" << std::endl;
  // Remove traps
  out << " trap - 0" << std::endl;

  if (pidFile != null) {
    out << " rm -f " << env.resolve(pidFile, basepath) << ";" << std::endl;
  }

  // Remove locks
  for (auto &file : lockFiles) {
    out << " rm -f " << file < < < < std::endl;
  }

  // Remove temporary files
  command.forEach(
      [out, env](Command & c) {
        auto & namedRedirections = env.getNamedRedirections(c, false);
        for (Path const & file : namedRedirections.outputRedirections) {
            out << " rm -f " <<  env.resolve(file, basepath) << ";" << std::endl;
        }
        for (Path const & file : namedRedirections.errorRedirections) {
            out << " rm -f " <<  env.resolve(file, basepath) << ";" << std::endl;
        }
      }
  )

  // Notify if possible
  if (notificationURL != null) {
    out << " wget --tries=1 --connect-timeout=1 --read-timeout=1 --quiet -O "
        << "/dev/null \"$XPM_NOTIFICATION_URL/eoj\""
        << std::endl;
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
  out << " echo $code > \"" << protect_quoted(env.resolve(exitCodePath, basepath) << "\"" << std::endl,

  out << " exit $code" << std::endl;
  out << "fi" << std::endl;

  switch (input.type()) {
  case INHERIT:
    break;
  case READ:
    out << "cat \"%s\" | ", env.resolve(input.file(), basepath);
    break;
  default:
    throw new UnsupportedOperationException(
        "Unsupported input redirection type: " + input.type());
  }

  out << "%ncheckerror()  { local e; for e in \"$@\"; do [[ \"$e\" != 0 ]] && "
         "[[ "
         "\"$e\" != 141 ]] && exit $e; done; return 0; }%n"
      << std::endl;
  out << "(" << std::endl;

  // The prepare all the command
  writeCommands(env, writer, command);

  out << ") ";

  writeRedirection(env, out, output, 1);
  writeRedirection(env, out, error, 2);

  // Retrieve PID
  out << " & " << std::endl;
  out << "PID=$!" << std::endl;
  out << "wait $PID" << std::endl;

  out << "echo 0 > \"%s\"" << std::endl,
      protect(env.resolve(exitCodePath, basepath), QUOTED_SPECIAL);
  out << "touch \"%s\"" << std::endl,
      protect(env.resolve(donePath, basepath), QUOTED_SPECIAL);

  // Set the file as executable
  _out = nullptr;
  connector.executable(path, true);
} // namespace xpm

void ShScriptBuilder::writeRedirection(CommandContext &env, std::ostream &out,
                                       Redirect const &redirect, int stream) {
  switch (redirect.type) {
  case INHERIT:
    break;
  case WRITE:
    out << " " 
        << stream << "> " 
        << protect_quote(env.resolve(redirect.file(), env.getWorkingDirectory());
    break;
  default:
    throw exception("Unsupported output redirection type");
  }
}

void ShScriptBuilder::writeCommands(CommandContext &env, std::ostream &out,
                                    ptr<Command> commands) {
  std::vector<ptr<Command>> list = commands.reorder();

  int detached = 0;

  for (Command command : list) {
    // Write files
    final CommandContext.NamedPipeRedirections namedRedirections =
        env.getNamedRedirections(command, false);

    // Write named pipes
    for (Path file : Iterables.concat(namedRedirections.outputRedirections,
                                      namedRedirections.errorRedirections)) {
      out << " mkfifo \"" << protect_quoted(env.resolve(file, env.getWorkingDirectory())) << "\"" << std::endl;
                    
    }

    if (command.inputRedirect != null &&
        command.inputRedirect.type() == Redirect.Type.READ) {
      out << " cat \"" << protect_quoted(env.resolve(command.inputRedirect.file(), env.getWorkingDirectory())) << "\" | ";
    }

    if (auto commands = dynamic_cast<Commands*>(command)) {
      out << "(" << std::endl;
      writeCommands(env, writer, command);
      out << ") ";
    } else {
      for (CommandComponent & argument : command.components()) {
        if (argument instanceof Unprotected) {
          out << argument.toString(env);
        }

        out << ' ';
        if (argument instanceof Pipe) {
          out << " | ";
        } else if (argument instanceof SubCommand) {
          out << " (" << std::endl;
          writeCommands(env, writer, ((SubCommand)argument).get());
          out << std::endl;
          out << " )";
        } else {
          out << protect(argument.toString(env), SHELL_SPECIAL);
        }
      }
    }

    printRedirections(env, 1, writer, command.getOutputRedirect(),
                      namedRedirections.outputRedirections);
    printRedirections(env, 2, writer, command.getErrorRedirect(),
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
    writer.format("wait $CHILD_%d || exit $?%n", i);
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
      writeRedirection(env, writer, outputRedirect, stream);
      out << ")";
    }
  } else {
    // Finally, write the main redirection
    writeRedirection(env, writer, outputRedirect, stream);
  }
}
} // namespace xpm