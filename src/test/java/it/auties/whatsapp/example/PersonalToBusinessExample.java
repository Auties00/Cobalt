package it.auties.whatsapp.example;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.companion.CompanionDevice;
import it.auties.whatsapp.model.mobile.SixPartsKeys;
import it.auties.whatsapp.model.mobile.VerificationCodeMethod;

import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

public class PersonalToBusinessExample {
    public static void main(String[] args) {
        System.out.println("Enter the six parts of the account you want to migrate to business:");
        var sixParts = new Scanner(System.in).nextLine();
        var sixPartsKeys = SixPartsKeys.of(sixParts);
        var codeFuture = new CompletableFuture<String>();
        Whatsapp.mobileBuilder()
                .newConnection(sixPartsKeys)
                .device(CompanionDevice.ios(false))
                .registered()
                .orElseThrow()
                .addRegistrationCodeListener(otp -> codeFuture.complete(String.valueOf(otp)))
                .connect()
                .thenApplyAsync(api -> {
                    // Register the account and use codeFuture as an otp provider
                    Whatsapp.mobileBuilder()
                            .newConnection()
                            .device(CompanionDevice.ios(true))
                            .unregistered()
                            .verificationCodeMethod(VerificationCodeMethod.WHATSAPP)
                            .verificationCodeSupplier(() -> codeFuture)
                            .register(sixPartsKeys.phoneNumber().number())
                            .join();
                    return api;
                })
                .join()
                .awaitDisconnection();
    }
}
