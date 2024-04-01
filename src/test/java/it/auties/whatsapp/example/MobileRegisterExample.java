package it.auties.whatsapp.example;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.companion.CompanionDevice;

import java.util.Scanner;

// Android setup: open the directory at support/android and read README.md
// iOS setup: none necessary
// KaiOS setup: none necessary
public class MobileRegisterExample {
    public static void main(String[] args) {
        System.out.println("Enter the phone number: ");
        Scanner scanner = new Scanner(System.in);
        var phoneNumber = scanner.nextLong();
        System.out.println("Select if the account is business or personal:\n(1) Business (2) Personal");
        var business = switch (scanner.nextInt()) {
            case 1 -> true;
            case 2 -> false;
            default -> throw new IllegalStateException("Unexpected value: " + scanner.nextInt());
        };
        Whatsapp.mobileBuilder()
                .newConnection()
                .device(CompanionDevice.ios(business))
                // .proxy(URI.create("http://username:password@host:port/")) Remember to set an HTTP proxy
                .unregistered()
                .verificationCodeSupplier(() -> {
                    System.out.println("Enter OTP: ");
                    return new Scanner(System.in).nextLine();
                })
                .register(phoneNumber)
                .join();
    }
}
