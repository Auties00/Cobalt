package org.example.whatsapp;

import it.auties.whatsapp4j.api.WhatsappAPI;

public class BanBot {
    public static void main(String... args) {
        var api = new WhatsappAPI();
        api.registerListener(new BanBotListener(api));
        api.connect();
    }
}
