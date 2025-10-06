package com.github.auties00.cobalt.example;

import com.github.auties00.cobalt.api.Whatsapp;
import com.github.auties00.cobalt.model.jid.JidDevice;
import com.github.auties00.cobalt.api.WhatsappSixPartsKeys;

import java.util.Scanner;

public class MobileLoginExample {
    public static void main(String[] args) {
        System.out.println("Enter the six parts segment: ");
        var scanner = new Scanner(System.in);
        var sixParts = scanner.nextLine().trim();
        System.out.println("Select if the account is business or personal:\n(1) Business (2) Personal");
        var business = switch (scanner.nextInt()) {
            case 1 -> true;
            case 2 -> false;
            default -> throw new IllegalStateException("Unexpected value: " + scanner.nextInt());
        };
        Whatsapp.builder()
                .mobileClient()
                .newConnection(WhatsappSixPartsKeys.of(sixParts))
                // .proxy(URI.create("http://username:password@host:port/")) Remember to set an HTTP proxy
                .device(JidDevice.ios(business)) // Make sure to select the correct account type(business or personal) or you'll get error 401
                .registered()
                .orElseThrow()
                .addNodeReceivedListener(incoming -> System.out.printf("Received node %s%n", incoming))
                .addNodeSentListener(outgoing -> System.out.printf("Sent node %s%n", outgoing))
                .addLoggedInListener(api -> System.out.println("Logged in"))
                .connect() // If you get error 403 o 503 the account is banned
                .waitForDisconnection();
    }
}
