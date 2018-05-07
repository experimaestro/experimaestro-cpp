#ifndef EXPERIMAESTRO_SCRIPTBUILDER_HPP
#define EXPERIMAESTRO_SCRIPTBUILDER_HPP

#include <xpm/filesystem.hpp>
#include <xpm/launchers.hpp>

namespace xpm {

class ProcessBuilder;
class Command;
class Commands;
class AbstractCommand;
struct CommandContext;
class Connector;
class Workspace;
class Job;

/**
 * Base class for script building
 */
class ScriptBuilder {
public:
    /**
     * Environment
     */
    Environment environment;
    
    /**
     * Command line
     */
    std::shared_ptr<AbstractCommand> command;

    /**
     * Pre-process commands
     */
    std::shared_ptr<AbstractCommand> preprocessCommands;

    /**
     * The notification URL (if any)
     */
    std::string notificationURL;

    Redirect stdin;
    Redirect stdout;
    Redirect stderr;

    /**
     * Files that should be locked when beginning, and unlock at the end
     */
    std::vector<Path> lockFiles;

    
    virtual ~ScriptBuilder();

    /// Write the script
    virtual Path write(Workspace & ws, Connector const & connector, Path const & path, Job const & job) = 0;
};


class ShScriptBuilder : public ScriptBuilder {
public:
    /// Path to sh (default /bin/sh)
    std::string shPath;

    ShScriptBuilder();
    virtual Path write(Workspace & ws, Connector const & connector, Path const &path, Job const & job) override;

    static std::string protect_quoted(std::string const & text);
};

}

#endif