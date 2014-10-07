package sf.net.experimaestro.manager;

import sf.net.experimaestro.annotations.Expose;
import sf.net.experimaestro.annotations.Exposed;
import sf.net.experimaestro.scheduler.Command;
import sf.net.experimaestro.scheduler.Dependency;

import java.util.ArrayList;
import java.util.List;

/**
 * A part of a command
 */
@Exposed
public class CommandPart {
    private Command command;
    private ArrayList<Dependency> dependencies = new ArrayList<>();

    public CommandPart(Command command) {
        this.command = command;
    }

    @Expose("add_dependency")
    public void addDependency(Dependency dependency) {
        dependencies.add(dependency);
    }

    @Expose
    public Command command() {
        return command;
    }

    @Expose
    public List<Dependency> dependencies() {
        return dependencies;
    };



}
