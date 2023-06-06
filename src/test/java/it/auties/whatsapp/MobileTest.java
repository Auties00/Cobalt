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
                    api.linkDevice("2@uyVBue6H+nnQqE+IIRSOPB/HkWSaRdI8g8bJ/HhrwMJNww4SDwR/hpedtfH5Xk+ZBfT7fFW5hlLuKw==,7Fl6JuW6xHfq40f7EaHrqhePRZ9rzWpVQMv6xLc/MBM=,gAH3Oy7vFBDVbNV4QJ9/E+55vIDrnDK79Cdr+es2ghI=,005r3gGnvXuMw2PzXW9nhUaDc98zwbZE50KWlvpbaNQ=,1").join();
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
