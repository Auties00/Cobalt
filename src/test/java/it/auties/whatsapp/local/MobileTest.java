package it.auties.whatsapp.local;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.companion.CompanionDevice;
import it.auties.whatsapp.model.mobile.VerificationCodeMethod;
import it.auties.whatsapp.model.mobile.VerificationCodeResponse;

import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

public class MobileTest {
    public static void main(String[] args) {
        Whatsapp.mobileBuilder()
                .lastConnection()
                .business(false)
                .device(CompanionDevice.ios())
                .unregistered()
                .verificationCodeSupplier(MobileTest::onScanCode)
                .verificationCodeMethod(VerificationCodeMethod.CALL)
                .register(393495089819L)
                .join()
                .connect()
                .join()
                .addLoggedInListener(api -> System.out.printf("Connected: %s%n", api.store().privacySettings()))
                .addFeaturesListener(features -> System.out.printf("Received features: %s%n", features))
                .addNewChatMessageListener((api, message) -> System.out.println(message.toJson()))
                .addContactsListener((api, contacts) -> System.out.printf("Contacts: %s%n", contacts.size()))
                .addChatsListener(chats -> System.out.printf("Chats: %s%n", chats.size()))
                .addNewslettersListener((newsletters) -> System.out.printf("Newsletters: %s%n", newsletters.size()))
                .addNodeReceivedListener(incoming -> System.out.printf("Received node %s%n", incoming))
                .addNodeSentListener(outgoing -> System.out.printf("Sent node %s%n", outgoing))
                .addActionListener ((action, info) -> System.out.printf("New action: %s, info: %s%n", action, info))
                .addSettingListener(setting -> System.out.printf("New setting: %s%n", setting))
                .addContactPresenceListener((chat, contact, status) -> System.out.printf("Status of %s changed in %s to %s%n", contact, chat.name(), status.name()))
                .addDisconnectedListener(reason -> System.out.printf("Disconnected: %s%n", reason))
                .connect()
                .join();
    }

    private static CompletableFuture<String> onScanCode() {
        System.out.println("Enter OTP: ");
        var scanner = new Scanner(System.in);
        return CompletableFuture.completedFuture(scanner.nextLine());
    }

    private static CompletableFuture<String> onCaptcha(VerificationCodeResponse response) {
        System.out.println("Enter captcha: ");
        var scanner = new Scanner(System.in);
        return CompletableFuture.completedFuture(scanner.nextLine());
    }
}
