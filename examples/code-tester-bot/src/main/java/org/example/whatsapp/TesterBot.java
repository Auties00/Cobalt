package org.example.whatsapp;

import it.auties.whatsapp4j.api.WhatsappAPI;

// This is the main class of our bot
public class TesterBot {
    public static void main(String... args) {
        // Create a new instance of WhatsappAPI
        var api = new WhatsappAPI();

        // Register the ban listener
        api.registerListener(new TesterBotListener(api));

        // Connect to WhatsappWeb's Servers
        api.connect();
    }
}