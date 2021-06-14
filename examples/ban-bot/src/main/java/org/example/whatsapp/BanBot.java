package org.example.whatsapp;

import it.auties.whatsapp4j.whatsapp.WhatsappAPI;

// This is the main class of our bot
public class BanBot {
    public static void main(String... args) {
        // Create a new instance of WhatsappAPI
        var api = new WhatsappAPI();

        // Register the ban listener
        api.registerListener(new BanBotListener(api));

        // Connect to WhatsappWeb's Servers
        api.connect();
    }
}
