package it.auties.whatsapp;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.contact.ContactJid;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// Just used for testing locally
public class WebForMobileTest {
    public static void main(String[] args) {
        var whatsapp = Whatsapp.webBuilder()
                .lastConnection()
                .build()
                .addLoggedInListener(api -> {
                    System.out.printf("Connected: %s%n", api.store().privacySettings());
                    Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
                        api.sendMessage(ContactJid.of("17154086027@s.whatsapp.net"), "Hello World");
                    }, 0, 5, TimeUnit.SECONDS);
                })
                .addDisconnectedListener(reason -> System.out.printf("Disconnected: %s%n", reason))
                .connect()
                .join();
        System.out.println("Connected");
        whatsapp.awaitDisconnection();
        System.out.println("Disconnected");
    }
}
