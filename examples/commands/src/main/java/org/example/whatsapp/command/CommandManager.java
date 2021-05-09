package org.example.whatsapp.command;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class CommandManager {
    private final Set<Command> commands;
    public CommandManager(){
        this.commands = new HashSet<>();
    }

    public void addCommand(Command command){
        commands.add(command);
    }

    public Optional<Command> findCommand(String filter){
        return commands.stream()
                .filter(command -> command.command().equalsIgnoreCase(filter) || command.aliases().contains(filter))
                .findAny();
    }
}
