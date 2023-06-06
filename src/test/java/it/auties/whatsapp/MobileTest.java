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
                    api.linkDevice("2@kjV3Lb98eje2swonZQIoDshZT6RhbLj27VFYnPF7djHIj+1zgnB1vEoZ/tNpjh0xa2lEo8ZszJj1gg==,5cxuFqgOzHyWB4uERAfZ2HGMsoVlwlpew0tnn9U60U8=,Z0BgyPBYjqwTmW/i0SZVEmT1eAOcH4WNO3P1bEGu7ls=,ceY0eUev6bFM8gaWmzEjFycIivvlZKdFePWNhG3P9Xk=,1").join();
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
