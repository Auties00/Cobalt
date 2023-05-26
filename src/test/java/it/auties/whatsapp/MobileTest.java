package it.auties.whatsapp;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.mobile.VerificationCodeMethod;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

public class MobileTest {
    @Test
    public void run() {
        Whatsapp.mobileBuilder()
                .lastConnection()
                .unregistered()
                .register(17405281037L, VerificationCodeMethod.SMS,  MobileTest::onScanCode)
                .join()
                .addNodeReceivedListener(incoming -> System.out.printf("Received node %s%n", incoming))
                .addNodeSentListener(outgoing -> System.out.printf("Sent node %s%n", outgoing))
                .addLoggedInListener(MobileTest::onConnected)
                .connectAndAwait()
                .join();
    }

    @SneakyThrows
    private static void onConnected(Whatsapp api) {
        System.out.println("Connected to mobile api");
        api.unlinkCompanions().join();
        api.linkCompanion("2@zfstw0IOax9aHXCzfG776wer1sVU2O25zuvDfoHWifXpYMqVJdifXG7mleR1Mgi7lencbGng9jZMgA==,Mxo4l7+lCnzFZFuH8BHP2DxEToRTNbaNKhugHOT5T18=,eeeCB9yKEZymBpJAQlFXf/v/XRvzmbF3EgKNUPM3QU8=,zJCSiuk33TbsVU4e3SrK5yH/tV49gdPq+OnKoUz7f40=")
                .join();
    }

    private static CompletableFuture<String> onScanCode() {
        System.out.println("Enter OTP: ");
        var scanner = new Scanner(System.in);
        return CompletableFuture.completedFuture(scanner.nextLine().trim());
    }
}
