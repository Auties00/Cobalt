package it.auties.whatsapp;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.mobile.VerificationCodeMethod;
import org.junit.jupiter.api.Test;

import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

public class MobileTest {
    @Test
    public void run() {
        Whatsapp.mobileBuilder()
                .lastConnection()
                .unregistered()
                .register(16059009994L, VerificationCodeMethod.SMS ,  MobileTest::onScanCode)
                .join()
                .addLoggedInListener(api -> {
                    api.unlinkDevices().join();
                    api.linkDevice("2@oTI3JSmBc2ZWVdS6MGmDz7h0MErmaOWqJMAP+PG0bSnGJIs31E9Wdft17SEj1sjx3Ye4OlV6L7bV7g==,kVLGobUkIdhMKlW+ss4ZG7PHr3tPNPpT21YM4pzSAgE=,rxCRpPY38BS188pJE73NiVTtGuUHjbNS7q295EHWZHY=,PsezYgJB4eycVv32Yf1LKeRiDrcDF5TML91Q1Wrgjzs=,1").join();
                })
                .addContactsListener((api, contacts) -> System.out.printf("Contacts: %s%n", contacts.size()))
                .addChatsListener(chats -> System.out.printf("Chats: %s%n", chats.size()))
                .addNodeReceivedListener(incoming -> System.out.printf("Received node %s%n", incoming))
                .addNodeSentListener(outgoing -> System.out.printf("Sent node %s%n", outgoing))
                .addActionListener((action, info) -> System.out.printf("New action: %s, info: %s%n", action, info))
                .addSettingListener(setting -> System.out.printf("New setting: %s%n", setting))
                .addContactPresenceListener((chat, contact, status) -> System.out.printf("Status of %s changed in %s to %s%n", contact, chat.name(), status.name()))
                .addAnyMessageStatusListener((chat, contact, info, status) -> System.out.printf("Message %s in chat %s now has status %s for %s %n", info.id(), info.chatName(), status, contact == null ? null : contact.name()))
                .addChatMessagesSyncListener((chat, last) -> System.out.printf("%s now has %s messages: %s%n", chat.name(), chat.messages().size(), !last ? "waiting for more" : "done"))
                .addDisconnectedListener(reason -> System.out.printf("Disconnected: %s%n", reason))
                .connectAndAwait()
                .join();
    }

    private static CompletableFuture<String> onScanCode() {
        System.out.println("Enter OTP: ");
        var scanner = new Scanner(System.in);
        return CompletableFuture.completedFuture(scanner.nextLine().trim());
    }
}
