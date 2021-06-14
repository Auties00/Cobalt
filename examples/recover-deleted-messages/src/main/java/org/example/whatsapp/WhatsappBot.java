package org.example.whatsapp;

import it.auties.whatsapp4j.whatsapp.WhatsappAPI;

// This is the main class of our bot
public class WhatsappBot {
    public static void main(String... args) {
        // Create a new instance of WhatsappAPI
        var api = new WhatsappAPI();

        // Calling this method ensures that all listeners annotated with @RegisterListener will be automatically
        api.autodetectListeners();

        // Connect to WhatsappWeb's Servers
        api.connect();
    }
}
