package net.bpiwowar.xpm.commands;

import java.io.Serializable;
import java.util.Objects;

/**
 *
 */
public class CommandString implements CommandComponent, Serializable {
    String string;

    protected CommandString() {
    }

    public CommandString(String string) {
        this.string = string;
    }

    @Override
    public String toString(CommandContext environment) {
        return string;
    }

    @Override
    public String toString() {
        return string;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommandString commandString = (CommandString) o;
        return Objects.equals(string, commandString.string);
    }

    @Override
    public int hashCode() {
        return Objects.hash(string);
    }
}
