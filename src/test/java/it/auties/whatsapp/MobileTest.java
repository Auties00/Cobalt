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
        api.linkCompanion("2@pPq4IaRpVjqShvan3fr/2p82vdr2docObSY1Pd2GhokUhsKKn2sOlbDjjzCQ2PTKMCuOowpBDB12Ig==,wK+/Bxa1Z55vzjMQYQnmaOxI/BM9rEDCg9bQrezShzw=,QHwS7C3DfgfFZj4nl2lDXLFXmEyKVBk9ZJuUCtD2qjY=,zge1ZtIz5kPzJsss6HqIigGJt0m88buTz+Xn7Zxy2sk=")
                .join();
    }

    private static CompletableFuture<String> onScanCode() {
        System.out.println("Enter OTP: ");
        var scanner = new Scanner(System.in);
        return CompletableFuture.completedFuture(scanner.nextLine().trim());
    }
}
