package it.auties.whatsapp.local;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.companion.CompanionDevice;
import it.auties.whatsapp.model.mobile.VerificationCodeMethod;

import java.net.URI;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class MobileRunner {
    public static void main(String[] args) {
        var scanner = new Scanner(System.in);
        System.out.println("Enter the phone number(with no +, spaces or parenthesis):");
        var phoneNumber = scanner.nextLong();
        var whatsapp = Whatsapp.mobileBuilder()
                .newConnection()
                .proxy(URI.create("http://wy961882248_1:999999@gate8.rola.vip:1066/"))
                .device(CompanionDevice.ios(false))
                .unregistered()
                .verificationCodeSupplier(MobileRunner::onScanCode)
                .verificationCodeMethod(VerificationCodeMethod.SMS)
                .register(phoneNumber)
                .join();
        System.out.println(whatsapp.keys().toString());
        whatsapp.addLoggedInListener(() -> System.out.println("Connected"))
                .connect()
                .join();
        CompletableFuture.delayedExecutor(10, TimeUnit.SECONDS)
                .execute(() -> whatsapp.disconnect().join());
    }

    private static CompletableFuture<String> onScanCode() {
        System.out.println("Enter OTP: ");
        var scanner = new Scanner(System.in);
        return CompletableFuture.completedFuture(scanner.nextLine());
    }
}

