package it.auties.whatsapp;

import it.auties.whatsapp.api.Whatsapp;
import org.junit.jupiter.api.Test;

// Just used for testing locally
public class WebForMobileTest {
    @Test
    public void run() {
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
