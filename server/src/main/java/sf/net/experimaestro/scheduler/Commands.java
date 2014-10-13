package sf.net.experimaestro.scheduler;

import com.sleepycat.persist.model.Persistent;
import sf.net.experimaestro.annotations.Expose;
import sf.net.experimaestro.annotations.Exposed;
import sf.net.experimaestro.utils.Graph;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * A full command
 */
@Exposed
@Persistent
public class Commands implements Iterable<Command> {
    /**
     * The list of commands to be executed
     * <p/>
     * The commands can refer to each other
     */
    ArrayList<Command> commands = new ArrayList<>();

    /**
     * List of dependencies attached to this command
     *
     * The dependencies are not saved during serialization since this will be handled
     * by the resource
     */
    transient private ArrayList<Dependency> dependencies = new ArrayList<>();

    /**
     * Default constructor (for DB serialization)
     */
    public Commands() {
    }

    /**
     * Construct with a set of commands
     */
    public Commands(Command... commands) {
        this.commands = new ArrayList<>(Arrays.asList(commands));
    }

    /**
     * Re-order the commands so that the dependencies are fullfiled
     */
    public void reorder() {
        final ArrayList<Command> graph = new ArrayList<>(commands);
        Map<Command, ? extends Set<Command>> forward_edges = new IdentityHashMap<>();
        Map<Command, ? extends Set<Command>> backwards_edges = new IdentityHashMap<>();

        for (Command command : commands) {
            command.allComponents().forEach(argument -> {
                if (argument instanceof Command.CommandOutput) {
                    // FIXME Implements command output component
                    throw new NotImplementedException();
                }

            });
        }
        final ArrayList<Command> ordered_objects = Graph.topologicalSort(graph, forward_edges, backwards_edges);
        if (!graph.isEmpty())
            throw new IllegalArgumentException("Command has a loop");

        commands = ordered_objects;
    }

    @Expose("add_dependency")
    public void addDependency(Dependency dependency) {
        dependencies.add(dependency);
    }

    public void forEachDependency(Consumer<Dependency> consumer) {
        // Process our dependencies
        for(Dependency dependency: dependencies) {
            consumer.accept(dependency);
        }

        commands.stream().forEach(c -> c.forEachDependency(consumer));
    }

    @Override
    public Iterator<Command> iterator() {
        return commands.iterator();
    }

    public int size() {
        return commands.size();
    }

    @Override
    public String toString() {
        return "Commands{" +
                "commands=" + commands +
                '}';
    }

    public void add(Command command) {
        commands.add(command);
    }
}
