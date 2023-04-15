package it.auties.whatsapp;

import it.auties.whatsapp.api.Whatsapp;

import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

public class MobileTest {
    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> e.printStackTrace());
        Whatsapp.mobileBuilder()
                .lastConnection()
                .unregistered()
                .register(393495089819L, MobileTest::onScanCode)
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
        api.unlinkCompanions().join();
        System.out.println("Connected to mobile api");
        api.linkCompanion("2@Q8cjUDZhhqW9mFFn5bMaGm9WFhQaCs+ZCoAPHozUA1RgJmficSMkt+1YeE0tkOjTTK92k89mQ8u0qA==,xYwjBykFvbvZCNrC+wncFGDa7eQCsPMCZwhzNnxYRgM=,IYmyatRvixSxt135z7luD6yZcDCuyI4KABhrdrndAFI=,KbXH9sj4bEmI4Kp8BBX+A3KboerISLAisWCiLSYxKzI=").join();
    }

    private static CompletableFuture<String> onScanCode() {
        System.out.println("Enter OTP: ");
        var scanner = new Scanner(System.in);
        return CompletableFuture.completedFuture(scanner.nextLine().trim());
    }
}
