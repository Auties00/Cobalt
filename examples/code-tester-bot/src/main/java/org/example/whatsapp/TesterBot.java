package org.example.whatsapp;

import it.auties.whatsapp4j.api.WhatsappAPI;

public class TesterBot {
    public static void main(String... args) {
        var api = new WhatsappAPI();
        api.registerListener(new TesterBotListener(api));
        api.connect();
    }
}
