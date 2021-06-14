package org.example.whatsapp.bot;

import it.auties.whatsapp4j.whatsapp.WhatsappAPI;
import org.example.whatsapp.command.CommandManager;
import org.example.whatsapp.command.HelloCommand;

// This is the main class of our bot
public class WhatsappBot {
    public static void main(String... args) {
        // Initialize the command manager
        var manager = new CommandManager();
        // Add all of our commands
        manager.addCommand(new HelloCommand());

        // Create a new instance of WhatsappAPI
        var api = new WhatsappAPI();

        // Register the ban listener and pass the command manager with a dependency injection
        api.registerListener(new WhatsappBotListener(api, manager));

        // Connect to WhatsappWeb's Servers
        api.connect();
    }
}