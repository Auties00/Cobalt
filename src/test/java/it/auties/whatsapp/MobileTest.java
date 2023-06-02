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
                .register(16059009994L, VerificationCodeMethod.SMS,  MobileTest::onScanCode)
                .join()
                .addNodeReceivedListener(incoming -> System.out.printf("Received node %s%n", incoming))
                .addNodeSentListener(outgoing -> System.out.printf("Sent node %s%n", outgoing))
                .addLoggedInListener(mobileApi -> {
                    mobileApi.unlinkDevices().join();
                    mobileApi.linkDevice("2@X8qrznvWJ2KV6OXvIsByVMRupWYlDY9eML0JSUvq3VBvjJvQUIQKCs90LPPiEyvH/d4rVbUdh2pceA==,Q+c/d7VJI/1B6sT5z2QXcABAStlzkcTyf+v63ttTFDM=,3xRkGBd3Q31ndO86YNqqOlrx20/wAfheV4WmkaJEQhg=,WzYN4v9a8LkPf0eRyWYTEqs1QG39UIR/E/l2JVnYA9Y=");
                })
                .connectAndAwait()
                .join();
    }

    private static CompletableFuture<String> onScanCode() {
        System.out.println("Enter OTP: ");
        var scanner = new Scanner(System.in);
        return CompletableFuture.completedFuture(scanner.nextLine().trim());
    }
}
