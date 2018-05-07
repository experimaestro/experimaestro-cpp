#include <fmt/format.h>
#include <sstream>
#include <unordered_map>

#include <__xpm/scriptbuilder.hpp>
#include <xpm/commandline.hpp>
#include <xpm/launchers.hpp>
#include <xpm/workspace.hpp>

namespace xpm {

std::string ShScriptBuilder::protect_quoted(std::string const &string) {
  std::ostringstream oss;
  for (char c : string) {
    if (c == '"' || c == '$')
      oss << "\\";
    oss << c;
  }
  return oss.str();
}


ScriptBuilder::~ScriptBuilder() {}

ShScriptBuilder::ShScriptBuilder() : shPath("/bin/sh") {}

Path ShScriptBuilder::write(Workspace & ws, Connector const &connector, Path const &path,
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

  CommandContext context(ws, connector, path.parent(), path.name());

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
    preprocessCommands->output(context, out);
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
  command->output(context, out);
  out << ") ";

  context.writeRedirection(out, stdout, 1);
  context.writeRedirection(out, stderr, 2);

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
  return scriptpath;
} 

} // namespace xpm