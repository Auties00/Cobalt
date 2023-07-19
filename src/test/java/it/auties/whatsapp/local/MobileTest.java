package it.auties.whatsapp.local;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.mobile.VerificationCodeMethod;
import it.auties.whatsapp.model.mobile.VerificationCodeResponse;
import org.junit.jupiter.api.Test;

import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class MobileTest {
    @Test
    public void run() {
        Whatsapp.mobileBuilder()
                .lastConnection()
                .business(true)
                .unregistered()
                .verificationCodeMethod(VerificationCodeMethod.SMS)
                .verificationCodeSupplier(MobileTest::onScanCode)
                .verificationCaptchaSupplier(MobileTest::onCaptcha)
                .register(19176199769L)
                .join()
                .addLoggedInListener(api -> {
                    System.out.println("Connected");
                    var call = api.startCall(ContactJid.of("393495089819")).join();
                    System.out.println(call);
                    CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS)
                            .execute(() -> api.stopCall(call).join());
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
                .connectAwaitingLogout()
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
