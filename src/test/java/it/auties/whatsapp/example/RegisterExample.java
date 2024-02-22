package it.auties.whatsapp.example;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.companion.CompanionDevice;

import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class RegisterExample {
    public static void main(String[] args) {
        System.out.println("Enter the phone number: ");
        var phoneNumber = new Scanner(System.in).nextLong();
        Whatsapp.mobileBuilder()
                .newConnection()
                .device(CompanionDevice.ios(true))
                // .proxy(URI.create("http://username:password@host:port/")) Remember to set an HTTP proxy
                .unregistered()
                .verificationCodeSupplier(() -> {
                    System.out.println("Enter OTP: ");
                    return new Scanner(System.in).nextLine();
                })
                .register(phoneNumber)
                .join()
                .whatsapp()
                .addLoggedInListener(api -> CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS).execute(() -> {
                    System.out.println(api.keys().toString());
                    api.disconnect().join();
                }))
                .connect()
                .join();
    }
}
