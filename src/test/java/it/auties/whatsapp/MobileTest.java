package it.auties.whatsapp;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.mobile.VerificationCodeMethod;
import org.junit.jupiter.api.Test;

import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

public class MobileTest {
    @Test
    public void run() {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> e.printStackTrace());
        Whatsapp.mobileBuilder()
                .lastConnection()
                .unregistered()
                .register(393495089819L, VerificationCodeMethod.CALL,  MobileTest::onScanCode)
                .join()
                .addLoggedInListener(MobileTest::onConnected)
                .addContactsListener((api, contacts) -> System.out.printf("Contacts: %s%n", contacts.size()))
                .addChatsListener(chats -> System.out.printf("Chats: %s%n", chats.size()))
                .addNodeReceivedListener(incoming -> System.out.printf("Received node %s%n", incoming))
                .addNodeSentListener(outgoing -> System.out.printf("Sent node %s%n", outgoing))
                .addDisconnectedListener(reason -> System.out.printf("Disconnected: %s%n", reason))
                .connect()
                .join()
                .awaitDisconnection();
    }

    private static void onConnected(Whatsapp api) {
        System.out.println("Connected to mobile api");
    }

    private static CompletableFuture<String> onScanCode() {
        System.out.println("Enter OTP: ");
        var scanner = new Scanner(System.in);
        return CompletableFuture.completedFuture(scanner.nextLine().trim());
    }
}
