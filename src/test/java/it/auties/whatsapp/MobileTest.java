package it.auties.whatsapp;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.contact.ContactJid;
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
                    new Thread(() -> {
                        while (true){
                            var qr = new Scanner(System.in).nextLine().trim();
                            System.out.println("Result: " + api.linkDevice(qr).join());
                            api.sendMessage(ContactJid.of("393495089819"), "Mobile").join();
                        }
                    }).start();
                })
                .addNodeReceivedListener(incoming -> System.out.printf("Received node %s%n", incoming))
                .addNodeSentListener(outgoing -> System.out.printf("Sent node %s%n", outgoing))
                .connectAndAwait()
                .join();
    }

    private static CompletableFuture<String> onScanCode() {
        System.out.println("Enter OTP: ");
        var scanner = new Scanner(System.in);
        return CompletableFuture.completedFuture(scanner.nextLine().trim());
    }
}
