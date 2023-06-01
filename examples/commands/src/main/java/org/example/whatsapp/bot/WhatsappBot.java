package org.example.whatsapp.bot;

import it.auties.whatsapp.api.Whatsapp;
import org.example.whatsapp.command.CommandManager;
import org.example.whatsapp.command.HelloCommand;

// This is the main class of our bot
public class WhatsappBot {
    public static void main(String... args) {
        // Initialize the command manager
        CommandManager.instance()
                .addCommand(new HelloCommand());

        // Create a new instance of WhatsappAPI
        Whatsapp.webBuilder()
                .lastConnection()
                .build()
                .addLoggedInListener(() -> System.out.println("Connected!"))
                .connectAndAwait()
                .join();
    }
}