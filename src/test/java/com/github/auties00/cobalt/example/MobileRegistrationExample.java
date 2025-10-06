package com.github.auties00.cobalt.example;

import com.github.auties00.cobalt.api.Whatsapp;
import com.github.auties00.cobalt.api.WhatsappVerificationHandler;
import com.github.auties00.cobalt.model.jid.JidDevice;

import java.util.Scanner;

public class MobileRegistrationExample {
    public static void main(String[] args) {
        System.out.println("Enter the phone value: "); // You can get a value from https://daisysms.com, do not spam registrations, or you'll get banned
        var scanner = new Scanner(System.in);
        var phoneNumber = scanner.nextLong();
        System.out.println("Select if the account is business or personal:\n(1) Business (2) Personal");
        var business = switch (scanner.nextInt()) {
            case 1 -> true;
            case 2 -> false;
            default -> throw new IllegalStateException("Unexpected value: " + scanner.nextInt());
        };
        Whatsapp.builder()
                .mobileClient()
                .lastConnection()
                // .proxy(URI.create("http://username:password@host:port/")) Remember to set an HTTP proxy
                .device(JidDevice.ios(business)) // Make sure to select the correct account type(business or personal) or you'll get error 401
                .register(phoneNumber, WhatsappVerificationHandler.Mobile.sms(() -> {
                    System.out.println("Enter the verification code: ");
                    return new Scanner(System.in)
                            .nextLine()
                            .trim()
                            .replace("-", "");
                }))
                .addNodeReceivedListener(incoming -> System.out.printf("Received node %s%n", incoming))
                .addNodeSentListener(outgoing -> System.out.printf("Sent node %s%n", outgoing))
                .addLoggedInListener(api -> System.out.println("Logged in"))
                .connect() // If you get error 403 o 503 the account is banned
                .waitForDisconnection();
    }
}
