package org.example.whatsapp;

import it.auties.whatsapp.api.Whatsapp;

import java.util.concurrent.ExecutionException;

// This is the main class of our bot
public class WhatsappBot {
    public static void main(String... args) throws ExecutionException, InterruptedException {
        // Create a new instance of WhatsappAPI
        Whatsapp.lastConnection()
                .connect()
                .get();
    }
}
