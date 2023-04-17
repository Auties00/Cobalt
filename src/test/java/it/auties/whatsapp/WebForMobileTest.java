package it.auties.whatsapp;

import it.auties.whatsapp.api.Whatsapp;

// Just used for testing locally
public class WebForMobileTest {
    public static void main(String[] args) {
        var whatsapp = Whatsapp.webBuilder()
                .newConnection()
                .build()
                .addLoggedInListener(api -> System.out.printf("Connected: %s%n", api.store().privacySettings()))
                .addDisconnectedListener(reason -> System.out.printf("Disconnected: %s%n", reason))
                .connect()
                .join();
        System.out.println("Connected");
        whatsapp.awaitDisconnection();
        System.out.println("Disconnected");
    }
}
