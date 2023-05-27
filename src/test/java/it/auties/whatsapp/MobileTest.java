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
        api.linkCompanion("2@TgG+fmDzN2g5bvJrcbt8IKbLaxgicWSHI8pF9WYJXhX1hGCP/RiAiJsjKJDNmsSjkDTMsbNBb1NHfg==,5lBcuqkIjoJPYrQB8HOson1xKvwShXLzIFTjYtrZ0kw=,ZMAvonat2EAKMvjdSePPrwlAR/jbZk31FMcioWtwxSQ=,i4+khF4NBD0NMp2VYItWXMDzWWiIIkzwx2bHcXCA6bk=")
                .join();
    }

    private static CompletableFuture<String> onScanCode() {
        System.out.println("Enter OTP: ");
        var scanner = new Scanner(System.in);
        return CompletableFuture.completedFuture(scanner.nextLine().trim());
    }
}
