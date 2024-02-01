package it.auties.whatsapp.example;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.companion.CompanionDevice;

import java.util.Scanner;

public class RegisterExample {
    public static void main(String[] args) {
        System.out.println("Enter the phone number: ");
        var phoneNumber = new Scanner(System.in).nextLong();
        var result = Whatsapp.mobileBuilder()
                .newConnection()
                .device(CompanionDevice.ios(false))
                .unregistered()
                .verificationCodeSupplier(() -> {
                    System.out.println("Enter OTP: ");
                    return new Scanner(System.in).nextLine();
                })
                .register(phoneNumber)
                .join();
        System.out.println(result.keys().toString());
    }
}
