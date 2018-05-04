#ifndef EXPERIMAESTRO_SCRIPTBUILDER_HPP
#define EXPERIMAESTRO_SCRIPTBUILDER_HPP

#include <xpm/filesystem.hpp>
#include <xpm/launchers.hpp>

namespace xpm {

class ProcessBuilder;
class Command;
struct CommandContext;
class Connector;
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
     * Commands
     */
    ptr<Command> command;

    /**
     * The notification URL (if any)
     */
    std::string notificationURL;

    /**
     * Files that should be locked when beginning, and unlock at the end
     */
    std::vector<Path> lockFiles;

    virtual ~ScriptBuilder();

    /// Write the script
    virtual Path write(Connector const & connector, Path const & path, Job const & job) = 0;
};


class ShScriptBuilder : public ScriptBuilder {
public:
    /// Path to sh (default /bin/sh)
    std::string shPath;

    ShScriptBuilder();
    virtual Path write(Connector const & connector, Path const &path, Job const & job) override;
private:
    void writeRedirection(CommandContext & env, std::ostream &out, Redirect redirect, int stream);
    void writeCommands(CommandContext & env, std::ostream &out, ptr<Command> commands);
    void printRedirections(CommandContext & env, int stream, std::ostream &out, 
        Redirect const & outputRedirect, std::vector<Path> const & outputRedirects);
};

}

#endif