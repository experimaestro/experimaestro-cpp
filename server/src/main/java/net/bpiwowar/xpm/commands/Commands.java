package net.bpiwowar.xpm.commands;

/*
 * This file is part of experimaestro.
 * Copyright (c) 2014 B. Piwowarski <benjamin@bpiwowar.net>
 *
 * experimaestro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * experimaestro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 */

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.scheduler.Dependency;
import net.bpiwowar.xpm.utils.UUIDObject;
import net.bpiwowar.xpm.utils.log.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * A full command
 * <p>
 */
@Exposed
public class Commands extends AbstractCommand {
    final static private Logger LOGGER = Logger.getLogger();
    /**
     * The list of command status be executed
     * <p>
     * The command can refer status each other
     */
    transient ArrayList<AbstractCommand> commands = new ArrayList<>();

    /**
     * Default constructor (for DB serialization)
     */
    public Commands() {
    }

    /**
     * Construct with a set of command
     */
    public Commands(AbstractCommand... commands) {
        this.commands = new ArrayList<>(Arrays.asList(commands));
    }


    public Stream<Dependency> dependencies() {
        // Process our dependencies
        return Stream.concat(super.dependencies(), commands.stream().flatMap(AbstractCommand::dependencies));
    }

    @Override
    public Iterator<AbstractCommand> iterator() {
        return commands.iterator();
    }

    public int size() {
        return commands.size();
    }

    @Override
    public String toString() {
        return "Commands{" +
                "command=" + commands +
                '}';
    }

    public void add(AbstractCommand command) {
        if (command.outputRedirect == null) {
            command.outputRedirect = Redirect.INHERIT;
        }
        commands.add(command);
    }

    public void prepare(CommandContext env) {
        super.prepare(env);
        commands.forEach(c -> c.prepare(env));
    }

    @Override
    public Stream<? extends AbstractCommandComponent> allComponents() {
        return Stream.concat(super.allComponents(), commands.stream().flatMap(AbstractCommand::allComponents));
    }


    public void addUnprotected(String command) {
        add(new Command(new Unprotected(command)));
    }

    public AbstractCommand get(int i) {
        return commands.get(i);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Commands that = (Commands) o;
        return Objects.equals(commands, that.commands) &&
                Objects.equals(dependencies, that.dependencies);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commands, dependencies);
    }

    @Override
    public boolean needsProtection() {
        return commands.size() > 1;
    }

    @Override
    public Stream<AbstractCommand> commands() {
        return Stream.concat(Stream.concat(super.commands(), Stream.of(this)), commands.stream().flatMap(AbstractCommand::commands));
    }

    /**
     * Simplify the command
     *
     * @return A simplified command
     */
    public AbstractCommand simplify() {
        for (int i = 0; i < commands.size(); i++) {
            commands.set(i, commands.get(i).simplify());
        }

        if (commands.size() == 1) {
            // Copy dependencies
            final AbstractCommand command = commands.get(0);
            copyToCommand(command);
            return command;
        }
        return this;
    }

    @Override
    public void postJSONSave(JsonWriter out) throws IOException {
        super.postJSONSave(out);
        out.name("commands");
        out.beginArray();
        for (AbstractCommand command : commands) {
            out.value(command.getUUID());
        }

        out.endArray();
    }

    @Override
    public void postJSONLoad(Map<String, UUIDObject> map, JsonReader in, String name) throws IOException {
        switch (name) {
            case "commands":
                in.beginArray();
                while (in.peek() != JsonToken.END_ARRAY) {
                    commands.add((AbstractCommand) map.get(in.nextString()));
                }
                in.endArray();
                break;
            default:
                super.postJSONLoad(map, in, name);

        }
    }
}
