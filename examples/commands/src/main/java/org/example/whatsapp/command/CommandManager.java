package org.example.whatsapp.command;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class CommandManager {
    private static final CommandManager instance = new CommandManager();

    private final Set<Command> commands;

    private CommandManager() {
        this.commands = new HashSet<>();
    }

    public static CommandManager instance() {
        return instance;
    }

    public void addCommand(Command command) {
        commands.add(command);
    }

    public Optional<Command> findCommand(String filter) {
        return commands.stream()
                .filter(command -> command.command()
                        .equalsIgnoreCase(filter) || command.alias()
                        .contains(filter))
                .findAny();
    }
}
